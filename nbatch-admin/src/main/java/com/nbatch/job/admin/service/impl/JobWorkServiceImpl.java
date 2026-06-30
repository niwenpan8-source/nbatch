package com.nbatch.job.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.core.domain.param.JobWorkPageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkParam;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogDetailPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogPo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.vo.JobWorkVo;
import com.nbatch.job.admin.core.executor.ExecutorBizProxy;
import com.nbatch.job.admin.core.helper.RunWorkHelper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeLogDetailMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeLogMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunMapper;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.service.IJobWorkService;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.StopRunNodeParam;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.FlowStatusEnum;
import com.nbatch.job.core.enums.WorkTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description: 作业执行服务实现类
 * @author: Mr.ni
 * @date: 2025/11/13
 */
@Service
@Slf4j
public class JobWorkServiceImpl implements IJobWorkService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private IJobWorkMapper jobWorkMapper;

    @Resource
    private IJobWorkRunMapper jobRunWorkMapper;

    @Resource
    private IJobWorkRunNodeMapper jobWorkRunNodeMapper;

    @Resource
    private IJobWorkRunNodeLogMapper jobWorkRunNodeLogMapper;

    @Resource
    private IJobWorkRunNodeLogDetailMapper jobWorkRunNodeLogDetailMapper;

    @Resource
    private IJobWorkNodeMapper jobWorkNodeMapper;

    @Resource
    private RunWorkHelper runWorkHelper;

    /**
     * 分页列表
     */
    @Override
    public Map<String, Object> pageList(JobWorkPageParam param) {
        Page<JobWorkPo> page = jobWorkMapper.selectPage(
                new Page<>((param.getStart() / param.getLength()) + 1, param.getLength()),
                Wrappers.lambdaQuery(JobWorkPo.class)
                        .eq(param.getWorkStatus() != null, JobWorkPo::getWorkStatus, param.getWorkStatus())
                        .like(StrUtil.isNotBlank(param.getWorkName()), JobWorkPo::getWorkName, param.getWorkName()));
        List<JobWorkPo> records = page.getRecords();
        List<String> workIdList = records.stream().map(JobWorkPo::getWorkId).collect(Collectors.toList());
        List<JobWorkRunPo> jobRunWorkPoList = workIdList.isEmpty() ? Collections.emptyList()
                : jobRunWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkRunPo.class)
                .in(JobWorkRunPo::getWorkId, workIdList)
                .orderByDesc(JobWorkRunPo::getCreateTime));

        Map<String, JobWorkRunPo> jobRunWorkPoMap = jobRunWorkPoList.stream()
                .collect(Collectors.toMap(JobWorkRunPo::getWorkId, jobRunWorkPo -> jobRunWorkPo, (old, v) -> old));

        List<JobWorkVo> jobWorkVoList = records.stream().map(x -> {
            JobWorkRunPo jobRunWorkPo = jobRunWorkPoMap.get(x.getWorkId());
            JobWorkVo jobWorkVo = BeanUtil.toBean(x, JobWorkVo.class);
            jobWorkVo.setWorkStatusName(getFlowStatusName(x.getWorkStatus()));
            jobWorkVo.setWorkTypeName(getWorkTypeName(x.getWorkType()));
            jobWorkVo.setInitTurnDateText(x.getInitTurnDate() == null ? null : DateUtil.formatDate(x.getInitTurnDate()));
            if (jobRunWorkPo != null) {
                jobWorkVo.setRunWorkId(jobRunWorkPo.getRunWorkId());
                if (jobRunWorkPo.getTurnDate() != null) {
                    jobWorkVo.setTurnDate(DateUtil.formatDate(jobRunWorkPo.getTurnDate()));
                }
                if (jobRunWorkPo.getCreateTime() != null) {
                    jobWorkVo.setRunWorkCreateTime(DateUtil.formatDateTime(jobRunWorkPo.getCreateTime()));
                }
                jobWorkVo.setRunWorkStatus(jobRunWorkPo.getRunWorkStatus());
                jobWorkVo.setRunWorkStatusName(FlowRunStatusEnum.getValueByCode(jobRunWorkPo.getRunWorkStatus()));
            }
            return jobWorkVo;
        }).collect(Collectors.toList());
        // package result
        Map<String, Object> maps = new HashMap<>();
        // 总记录数
        maps.put("recordsTotal", page.getTotal());
        // 过滤后的总记录数
        maps.put("recordsFiltered", page.getTotal());
        // 分页列表
        maps.put("data", jobWorkVoList);
        return maps;
    }

    private String getFlowStatusName(Integer code) {
        if (code == null) {
            return null;
        }
        for (FlowStatusEnum value : FlowStatusEnum.values()) {
            if (value.getCode().equals(code)) {
                return value.getValue();
            }
        }
        return null;
    }

    private String getWorkTypeName(Integer code) {
        if (code == null) {
            return null;
        }
        for (WorkTypeEnum value : WorkTypeEnum.values()) {
            if (value.getCode() == code) {
                return value.getValue();
            }
        }
        return null;
    }

    private long countNodeStatus(List<JobWorkRunNodePo> nodeList, int status) {
        return nodeList.stream()
                .filter(node -> node.getNodeRunStatus() != null && node.getNodeRunStatus() == status)
                .count();
    }

    /**
     * 获取作业列表
     */
    @Override
    public List<JobWorkPo> getWorkList() {
        return jobWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkPo.class));
    }

    /**
     * 插入
     */
    @Override
    public int insert(JobWorkParam param) {
        validateInitTurnDate(param.getInitTurnDate());
        return jobWorkMapper.insert(BeanUtil.toBean(param, JobWorkPo.class));
    }

    /**
     * 修改
     */
    @Override
    public int update(JobWorkParam param) {
        validateInitTurnDate(param.getInitTurnDate());
        return jobWorkMapper.updateById(BeanUtil.toBean(param, JobWorkPo.class));
    }

    /**
     * 通过得到id得到对象
     */
    @Override
    public JobWorkVo getModel(String id) {
        JobWorkPo jobWorkPo = jobWorkMapper.selectById(id);
        if (jobWorkPo == null) {
            return null;
        }
        return BeanUtil.toBean(jobWorkPo, JobWorkVo.class);
    }

    @Override
    public ReturnT<Map<String, Object>> detail(String workId) {
        if (workId == null || workId.trim().isEmpty()) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业ID不能为空");
        }
        JobWorkPo jobWorkPo = jobWorkMapper.selectById(workId);
        if (jobWorkPo == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业不存在");
        }

        List<JobWorkRunPo> runWorkList = jobRunWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkRunPo.class)
                .eq(JobWorkRunPo::getWorkId, workId)
                .orderByDesc(JobWorkRunPo::getCreateTime));
        List<String> runWorkIdList = runWorkList.stream().map(JobWorkRunPo::getRunWorkId).collect(Collectors.toList());
        Map<String, List<JobWorkRunNodePo>> runNodeMap = runWorkIdList.isEmpty() ? Collections.emptyMap()
                : jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                .in(JobWorkRunNodePo::getRunWorkId, runWorkIdList)).stream()
                .collect(Collectors.groupingBy(JobWorkRunNodePo::getRunWorkId));

        List<Map<String, Object>> snapshotRunList = runWorkList.stream().map(runWorkPo -> {
            List<JobWorkRunNodePo> nodeList = runNodeMap.getOrDefault(runWorkPo.getRunWorkId(), Collections.emptyList());
            Map<String, Object> item = new HashMap<>();
            item.put("runWorkId", runWorkPo.getRunWorkId());
            item.put("turnDate", runWorkPo.getTurnDate() == null ? null : DateUtil.formatDate(runWorkPo.getTurnDate()));
            item.put("runWorkStatus", runWorkPo.getRunWorkStatus());
            item.put("runWorkStatusName", runWorkPo.getRunWorkStatus() == null ? null : FlowRunStatusEnum.getValueByCode(runWorkPo.getRunWorkStatus()));
            item.put("createTime", runWorkPo.getCreateTime() == null ? null : DateUtil.formatDateTime(runWorkPo.getCreateTime()));
            item.put("nodeCount", nodeList.size());
            item.put("completeCount", countNodeStatus(nodeList, FlowRunStatusEnum.COMPLETE.getCode()));
            item.put("skippedCount", countNodeStatus(nodeList, FlowRunStatusEnum.SKIPPED.getCode()));
            item.put("dispatchedCount", countNodeStatus(nodeList, FlowRunStatusEnum.DISPATCHED.getCode()));
            item.put("runningCount", countNodeStatus(nodeList, FlowRunStatusEnum.RUNNING.getCode()));
            item.put("exceptionCount", countNodeStatus(nodeList, FlowRunStatusEnum.EXCEPTION.getCode()));
            item.put("stoppedCount", countNodeStatus(nodeList, FlowRunStatusEnum.STOPPED.getCode()));
            item.put("waitCount", countNodeStatus(nodeList, FlowRunStatusEnum.WAIT.getCode()));
            return item;
        }).collect(Collectors.toList());
        List<Map<String, Object>> runList = buildRunHistoryFromLogs(workId, runWorkList, runNodeMap);
        if (CollUtil.isEmpty(runList)) {
            runList = snapshotRunList;
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("workId", jobWorkPo.getWorkId());
        detail.put("workName", jobWorkPo.getWorkName());
        detail.put("workDesc", jobWorkPo.getWorkDesc());
        detail.put("workTypeName", getWorkTypeName(jobWorkPo.getWorkType()));
        detail.put("workStatusName", getFlowStatusName(jobWorkPo.getWorkStatus()));
        detail.put("initTurnDate", jobWorkPo.getInitTurnDate() == null ? null : DateUtil.formatDate(jobWorkPo.getInitTurnDate()));
        detail.put("version", jobWorkPo.getVersion());
        detail.put("runCount", runList.size());
        detail.put("runList", runList);
        return ReturnT.success(detail);
    }

    private List<Map<String, Object>> buildRunHistoryFromLogs(String workId,
                                                              List<JobWorkRunPo> runWorkList,
                                                              Map<String, List<JobWorkRunNodePo>> runNodeMap) {
        List<JobWorkRunNodeLogPo> logList = jobWorkRunNodeLogMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                .eq(JobWorkRunNodeLogPo::getWorkId, workId)
                .orderByDesc(JobWorkRunNodeLogPo::getTurnDate)
                .orderByDesc(JobWorkRunNodeLogPo::getCreateTime));
        if (CollUtil.isEmpty(logList)) {
            return Collections.emptyList();
        }
        Map<String, JobWorkRunPo> runWorkMap = runWorkList.stream()
                .collect(Collectors.toMap(JobWorkRunPo::getRunWorkId, x -> x, (oldValue, newValue) -> oldValue));
        Map<String, List<JobWorkRunNodeLogPo>> turnDateLogMap = new LinkedHashMap<>();
        for (JobWorkRunNodeLogPo logPo : logList) {
            String turnDate = logPo.getTurnDate() == null ? "未记录" : DateUtil.formatDate(logPo.getTurnDate());
            turnDateLogMap.computeIfAbsent(turnDate, key -> new ArrayList<>()).add(logPo);
        }
        List<Map<String, Object>> runList = new ArrayList<>();
        for (Map.Entry<String, List<JobWorkRunNodeLogPo>> entry : turnDateLogMap.entrySet()) {
            List<JobWorkRunNodeLogPo> logs = entry.getValue();
            JobWorkRunNodeLogPo firstLog = logs.get(0);
            JobWorkRunPo runWorkPo = runWorkMap.get(firstLog.getRunWorkId());
            List<JobWorkRunNodePo> currentNodeList = runWorkPo == null
                    ? Collections.emptyList()
                    : runNodeMap.getOrDefault(runWorkPo.getRunWorkId(), Collections.emptyList());
            Map<String, JobWorkRunNodeLogPo> latestNodeLogMap = new LinkedHashMap<>();
            for (JobWorkRunNodeLogPo logPo : logs) {
                latestNodeLogMap.putIfAbsent(logPo.getNodeId(), logPo);
            }
            Map<String, Object> item = new HashMap<>();
            item.put("runWorkId", firstLog.getRunWorkId());
            item.put("turnDate", entry.getKey());
            item.put("runWorkStatus", runWorkPo == null ? null : runWorkPo.getRunWorkStatus());
            item.put("runWorkStatusName", runWorkPo == null || runWorkPo.getRunWorkStatus() == null ? null : FlowRunStatusEnum.getValueByCode(runWorkPo.getRunWorkStatus()));
            item.put("createTime", firstLog.getCreateTime() == null ? null : DATE_TIME_FORMATTER.format(firstLog.getCreateTime()));
            item.put("nodeCount", latestNodeLogMap.size());
            if (runWorkPo != null && runWorkPo.getTurnDate() != null && DateUtil.formatDate(runWorkPo.getTurnDate()).equals(entry.getKey())) {
                item.put("completeCount", countNodeStatus(currentNodeList, FlowRunStatusEnum.COMPLETE.getCode()));
                item.put("skippedCount", countNodeStatus(currentNodeList, FlowRunStatusEnum.SKIPPED.getCode()));
                item.put("dispatchedCount", countNodeStatus(currentNodeList, FlowRunStatusEnum.DISPATCHED.getCode()));
                item.put("runningCount", countNodeStatus(currentNodeList, FlowRunStatusEnum.RUNNING.getCode()));
                item.put("exceptionCount", countNodeStatus(currentNodeList, FlowRunStatusEnum.EXCEPTION.getCode()));
                item.put("stoppedCount", countNodeStatus(currentNodeList, FlowRunStatusEnum.STOPPED.getCode()));
                item.put("waitCount", countNodeStatus(currentNodeList, FlowRunStatusEnum.WAIT.getCode()));
            } else {
                item.put("completeCount", countHandleCode(latestNodeLogMap, HandleCodeConstant.HANDLE_CODE_SUCCESS));
                item.put("skippedCount", 0L);
                item.put("dispatchedCount", countHandleCode(latestNodeLogMap, 0));
                item.put("runningCount", 0L);
                item.put("exceptionCount", latestNodeLogMap.values().stream()
                        .filter(logPo -> logPo.getHandleCode() != null
                                && logPo.getHandleCode() != 0
                                && logPo.getHandleCode() != HandleCodeConstant.HANDLE_CODE_SUCCESS)
                        .count());
                item.put("stoppedCount", 0L);
                item.put("waitCount", 0L);
            }
            runList.add(item);
        }
        return runList;
    }

    /**
     * 统计指定处理码的节点数量
     */
    private long countHandleCode(Map<String, JobWorkRunNodeLogPo> latestNodeLogMap, int handleCode) {
        return latestNodeLogMap.values().stream()
                .filter(logPo -> logPo.getHandleCode() != null && logPo.getHandleCode() == handleCode)
                .count();
    }

    /**
     * 删除
     */
    @Override
    public int delete(String id) {
        JobWorkPo jobWorkPo = jobWorkMapper.selectById(id);
        if (jobWorkPo == null) {
            return 1;
        }
        return jobWorkMapper.deleteById(id);
    }

    @Override
    public ReturnT<String> recoverRunWork(String runWorkId) {
        if (runWorkId == null || runWorkId.trim().isEmpty()) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "运行作业ID不能为空");
        }
        JobWorkRunPo oldRunWorkPo = jobRunWorkMapper.selectById(runWorkId);
        if (oldRunWorkPo == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "运行作业不存在");
        }
        if (oldRunWorkPo.getRunWorkStatus() != null
                && oldRunWorkPo.getRunWorkStatus() != FlowRunStatusEnum.RUNNING.getCode()
                && oldRunWorkPo.getRunWorkStatus() != FlowRunStatusEnum.DISPATCHED.getCode()
                && oldRunWorkPo.getRunWorkStatus() != FlowRunStatusEnum.STOPPED.getCode()
                && oldRunWorkPo.getRunWorkStatus() != FlowRunStatusEnum.EXCEPTION.getCode()) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "只有已下发、进行中、已停止或异常的运行作业可以恢复重跑");
        }

        List<JobWorkRunNodePo> resetRunNodeList = jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                .eq(JobWorkRunNodePo::getRunWorkId, runWorkId)
                .in(JobWorkRunNodePo::getNodeRunStatus,
                        FlowRunStatusEnum.EXCEPTION.getCode(),
                        FlowRunStatusEnum.STOPPED.getCode(),
                        FlowRunStatusEnum.DISPATCHED.getCode(),
                        FlowRunStatusEnum.RUNNING.getCode()));
        if (CollUtil.isEmpty(resetRunNodeList)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "当前运行作业没有异常或失败节点可恢复");
        }

        Map<String, JobWorkNodePo> nodeTemplateMap = getNodeTemplateMap(oldRunWorkPo.getWorkId());
        for (JobWorkRunNodePo runNodePo : resetRunNodeList) {
            JobWorkNodePo nodeTemplate = nodeTemplateMap.get(runNodePo.getNodeId());
            jobWorkRunNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkRunNodePo.class)
                    .set(JobWorkRunNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode())
                    .set(JobWorkRunNodePo::getRetryTimes, nodeTemplate == null ? runNodePo.getRetryTimes() : nodeTemplate.getRetryTimes())
                    .set(JobWorkRunNodePo::getErrorStrategy, nodeTemplate == null ? runNodePo.getErrorStrategy() : nodeTemplate.getErrorStrategy())
                    .set(JobWorkRunNodePo::getStartTime, null)
                    .set(JobWorkRunNodePo::getEndTime, null)
                    .eq(JobWorkRunNodePo::getRunNodeId, runNodePo.getRunNodeId()));
        }

        jobRunWorkMapper.update(null, Wrappers.lambdaUpdate(JobWorkRunPo.class)
                .set(JobWorkRunPo::getRunWorkStatus, FlowRunStatusEnum.WAIT.getCode())
                .eq(JobWorkRunPo::getRunWorkId, runWorkId));
        return ReturnT.SUCCESS;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> stopLatestRunWork(String workId) {
        if (workId == null || workId.trim().isEmpty()) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业ID不能为空");
        }
        JobWorkRunPo latestRunWorkPo = getLatestRunWork(workId);
        if (latestRunWorkPo == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "暂无运行作业记录");
        }
        if (latestRunWorkPo.getRunWorkStatus() == null
                || (latestRunWorkPo.getRunWorkStatus() != FlowRunStatusEnum.WAIT.getCode()
                && latestRunWorkPo.getRunWorkStatus() != FlowRunStatusEnum.DISPATCHED.getCode()
                && latestRunWorkPo.getRunWorkStatus() != FlowRunStatusEnum.RUNNING.getCode())) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "只有待执行、已下发或进行中的运行作业可以停止");
        }

        List<String> stopRunNodeIdList = jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                        .select(JobWorkRunNodePo::getRunNodeId)
                        .eq(JobWorkRunNodePo::getRunWorkId, latestRunWorkPo.getRunWorkId())
                        .in(JobWorkRunNodePo::getNodeRunStatus,
                                FlowRunStatusEnum.WAIT.getCode(),
                                FlowRunStatusEnum.DISPATCHED.getCode(),
                                FlowRunStatusEnum.RUNNING.getCode()))
                .stream()
                .map(JobWorkRunNodePo::getRunNodeId)
                .collect(Collectors.toList());

        jobRunWorkMapper.update(null, Wrappers.lambdaUpdate(JobWorkRunPo.class)
                .set(JobWorkRunPo::getRunWorkStatus, FlowRunStatusEnum.STOPPED.getCode())
                .eq(JobWorkRunPo::getRunWorkId, latestRunWorkPo.getRunWorkId()));

        jobWorkRunNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkRunNodePo.class)
                .set(JobWorkRunNodePo::getNodeRunStatus, FlowRunStatusEnum.STOPPED.getCode())
                .set(JobWorkRunNodePo::getEndTime, java.time.LocalDateTime.now())
                .eq(JobWorkRunNodePo::getRunWorkId, latestRunWorkPo.getRunWorkId())
                .in(JobWorkRunNodePo::getNodeRunStatus,
                        FlowRunStatusEnum.WAIT.getCode(),
                        FlowRunStatusEnum.DISPATCHED.getCode(),
                        FlowRunStatusEnum.RUNNING.getCode()));
        sendStopRunNodeCommand(latestRunWorkPo.getRunWorkId(), latestRunWorkPo.getTurnDate(), stopRunNodeIdList);
        return ReturnT.SUCCESS;
    }

    private void sendStopRunNodeCommand(String runWorkId, Date turnDate, List<String> stopRunNodeIdList) {
        if (CollUtil.isEmpty(stopRunNodeIdList)) {
            return;
        }
        List<JobWorkRunNodeLogPo> runNodeLogList = jobWorkRunNodeLogMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                .eq(JobWorkRunNodeLogPo::getRunWorkId, runWorkId)
                .in(JobWorkRunNodeLogPo::getRunNodeId, stopRunNodeIdList)
                .eq(turnDate != null, JobWorkRunNodeLogPo::getTurnDate, turnDate)
                .isNull(JobWorkRunNodeLogPo::getCallBackTime)
                .isNotNull(JobWorkRunNodeLogPo::getExecutorAddress));
        if (CollUtil.isEmpty(runNodeLogList)) {
            return;
        }
        Map<String, List<JobWorkRunNodeLogPo>> addressRunNodeLogMap = runNodeLogList.stream()
                .filter(logPo -> StrUtil.isNotBlank(logPo.getExecutorAddress())
                        && StrUtil.isNotBlank(logPo.getRunNodeId())
                        && StrUtil.isNotBlank(logPo.getNodeLogId()))
                .collect(Collectors.groupingBy(JobWorkRunNodeLogPo::getExecutorAddress));
        for (Map.Entry<String, List<JobWorkRunNodeLogPo>> entry : addressRunNodeLogMap.entrySet()) {
            try {
                StopRunNodeParam stopRunNodeParam = new StopRunNodeParam()
                        .setRunWorkId(runWorkId)
                        .setRunNodeIdList(entry.getValue().stream()
                                .map(JobWorkRunNodeLogPo::getRunNodeId)
                                .collect(Collectors.toList()))
                        .setNodeLogIdList(entry.getValue().stream()
                                .map(JobWorkRunNodeLogPo::getNodeLogId)
                                .collect(Collectors.toList()));
                ReturnT<String> stopResult = ExecutorBizProxy.stopRunNode(entry.getKey(), stopRunNodeParam);
                if (stopResult == null || stopResult.getCode() != HandleCodeConstant.HANDLE_CODE_SUCCESS) {
                    log.warn("runWorkId:{} stop command failed, executor:{}, result:{}", runWorkId, entry.getKey(), stopResult);
                }
            } catch (Exception e) {
                log.warn("runWorkId:{} stop command error, executor:{}", runWorkId, entry.getKey(), e);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> rerunLatestRunWork(String workId) {
        if (workId == null || workId.trim().isEmpty()) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业ID不能为空");
        }
        // 一键重跑以页面展示的最新运行作业为准，防止重置历史运行批次。
        JobWorkRunPo latestRunWorkPo = getLatestRunWork(workId);
        if (latestRunWorkPo == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "暂无运行作业记录");
        }

        JobWorkPo jobWorkPo = jobWorkMapper.selectById(workId);
        return runWorkHelper.resetRunWorkSnapshot(jobWorkPo, latestRunWorkPo, latestRunWorkPo.getTurnDate());
    }

    private JobWorkRunPo getLatestRunWork(String workId) {
        return jobRunWorkMapper.selectOne(Wrappers.lambdaQuery(JobWorkRunPo.class)
                .eq(JobWorkRunPo::getWorkId, workId)
                .orderByDesc(JobWorkRunPo::getCreateTime)
                .orderByDesc(JobWorkRunPo::getRunWorkId)
                .last("LIMIT 1"));
    }

    /**
     * 按初始化翻牌日期重跑
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> rerunFromInitTurnDate(String workId) {
        if (workId == null || workId.trim().isEmpty()) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业ID不能为空");
        }
        JobWorkPo jobWorkPo = jobWorkMapper.selectById(workId);
        if (jobWorkPo == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业不存在");
        }
        java.util.Date initTurnDate = resolveInitTurnDate(jobWorkPo);

        JobWorkRunPo initTurnRunWorkPo = getLatestRunWork(workId);
        if (initTurnRunWorkPo == null) {
            ReturnT<JobWorkRunPo> initResult = initRunWorkForTurnDate(jobWorkPo, initTurnDate);
            if (initResult.getCode() != HandleCodeConstant.HANDLE_CODE_SUCCESS) {
                return new ReturnT<>(initResult.getCode(), initResult.getMsg());
            }
            initTurnRunWorkPo = initResult.getContent();
        }
        if (initTurnRunWorkPo.getRunWorkStatus() != null
                && (initTurnRunWorkPo.getRunWorkStatus() == FlowRunStatusEnum.DISPATCHED.getCode()
                || initTurnRunWorkPo.getRunWorkStatus() == FlowRunStatusEnum.RUNNING.getCode())) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "当前运行作业正在执行，不能切换翻牌日期重跑");
        }
        return runWorkHelper.resetRunWorkSnapshot(jobWorkPo, initTurnRunWorkPo, initTurnDate);
    }

    /**
     * 解析初始化翻牌日期
     */
    private Date resolveInitTurnDate(JobWorkPo jobWorkPo) {
        Date initTurnDate = jobWorkPo.getInitTurnDate() == null
                ? DateUtil.parseDate(DateUtil.today())
                : DateUtil.parseDate(DateUtil.formatDate(jobWorkPo.getInitTurnDate()));
        if (jobWorkPo.getInitTurnDate() == null) {
            JobWorkPo updateJobWorkPo = new JobWorkPo();
            updateJobWorkPo.setWorkId(jobWorkPo.getWorkId());
            updateJobWorkPo.setInitTurnDate(initTurnDate);
            jobWorkMapper.updateById(updateJobWorkPo);
            jobWorkPo.setInitTurnDate(initTurnDate);
        }
        return initTurnDate;
    }

    /**
     * 初始化翻牌日期重跑
     */
    private ReturnT<JobWorkRunPo> initRunWorkForTurnDate(JobWorkPo jobWorkPo, java.util.Date turnDate) {
        List<JobWorkNodePo> nodeList = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, jobWorkPo.getWorkId()));
        if (CollUtil.isEmpty(nodeList)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "该运行作业没有运行节点");
        }
        JobWorkRunPo jobWorkRunPo = new JobWorkRunPo();
        jobWorkRunPo.setRunWorkId(IdUtil.getSnowflakeNextIdStr());
        jobWorkRunPo.setWorkId(jobWorkPo.getWorkId());
        jobWorkRunPo.setRunWorkStatus(FlowRunStatusEnum.WAIT.getCode());
        jobWorkRunPo.setWorkType(jobWorkPo.getWorkType());
        jobWorkRunPo.setCreateTime(DateUtil.date());
        if (jobWorkPo.getWorkType() == WorkTypeEnum.TYPE_TURN.getCode()) {
            jobWorkRunPo.setTurnDate(turnDate);
        }
        jobRunWorkMapper.insert(jobWorkRunPo);

        for (JobWorkNodePo nodePo : nodeList) {
            JobWorkRunNodePo runNodePo = new JobWorkRunNodePo();
            runNodePo.setRunNodeId(IdUtil.getSnowflakeNextIdStr());
            runNodePo.setRunWorkId(jobWorkRunPo.getRunWorkId());
            runNodePo.setWorkId(jobWorkPo.getWorkId());
            runNodePo.setNodeId(nodePo.getNodeId());
            runNodePo.setNodeRunStatus(FlowRunStatusEnum.WAIT.getCode());
            if (jobWorkPo.getWorkType() == WorkTypeEnum.TYPE_TURN.getCode()) {
                runNodePo.setTurnDate(turnDate);
            }
            runNodePo.setCreateTime(DateUtil.date());
            runNodePo.setErrorStrategy(nodePo.getErrorStrategy());
            runNodePo.setRetryTimes(nodePo.getRetryTimes());
            jobWorkRunNodeMapper.insert(runNodePo);
        }
        return ReturnT.success(jobWorkRunPo);
    }

    private void validateInitTurnDate(java.util.Date initTurnDate) {
        if (initTurnDate != null && DateUtil.compare(DateUtil.parseDate(DateUtil.formatDate(initTurnDate)),
                DateUtil.parseDate(DateUtil.today())) > 0) {
            throw new IllegalArgumentException("初始化翻牌日期不能晚于今天");
        }
    }

    private Map<String, JobWorkNodePo> getNodeTemplateMap(String workId) {
        List<JobWorkNodePo> nodeList = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, workId));
        if (CollUtil.isEmpty(nodeList)) {
            return Collections.emptyMap();
        }
        return nodeList.stream().collect(Collectors.toMap(JobWorkNodePo::getNodeId, x -> x, (oldValue, newValue) -> oldValue));
    }

}
