package com.nbatch.job.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.core.cron.CronExpression;
import com.nbatch.job.admin.core.domain.param.JobWorkPageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkParam;
import com.nbatch.job.admin.core.domain.po.JobGroupPo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogPo;
import com.nbatch.job.admin.core.domain.vo.JobWorkVo;
import com.nbatch.job.admin.core.enums.MisfireStrategyEnum;
import com.nbatch.job.admin.core.enums.ScheduleTypeEnum;
import com.nbatch.job.admin.core.helper.FlowGroupHelper;
import com.nbatch.job.admin.core.thread.JobScheduleHelper;
import com.nbatch.job.admin.mapper.IJobGroupMapper;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeLogMapper;
import com.nbatch.job.admin.service.IJobWorkService;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.WorkTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 作业配置与调度服务。
 * 运行快照的停止、恢复及重跑由 {@link JobWorkRunServiceImpl} 处理。
 */
@Service
public class JobWorkServiceImpl implements IJobWorkService {

    private static final DateTimeFormatter DASHBOARD_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    @Resource
    private IJobWorkMapper jobWorkMapper;
    @Resource
    private IJobWorkRunNodeLogMapper jobWorkRunNodeLogMapper;
    @Resource
    private IJobWorkNodeMapper jobWorkNodeMapper;
    @Resource
    private IJobGroupMapper jobGroupMapper;
    @Resource
    private FlowGroupHelper flowGroupHelper;

