package com.nbatch.job.admin.core.helper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogDetailPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunPo;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.mapper.IJobLockMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeLogDetailMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeLogMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeMapper;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.WorkTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @description: 运行作业帮助类
 * @author: Mr.ni
 * @date: 2025/11/20
 */
@Component
@RequiredArgsConstructor
public class RunWorkHelper {

    private static final String RUN_WORK_INIT_LOCK_PREFIX = "run_work_init_lock:";

    private final IJobWorkMapper jobWorkMapper;

    private final IJobLockMapper jobLockMapper;

    private final IJobWorkRunMapper jobRunWorkMapper;

    private final IJobWorkNodeMapper jobWorkNodeMapper;

    private final IJobWorkRunNodeMapper jobWorkRunNodeMapper;

    private final IJobWorkRunNodeLogMapper jobWorkRunNodeLogMapper;

    private final IJobWorkRunNodeLogDetailMapper jobWorkRunNodeLogDetailMapper;

    /**
     * 初始化运行作业
     *
     * @param workId 作业id
     */
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> initRunWork(String workId) {
        if (workId == null || workId.trim().isEmpty()) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业ID不能为空.");
        }
        // 锁的生命周期是initRunWork，因为有Transactional注解所以发方法执行完毕后会
        lockRunWorkInit(workId);

