package com.nbatch.job.admin.core.helper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.mapper.IJobLockMapper;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.WorkTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 负责运行作业快照的初始化、恢复、停止和并发锁定。
 */
@Component
@RequiredArgsConstructor
public class RunWorkSnapshotHelper {

    private static final String RUN_WORK_LOCK_PREFIX = "run_work:";

    private final IJobWorkMapper jobWorkMapper;
    private final IJobLockMapper jobLockMapper;
    private final IJobWorkNodeMapper jobWorkNodeMapper;

    /**
     * 初始化运行作业，并把节点快照重置为当前批次的等待状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> initRunWork(String workId) {
        if (StrUtil.isBlank(workId)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_SUCCESS, "作业标识不能为空");
        }
        lockRunWork(workId);
        JobWorkPo work = jobWorkMapper.selectById(workId);
        if (work == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_SUCCESS, "运行作业不存在");
        }
        List<JobWorkNodePo> nodes = loadNodes(workId);
        if (CollUtil.isEmpty(nodes)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_SUCCESS, "运行作业不存在节点");
        }
        // 首次运行使用初始化批次日期，后续运行按作业类型推进日期。
        if (work.getRunWorkStatus() == null) {
            resetWork(work, resolveInitRunDate());
        } else {
            if (hasActiveNode(workId)) {
                return ReturnT.error("存在执行中的节点，不能初始化新批次");
            }
            if (!ObjUtil.equals(work.getRunWorkStatus(), FlowRunStatusEnum.COMPLETE.getCode())) {
                return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_SUCCESS, "当前作业未完成，不能初始化新批次");
            }
            Date turnDate = Objects.equals(work.getWorkType(), WorkTypeEnum.TYPE_TURN.getCode())
                    ? DateUtil.offset(resolveLastTurnDate(work), DateField.DAY_OF_MONTH, 1)
                    : DateUtil.parseDate(DateUtil.today());
            if ((work.getWorkType() == WorkTypeEnum.TYPE_TURN.getCode())
                    && DateUtil.compare(turnDate, DateUtil.parseDate(DateUtil.today())) > 0) {
                return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_SUCCESS, "当天翻牌任务已初始化");
            }
            resetWork(work, turnDate);
        }
        resetNodes(work, nodes);
        return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_SUCCESS, "作业已初始化，等待调度执行");
    }

    /**
     * 获取作业行锁，要求调用方已经处于事务中。
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockRunWork(String workId) {
        if (StrUtil.isBlank(workId)) {
            throw new IllegalArgumentException("workId不能为空");
        }
        String lockName = RUN_WORK_LOCK_PREFIX + workId;
        jobLockMapper.insertIgnore(lockName);
        jobLockMapper.lockByName(lockName);
    }

    /**
     * 重置正式运行作业快照，并同步重置所有节点状态。
     */
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> resetRunWorkSnapshot(JobWorkPo jobWorkPo, JobWorkPo runWorkPo, Date turnDate) {
        return resetRunWorkSnapshot(jobWorkPo, runWorkPo, turnDate, false);
    }

    /**
     * 重置正式运行作业快照；强制重跑允许旧节点尚未退出。
     */
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> resetRunWorkSnapshot(JobWorkPo jobWorkPo, JobWorkPo runWorkPo, Date turnDate,
                                                boolean force) {
        if (jobWorkPo == null || runWorkPo == null) {
            return ReturnT.error("运行作业不存在");
        }
        lockRunWork(jobWorkPo.getWorkId());
        JobWorkPo current = jobWorkMapper.selectById(jobWorkPo.getWorkId());
        if (current == null || (!force && (!isResettable(current.getRunWorkStatus())
                || hasActiveNode(current.getWorkId())))) {
            return ReturnT.error("当前作业不能重跑");
        }
        List<JobWorkNodePo> nodes = loadNodes(current.getWorkId());
        if (CollUtil.isEmpty(nodes)) {
            return ReturnT.error("运行作业不存在节点");
        }
        resetWork(runWorkPo, turnDate);
        resetNodes(runWorkPo, nodes);
        return ReturnT.SUCCESS;
    }

    /**
     * 恢复指定停止或异常节点，使用模板重置节点重试次数。
     */
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> recoverRunWorkSnapshot(JobWorkPo work, List<JobWorkNodePo> nodes) {
        return recoverRunWorkSnapshot(work, nodes, false);
    }