    /**
     * 首页汇总指标。
     */
    @Override
    public Map<String, Object> dashboardInfo() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime recentStart = now.minusDays(1);
        Map<String, Object> result = new HashMap<>();
        result.put("runningNodeCount", jobWorkRunNodeLogMapper.selectCount(Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                .eq(JobWorkRunNodeLogPo::getHandleCode, 0)
                .isNull(JobWorkRunNodeLogPo::getCallBackTime)));
        result.put("recentFailureCount", jobWorkRunNodeLogMapper.selectCount(Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                .eq(JobWorkRunNodeLogPo::getHandleCode, HandleCodeConstant.HANDLE_CODE_FAIL)
                .ge(JobWorkRunNodeLogPo::getCreateTime, recentStart)));
        result.put("todaySuccessCount", jobWorkRunNodeLogMapper.selectCount(Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                .eq(JobWorkRunNodeLogPo::getHandleCode, HandleCodeConstant.HANDLE_CODE_SUCCESS)
                .ge(JobWorkRunNodeLogPo::getCallBackTime, todayStart)));
        Set<String> addresses = new HashSet<>();
        jobGroupMapper.selectList(Wrappers.lambdaQuery(JobGroupPo.class)).forEach(group -> {
            if (group.getRegistryList() != null) {
                addresses.addAll(group.getRegistryList());
            }
        });
        result.put("executorCount", addresses.size());
        result.put("recentFailures", recentFailures());
        return result;
    }

    private List<Map<String, Object>> recentFailures() {
        List<JobWorkRunNodeLogPo> logs = jobWorkRunNodeLogMapper
                .selectLatestFailedNodeLogs(HandleCodeConstant.HANDLE_CODE_FAIL, 5);
        if (logs.isEmpty()) {
            return new ArrayList<>();
        }
        Set<String> workIds = logs.stream().map(JobWorkRunNodeLogPo::getWorkId).collect(Collectors.toSet());
        Set<String> nodeIds = logs.stream().map(JobWorkRunNodeLogPo::getNodeId).collect(Collectors.toSet());
        Map<String, String> workNames = jobWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkPo.class)
                        .in(JobWorkPo::getWorkId, workIds))
                .stream().collect(Collectors.toMap(JobWorkPo::getWorkId, JobWorkPo::getWorkName, (left, right) -> left));
        Map<String, String> nodeNames = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                        .in(JobWorkNodePo::getNodeId, nodeIds))
                .stream().collect(Collectors.toMap(JobWorkNodePo::getNodeId, JobWorkNodePo::getNodeName, (left, right) -> left));
        return logs.stream().map(log -> {
            Map<String, Object> item = new HashMap<>();
            item.put("workId", log.getWorkId());
            item.put("workName", StrUtil.blankToDefault(workNames.get(log.getWorkId()), log.getWorkId()));
            item.put("nodeName", StrUtil.blankToDefault(nodeNames.get(log.getNodeId()), log.getNodeId()));
            item.put("handleMsg", StrUtil.blankToDefault(log.getHandleMsg(), "节点执行失败"));
            LocalDateTime occurredAt = log.getCallBackTime() != null ? log.getCallBackTime()
                    : (log.getLastActiveTime() != null ? log.getLastActiveTime() : log.getCreateTime());
            item.put("occurredAt", occurredAt == null ? "" : DASHBOARD_TIME_FORMATTER.format(occurredAt));
            return item;
        }).collect(Collectors.toList());
    }

    /**
     * 统计指定日期区间内的节点运行结果。
     */
    @Override
    public ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate) {
        Date from = startDate == null ? DateUtil.offsetDay(new Date(), -6) : startDate;
        Date to = endDate == null ? new Date() : endDate;
        List<JobWorkRunNodeLogPo> logs = jobWorkRunNodeLogMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                .ge(JobWorkRunNodeLogPo::getCreateTime, LocalDateTime.ofInstant(from.toInstant(), ZoneId.systemDefault()))
                .le(JobWorkRunNodeLogPo::getCreateTime, LocalDateTime.ofInstant(to.toInstant(), ZoneId.systemDefault()))
                .orderByAsc(JobWorkRunNodeLogPo::getCreateTime));
        List<String> days = new ArrayList<>();
        List<Integer> running = new ArrayList<>(), success = new ArrayList<>(), fail = new ArrayList<>();
        for (Date day = DateUtil.beginOfDay(from), last = DateUtil.beginOfDay(to); !day.after(last); day = DateUtil.offsetDay(day, 1)) {
            String dayText = DateUtil.formatDate(day);
            int runningCount = 0, successCount = 0, failCount = 0;
            for (JobWorkRunNodeLogPo log : logs) {
                if (log.getCreateTime() == null || !dayText.equals(log.getCreateTime().toLocalDate().toString())) {
                    continue;
                }
                if (log.getHandleCode() != null && log.getHandleCode() == 0) {
                    runningCount++;
                }
                else if (log.getHandleCode() != null && log.getHandleCode() == HandleCodeConstant.HANDLE_CODE_SUCCESS) {
                    successCount++;
                } else {
                    failCount++;
                }
            }
            days.add(dayText); running.add(runningCount); success.add(successCount); fail.add(failCount);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("triggerDayList", days); result.put("triggerDayCountRunningList", running);
        result.put("triggerDayCountSucList", success); result.put("triggerDayCountFailList", fail);
        return new ReturnT<>(result);
    }

    /**
     * 分页查询作业配置，并补充显示用的枚举和执行器组名称。
     */
    @Override
    public Map<String, Object> pageList(JobWorkPageParam param) {
        Page<JobWorkPo> page = jobWorkMapper.selectPage(new Page<>((param.getStart() / param.getLength()) + 1, param.getLength()),
                Wrappers.lambdaQuery(JobWorkPo.class)
                        .like(StrUtil.isNotBlank(param.getWorkName()), JobWorkPo::getWorkName, param.getWorkName()));
        Map<String, String> groupNames = jobGroupMapper.selectList(Wrappers.lambdaQuery(JobGroupPo.class))
                .stream().collect(Collectors.toMap(JobGroupPo::getId, JobGroupPo::getTitle, (left, right) -> left));
        List<JobWorkVo> records = page.getRecords().stream()
                .map(work -> toWorkVo(work, groupNames)).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("recordsTotal", page.getTotal()); result.put("recordsFiltered", page.getTotal()); result.put("data", records);
        return result;
    }

    /**
     * 新建作业配置并初始化默认流程组。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(JobWorkParam param) {
        JobWorkPo work = toWorkPo(param);
        work.setCreateTime(DateUtil.date()).setUpdateTime(DateUtil.date());
        int count = jobWorkMapper.insert(work);
        if (count == 1) {
            flowGroupHelper.ensureRootGroup(work.getWorkId());
        }
        return count;
    }

    /**
     * 修改作业配置；未显式提交调度状态时保留原状态。
     */
    @Override
    public int update(JobWorkParam param) {
        JobWorkPo existing = jobWorkMapper.selectById(param.getWorkId());
        JobWorkPo work = toWorkPo(param);
        if (existing != null && param.getScheduleStatus() == null && StrUtil.isNotBlank(param.getCronExpression())) {
            work.setScheduleStatus(existing.getScheduleStatus());
        }
        work.setUpdateTime(DateUtil.date());
        return jobWorkMapper.updateById(work);
    }

    /**
     * 启动定时调度并计算下一次触发时间。
     */
    @Override
    public ReturnT<String> startSchedule(String workId) {
        JobWorkPo work = jobWorkMapper.selectById(workId);
        if (work == null || StrUtil.isBlank(work.getScheduleConf()) || StrUtil.isBlank(work.getJobGroup())) {
            return ReturnT.error("作业不存在、未配置调度或未选择执行器");
        }
        try {
            Date next = JobScheduleHelper.generateNextValidTime(work, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
            jobWorkMapper.update(null, Wrappers.lambdaUpdate(JobWorkPo.class).set(JobWorkPo::getScheduleStatus, 1)
                    .set(JobWorkPo::getTriggerNextTime, next == null ? null : next.getTime()).eq(JobWorkPo::getWorkId, workId));
            return ReturnT.SUCCESS;
        } catch (Exception e) {
            return ReturnT.error("调度配置无效");
        }
    }

    /**
     * 停止定时调度。
     */
    @Override
    public ReturnT<String> stopSchedule(String workId) {
        jobWorkMapper.update(null, Wrappers.lambdaUpdate(JobWorkPo.class).set(JobWorkPo::getScheduleStatus, 0)
                .set(JobWorkPo::getTriggerNextTime, null).eq(JobWorkPo::getWorkId, workId));
        return ReturnT.SUCCESS;
    }

    /**
     * 获取编辑页面使用的作业配置。
     */
    @Override
    public JobWorkVo getModel(String id) {
        JobWorkPo work = jobWorkMapper.selectById(id);
        if (work == null) {
            return null;
        }
        JobWorkVo vo = BeanUtil.toBean(work, JobWorkVo.class);
        vo.setCronExpression(work.getScheduleConf());
        return vo;
    }

    /**
     * 删除作业配置。
     */
    @Override
    public int delete(String id) {
        return jobWorkMapper.selectById(id) == null ? 1 : jobWorkMapper.deleteById(id);
    }

    private JobWorkVo toWorkVo(JobWorkPo work, Map<String, String> groupNames) {
        JobWorkVo vo = BeanUtil.toBean(work, JobWorkVo.class);
        vo.setJobGroupName(groupNames.get(work.getJobGroup())); vo.setWorkTypeName(getWorkTypeName(work.getWorkType()));
        if (work.getRunWorkStatus() != null) {
            vo.setWorkId(work.getWorkId());
            if (work.getTurnDate() != null) {
                vo.setTurnDate(DateUtil.formatDate(work.getTurnDate()));
            }
            if (work.getRunCreateTime() != null) {
                vo.setRunWorkCreateTime(DateUtil.formatDateTime(work.getRunCreateTime()));
            }
            vo.setRunWorkStatus(work.getRunWorkStatus());
            vo.setRunWorkStatusName(FlowRunStatusEnum.getValueByCode(work.getRunWorkStatus()));
        }
        return vo;
    }

    /** 将页面参数转换为持久化对象并校验调度表达式。 */
    private JobWorkPo toWorkPo(JobWorkParam param) {
        JobWorkPo work = BeanUtil.toBean(param, JobWorkPo.class);
        if (StrUtil.isBlank(param.getCronExpression())) {
            return work.setScheduleType(ScheduleTypeEnum.NONE.name())
                    .setScheduleConf(null)
                    .setScheduleStatus(0)
                    .setTriggerNextTime(null)
                    .setTriggerLastTime(null);
        }
        if (!CronExpression.isValidExpression(param.getCronExpression())) {
            throw new IllegalArgumentException("cronExpression格式无效");
        }
        work.setScheduleType(ScheduleTypeEnum.CRON.name()).setScheduleConf(param.getCronExpression()).setScheduleStatus(param.getScheduleStatus() == null ? 0 : param.getScheduleStatus()).setMisfireStrategy(StrUtil.blankToDefault(param.getMisfireStrategy(), MisfireStrategyEnum.DO_NOTHING.name()));
        try {
            Date next = JobScheduleHelper.generateNextValidTime(work, new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
            if (next == null) {
                throw new IllegalArgumentException("cronExpression没有下一次触发时间");
            }
            return work.setTriggerNextTime(next.getTime());
        } catch (Exception e) {
            throw new IllegalArgumentException("cronExpression无法计算下一次触发时间", e);
        }
    }

    private String getWorkTypeName(Integer code) {
        if (code != null) {
            for (WorkTypeEnum value : WorkTypeEnum.values()) {
                if (value.getCode() == code) {
                    return value.getValue();
                }
            }
        }
        return null;
    }
}