        JobWorkPo jobWorkPo = jobWorkMapper.selectById(workId);
        if (jobWorkPo == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业不存在.");
        }
        List<JobWorkNodePo> jobWorkNodePos = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, jobWorkPo.getWorkId()));
        if (CollUtil.isEmpty(jobWorkNodePos)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "该运行作业没有运行节点.");
        }

        List<JobWorkRunPo> jobRunWorkList = jobRunWorkMapper
                .selectList(Wrappers.lambdaQuery(JobWorkRunPo.class)
                        .eq(JobWorkRunPo::getWorkId, jobWorkPo.getWorkId())
                        .orderByDesc(JobWorkRunPo::getCreateTime));
        JobWorkRunPo jobRunWorkPo;
        // 作业第一次执行
        if (CollUtil.isEmpty(jobRunWorkList)) {
            Date initRunDate = resolveInitRunDate(jobWorkPo);
            jobRunWorkPo = initRunWork(jobWorkPo.getWorkId(), jobWorkPo.getWorkType(), initRunDate);
            jobRunWorkMapper.insert(jobRunWorkPo);
        } else {
            JobWorkRunPo lastJobRunWorkPo = jobRunWorkList.get(0);
            // 如果上一份运行作业状态不是执行完毕，返回错误,不可继续执行
            if (!Objects.equals(lastJobRunWorkPo.getRunWorkStatus(), FlowRunStatusEnum.COMPLETE.getCode())) {
                return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "可执行的运行作业为空.");
            }
            // 如果是翻牌作业，判断是否是翻牌时间
            if (Objects.equals(jobWorkPo.getWorkType(), WorkTypeEnum.TYPE_TURN.getCode())) {
                DateTime nextTurnDate = DateUtil.offset(resolveLastTurnDate(jobWorkPo, lastJobRunWorkPo), DateField.DAY_OF_MONTH, 1);
                // 如果任务已经执行了今天，返回错误,不可继续执行
                if (DateUtil.compare(nextTurnDate, DateUtil.parseDate(DateUtil.today())) > 0) {
                    return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "今天的翻牌任务已经执行过了.");
                }
                reuseRunWorkSnapshot(lastJobRunWorkPo, nextTurnDate);
                jobRunWorkPo = lastJobRunWorkPo;
            } else {
                reuseRunWorkSnapshot(lastJobRunWorkPo, DateUtil.parseDate(DateUtil.today()));
                jobRunWorkPo = lastJobRunWorkPo;
            }
        }
        syncRunNodesFromTemplate(jobWorkPo, jobRunWorkPo, jobWorkNodePos);
        return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_SUCCESS, "job work started successfully.");
    }

    private void lockRunWorkInit(String workId) {
        String lockName = RUN_WORK_INIT_LOCK_PREFIX.concat(workId);
        jobLockMapper.insertIgnore(lockName);
        jobLockMapper.lockByName(lockName);
    }

    private Date resolveInitTurnDate(JobWorkPo jobWorkPo) {
        return jobWorkPo.getInitTurnDate() == null
                ? DateUtil.parseDate(DateUtil.today())
                : DateUtil.parseDate(DateUtil.formatDate(jobWorkPo.getInitTurnDate()));
    }

    private Date resolveInitRunDate(JobWorkPo jobWorkPo) {
        if (Objects.equals(jobWorkPo.getWorkType(), WorkTypeEnum.TYPE_TURN.getCode())) {
            return resolveInitTurnDate(jobWorkPo);
        }
        return DateUtil.parseDate(DateUtil.today());
    }

    /**
     * 得到翻牌时间
     */
    private Date resolveLastTurnDate(JobWorkPo jobWorkPo, JobWorkRunPo lastJobRunWorkPo) {
        return lastJobRunWorkPo.getTurnDate() == null ? resolveInitTurnDate(jobWorkPo) : lastJobRunWorkPo.getTurnDate();
    }

    /**
     * 重新生成运行作业
     */
    private void reuseRunWorkSnapshot(JobWorkRunPo jobRunWorkPo, Date turnDate) {
        Date now = DateUtil.date();
        jobRunWorkMapper.update(null, Wrappers.lambdaUpdate(JobWorkRunPo.class)
                .set(JobWorkRunPo::getRunWorkStatus, FlowRunStatusEnum.WAIT.getCode())
                .set(JobWorkRunPo::getTurnDate, turnDate)
                .set(JobWorkRunPo::getContextJson, null)
                .set(JobWorkRunPo::getCreateTime, now)
                .eq(JobWorkRunPo::getRunWorkId, jobRunWorkPo.getRunWorkId()));
        jobRunWorkPo.setRunWorkStatus(FlowRunStatusEnum.WAIT.getCode());
        jobRunWorkPo.setTurnDate(turnDate);
        jobRunWorkPo.setContextJson(null);
        jobRunWorkPo.setCreateTime(now);
    }

    /**
     * 重置运行作业快照
     */
    public ReturnT<String> resetRunWorkSnapshot(JobWorkPo jobWorkPo, JobWorkRunPo jobRunWorkPo, Date turnDate) {
        if (jobWorkPo == null || jobRunWorkPo == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "运行作业不存在.");
        }
        List<JobWorkNodePo> jobWorkNodePos = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, jobWorkPo.getWorkId()));
        if (CollUtil.isEmpty(jobWorkNodePos)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "该运行作业没有运行节点.");
        }
        reuseRunWorkSnapshot(jobRunWorkPo, turnDate);
        syncRunNodesFromTemplate(jobWorkPo, jobRunWorkPo, jobWorkNodePos);
        return ReturnT.SUCCESS;
    }

    /**
     * 初始化运行作业
     *
     * @param workId 作业id
     */
    private JobWorkRunPo initRunWork(String workId, Integer workType, Date turnDate) {
        String runNodeId = IdUtil.getSnowflakeNextIdStr();
        JobWorkRunPo jobRunWorkPo = new JobWorkRunPo();
        jobRunWorkPo.setRunWorkId(runNodeId);
        jobRunWorkPo.setWorkId(workId);
        jobRunWorkPo.setRunWorkStatus(FlowRunStatusEnum.WAIT.getCode());
        jobRunWorkPo.setTurnDate(turnDate);
        jobRunWorkPo.setWorkType(workType);
        jobRunWorkPo.setCreateTime(DateUtil.date());
        return jobRunWorkPo;
    }

    /**
     * 初始化运行节点
     */
    private JobWorkRunNodePo initRunNode(JobWorkNodePo elementNode,
                                         JobWorkRunPo jobRunWorkPo) {
        JobWorkRunNodePo jobWorkRunNodePo = new JobWorkRunNodePo();
        jobWorkRunNodePo.setRunWorkId(jobRunWorkPo.getRunWorkId());
        jobWorkRunNodePo.setWorkId(jobRunWorkPo.getWorkId());
        jobWorkRunNodePo.setNodeId(elementNode.getNodeId());
        jobWorkRunNodePo.setNodeRunStatus(FlowRunStatusEnum.WAIT.getCode());
        jobWorkRunNodePo.setTurnDate(jobRunWorkPo.getTurnDate());
        jobWorkRunNodePo.setCreateTime(DateUtil.date());
        // 错误策略
        jobWorkRunNodePo.setErrorStrategy(elementNode.getErrorStrategy());
        jobWorkRunNodePo.setRetryTimes(elementNode.getRetryTimes());
        return jobWorkRunNodePo;
    }

    /**
     * 同步运行作业节点
     */
    private void syncRunNodesFromTemplate(JobWorkPo jobWorkPo,
                                          JobWorkRunPo jobRunWorkPo,
                                          List<JobWorkNodePo> templateNodeList) {
        // 模板节点map
        Map<String, JobWorkNodePo> templateNodeMap = templateNodeList.stream()
                .collect(Collectors.toMap(JobWorkNodePo::getNodeId, Function.identity(), (oldValue, newValue) -> oldValue));
        List<JobWorkRunNodePo> runNodeList = jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                .eq(JobWorkRunNodePo::getRunWorkId, jobRunWorkPo.getRunWorkId()));
        Map<String, JobWorkRunNodePo> runNodeMap = CollUtil.isEmpty(runNodeList)
                ? Collections.emptyMap()
                : runNodeList.stream()
                  .collect(Collectors.toMap(JobWorkRunNodePo::getNodeId, Function.identity(), (oldValue, newValue) -> oldValue));

        deleteRemovedTemplateRunNodes(runNodeList, templateNodeMap.keySet());

        for (JobWorkNodePo templateNode : templateNodeList) {
            JobWorkRunNodePo oldRunNode = runNodeMap.get(templateNode.getNodeId());
            if (oldRunNode == null) {
                jobWorkRunNodeMapper.insert(initRunNode(templateNode, jobRunWorkPo));
            } else {
                jobWorkRunNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkRunNodePo.class)
                        .set(JobWorkRunNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode())
                        .set(JobWorkRunNodePo::getTurnDate, jobRunWorkPo.getTurnDate())
                        .set(JobWorkRunNodePo::getCreateTime, DateUtil.date())
                        .set(JobWorkRunNodePo::getErrorStrategy, templateNode.getErrorStrategy())
                        .set(JobWorkRunNodePo::getRetryTimes, templateNode.getRetryTimes())
                        .set(JobWorkRunNodePo::getStartTime, null)
                        .set(JobWorkRunNodePo::getEndTime, null)
                        .eq(JobWorkRunNodePo::getRunNodeId, oldRunNode.getRunNodeId()));
            }
        }
    }

    /**
     * 删除掉节点模版表中不存在的节点的所有信息，包括日志信息以及运行节点信息
     */
    private void deleteRemovedTemplateRunNodes(List<JobWorkRunNodePo> runNodeList, Set<String> templateNodeIdSet) {
        if (CollUtil.isEmpty(runNodeList)) {
            return;
        }
        List<String> removedRunNodeIdList = runNodeList.stream()
                .filter(runNode -> !templateNodeIdSet.contains(runNode.getNodeId()))
                .map(JobWorkRunNodePo::getRunNodeId)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(removedRunNodeIdList)) {
            return;
        }
        jobWorkRunNodeLogDetailMapper.delete(Wrappers.lambdaQuery(JobWorkRunNodeLogDetailPo.class)
                .in(JobWorkRunNodeLogDetailPo::getRunNodeId, removedRunNodeIdList));
        jobWorkRunNodeLogMapper.delete(Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                .in(JobWorkRunNodeLogPo::getRunNodeId, removedRunNodeIdList));
        jobWorkRunNodeMapper.delete(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                .in(JobWorkRunNodePo::getRunNodeId, removedRunNodeIdList));
    }

    /**
     * 删除运行作业(只保留30天的记录)
     */
    public void deleteRunWork() {
        List<JobWorkRunPo> jobRunWorkPoList = jobRunWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkRunPo.class)
                .in(JobWorkRunPo::getRunWorkStatus,
                        FlowRunStatusEnum.COMPLETE.getCode(),
                        FlowRunStatusEnum.EXCEPTION.getCode(),
                        FlowRunStatusEnum.STOPPED.getCode())
                .lt(JobWorkRunPo::getCreateTime, DateUtil.offsetDay(new Date(), -30)));
        for (JobWorkRunPo jobRunWorkPo : jobRunWorkPoList) {
            JobWorkRunPo latestRunWork = jobRunWorkMapper.selectOne(Wrappers.lambdaQuery(JobWorkRunPo.class)
                    .eq(JobWorkRunPo::getWorkId, jobRunWorkPo.getWorkId())
                    .orderByDesc(JobWorkRunPo::getCreateTime)
                    .orderByDesc(JobWorkRunPo::getRunWorkId)
                    .last("limit 1"));
            if (latestRunWork != null && Objects.equals(latestRunWork.getRunWorkId(), jobRunWorkPo.getRunWorkId())) {
                continue;
            }
            jobWorkRunNodeLogDetailMapper.delete(Wrappers.lambdaQuery(JobWorkRunNodeLogDetailPo.class)
                    .eq(JobWorkRunNodeLogDetailPo::getRunWorkId, jobRunWorkPo.getRunWorkId()));
            jobWorkRunNodeLogMapper.delete(Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                    .eq(JobWorkRunNodeLogPo::getRunWorkId, jobRunWorkPo.getRunWorkId()));
            jobWorkRunNodeMapper.delete(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                    .eq(JobWorkRunNodePo::getRunWorkId, jobRunWorkPo.getRunWorkId()));
            jobRunWorkMapper.deleteById(jobRunWorkPo);
        }
    }

    /**
     * 获取需要继续调度执行的运行作业
     */
    public List<JobWorkRunPo> getAllNeedRunWorkList() {
        return jobRunWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkRunPo.class)
                .in(JobWorkRunPo::getRunWorkStatus,
                        FlowRunStatusEnum.WAIT.getCode(),
                        FlowRunStatusEnum.DISPATCHED.getCode(),
                        FlowRunStatusEnum.RUNNING.getCode())
                .orderByAsc(JobWorkRunPo::getCreateTime));
    }

    /**
     * 修改作业翻牌时间
     */
    public void updateWorkTurnDate() {
        List<JobWorkRunPo> jobRunWorkList = jobRunWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkRunPo.class)
                .in(JobWorkRunPo::getRunWorkStatus,
                        FlowRunStatusEnum.RUNNING.getCode(),
                        FlowRunStatusEnum.DISPATCHED.getCode(),
                        FlowRunStatusEnum.WAIT.getCode()));
        for (JobWorkRunPo jobRunWorkPo : jobRunWorkList) {
            List<JobWorkRunNodePo> jobWorkRunNodePos = jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                    .eq(JobWorkRunNodePo::getRunWorkId, jobRunWorkPo.getRunWorkId()));

            if (CollUtil.isNotEmpty(jobWorkRunNodePos)) {
                long count = jobWorkRunNodePos.stream()
                        .filter(x -> {
                            boolean flag = x.getNodeRunStatus() == FlowRunStatusEnum.COMPLETE.getCode()
                                    || x.getNodeRunStatus() == FlowRunStatusEnum.SKIPPED.getCode();
                            // 这里由于当作业类型为顺序类型时翻牌时间为空，不判断翻牌时间
                            if (flag && x.getTurnDate() != null) {
                                flag = DateUtil.compare(x.getTurnDate(), jobRunWorkPo.getTurnDate()) == 0;
                            }
                            return flag;
                        })
                        .count();
                if (count == jobWorkRunNodePos.size()) {
                    JobWorkRunPo updateJobWork = new JobWorkRunPo()
                            .setRunWorkStatus(FlowRunStatusEnum.COMPLETE.getCode())
                            .setWorkId(jobRunWorkPo.getWorkId())
                            .setRunWorkId(jobRunWorkPo.getRunWorkId());

                    jobRunWorkMapper.updateById(updateJobWork);
                }
            }
        }
    }

}