    /**
     * 恢复指定异常或停止节点；强制恢复允许停止中的旧节点尚未退出。
     */
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> recoverRunWorkSnapshot(JobWorkPo work, List<JobWorkNodePo> nodes, boolean force) {
        if (work == null || CollUtil.isEmpty(nodes)) {
            return ReturnT.error("运行作业或恢复节点不能为空");
        }
        lockRunWork(work.getWorkId());
        JobWorkPo current = jobWorkMapper.selectById(work.getWorkId());
        // 普通恢复前运行作业不能存在活动节点；强制恢复跳过旧节点退出确认。
        if (current == null || (!force && hasActiveNode(work.getWorkId()))) {
            return ReturnT.error("存在执行中的节点，不能恢复作业");
        }
        Map<String, JobWorkNodePo> nodeMap = loadNodes(work.getWorkId()).stream()
                .collect(Collectors.toMap(JobWorkNodePo::getNodeId, Function.identity(), (left, right) -> left));
        List<Integer> recoverableWorkStatuses = force
                ? Arrays.asList(FlowRunStatusEnum.STOPPED.getCode(), FlowRunStatusEnum.UNINITIALIZED.getCode(), FlowRunStatusEnum.EXCEPTION.getCode(),
                        FlowRunStatusEnum.STOPPING.getCode())
                : Arrays.asList(FlowRunStatusEnum.STOPPED.getCode(), FlowRunStatusEnum.UNINITIALIZED.getCode(), FlowRunStatusEnum.EXCEPTION.getCode());

        // 修改作业节点状态
        for (JobWorkNodePo node : nodes) {
            JobWorkNodePo template = nodeMap.get(node.getNodeId());
            boolean uninitialized = Objects.equals(node.getNodeRunStatus(),
                    FlowRunStatusEnum.UNINITIALIZED.getCode());
            int updated = jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                    .set(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode())
                    .set(uninitialized, JobWorkNodePo::getTurnDate, current.getTurnDate())
                    .set(JobWorkNodePo::getCurrentRetryTimes,
                            template == null ? node.getCurrentRetryTimes() : template.getRetryTimes())
                    .eq(JobWorkNodePo::getWorkId, work.getWorkId()).eq(JobWorkNodePo::getNodeId, node.getNodeId())
                    .in(JobWorkNodePo::getNodeRunStatus, recoverableWorkStatuses));
            if (updated != 1) {
                throw new IllegalStateException("运行节点状态已变化，nodeId：" + node.getNodeId());
            }
        }
        // 修改作业状态
        int updated = jobWorkMapper.update(null, Wrappers.lambdaUpdate(JobWorkPo.class)
                .set(JobWorkPo::getRunWorkStatus, FlowRunStatusEnum.WAIT.getCode())
                .eq(JobWorkPo::getWorkId, work.getWorkId())
                .in(JobWorkPo::getRunWorkStatus, recoverableWorkStatuses));
        if (updated != 1) {
            throw new IllegalStateException("运行作业状态已变化，workId：" + work.getWorkId());
        }
        return ReturnT.SUCCESS;
    }

    /**
     * 停止整个运行作业。已下发或运行中的节点进入停止中，保留未闭环日志等待执行器确认。
     */
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> stopRunWorkSnapshot(JobWorkPo work, List<JobWorkNodePo> nodes) {
        if (work == null) {
            return ReturnT.error("运行作业不存在");
        }
        boolean hasRemoteTask = nodes != null && nodes.stream().anyMatch(this::hasRemoteTask);
        int updated = jobWorkMapper.update(null, Wrappers.lambdaUpdate(JobWorkPo.class)
                .set(JobWorkPo::getRunWorkStatus, hasRemoteTask
                        ? FlowRunStatusEnum.STOPPING.getCode() : FlowRunStatusEnum.STOPPED.getCode())
                .eq(JobWorkPo::getWorkId, work.getWorkId())
                .in(JobWorkPo::getRunWorkStatus, FlowRunStatusEnum.WAIT.getCode(),
                        FlowRunStatusEnum.DISPATCHED.getCode(), FlowRunStatusEnum.RUNNING.getCode()));
        if (updated != 1) {
            throw new IllegalStateException("运行作业状态已变化，workId：" + work.getWorkId());
        }
        stopNodes(work.getWorkId(), nodes);
        return ReturnT.SUCCESS;
    }

    /**
     * 查询作业下所有的节点
     */
    private List<JobWorkNodePo> loadNodes(String workId) {
        return jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, workId));
    }

    /**
     * 查询作业下是否有正在运行的节点(已下发、运行中、停止中)
     */
    private boolean hasActiveNode(String workId) {
        return jobWorkNodeMapper.selectCount(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, workId)
                .in(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.DISPATCHED.getCode(),
                        FlowRunStatusEnum.RUNNING.getCode(), FlowRunStatusEnum.STOPPING.getCode())) > 0;
    }

    /**
     * 获取初始化批次日期
     */
    private Date resolveInitRunDate() {
        return DateUtil.parseDate(DateUtil.today());
    }


    private Date resolveLastTurnDate(JobWorkPo work) {
        return work.getTurnDate() == null ? DateUtil.parseDate(DateUtil.today()) : work.getTurnDate();
    }

    /**
     * 重置作业状态：修改运行作业的批次日期、状态和创建时间。
     */
    private void resetWork(JobWorkPo work, Date turnDate) {
        Date now = DateUtil.date();
        work.setRunWorkStatus(FlowRunStatusEnum.WAIT.getCode());
        work.setTurnDate(turnDate);
        work.setRunCreateTime(now);
        jobWorkMapper.update(null, Wrappers.lambdaUpdate(JobWorkPo.class)
                .set(JobWorkPo::getRunWorkStatus, work.getRunWorkStatus())
                .set(JobWorkPo::getTurnDate, work.getTurnDate())
                .set(JobWorkPo::getRunCreateTime, work.getRunCreateTime())
                .eq(JobWorkPo::getWorkId, work.getWorkId()));
    }

    /**
     * 重置节点状态：按作业快照重置节点状态、批次日期和剩余重试次数。
     */
    private void resetNodes(JobWorkPo work, List<JobWorkNodePo> nodes) {
        for (JobWorkNodePo node : nodes) {
            jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                    .set(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode())
                    .set(JobWorkNodePo::getTurnDate, work.getTurnDate())
                    .set(JobWorkNodePo::getCurrentRetryTimes, node.getRetryTimes())
                    .eq(JobWorkNodePo::getNodeId, node.getNodeId()));
        }
    }

    /**
     * 使用状态条件更新节点，防止覆盖并发线程已经产生的状态变化。
     */
    private void stopNodes(String workId, List<JobWorkNodePo> nodes) {
        if (CollUtil.isEmpty(nodes)) {
            return;
        }
        // 这里为什么要加上状态，为了防止并发运行的时候，日志那边拉取了任务将状态改为已经停止
        for (JobWorkNodePo node : nodes) {
            // 使用条件更新保护并发场景，状态已被其他线程修改时直接回滚事务。
            boolean hasRemoteTask = hasRemoteTask(node);
            int updated = jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                    .set(JobWorkNodePo::getNodeRunStatus, hasRemoteTask
                            ? FlowRunStatusEnum.STOPPING.getCode() : FlowRunStatusEnum.STOPPED.getCode())
                    .eq(JobWorkNodePo::getWorkId, workId).eq(JobWorkNodePo::getNodeId, node.getNodeId())
                    .in(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode(),
                            FlowRunStatusEnum.DISPATCHED.getCode(), FlowRunStatusEnum.RUNNING.getCode()));
            if (updated != 1) {
                throw new IllegalStateException("运行节点状态已变化，nodeId：" + node.getNodeId());
            }
        }
    }

    /**
     * 是否有远程任务
     */
    private boolean hasRemoteTask(JobWorkNodePo node) {
        return node != null && (Objects.equals(node.getNodeRunStatus(), FlowRunStatusEnum.DISPATCHED.getCode())
                || Objects.equals(node.getNodeRunStatus(), FlowRunStatusEnum.RUNNING.getCode()));
    }

    /**
     * 是否可以重跑
     */
    private boolean isResettable(Integer status) {
        return Objects.equals(status, FlowRunStatusEnum.COMPLETE.getCode())
                || Objects.equals(status, FlowRunStatusEnum.EXCEPTION.getCode())
                || Objects.equals(status, FlowRunStatusEnum.STOPPED.getCode());
    }
}
