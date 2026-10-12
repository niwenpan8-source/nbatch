package com.nbatch.job.admin.core.helper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 运行作业的对外入口，负责调度查询和最终状态汇总。
 */
@Component
@RequiredArgsConstructor
public class RunWorkHelper {

    private final IJobWorkMapper jobWorkMapper;

    private final IJobWorkNodeMapper jobWorkNodeMapper;

    private final RunWorkSnapshotHelper runWorkSnapshotHelper;

    /**
     * 初始化一个新的运行作业批次。
     */
    public ReturnT<String> initRunWork(String workId) {
        return runWorkSnapshotHelper.initRunWork(workId);
    }

    /**
     * 在当前事务中锁定运行作业，避免并发修改同一批次。
     */
    public void lockRunWork(String workId) {
        runWorkSnapshotHelper.lockRunWork(workId);
    }

    /**
     * 重置已完成或已停止作业的运行快照。
     */
    public ReturnT<String> resetRunWorkSnapshot(JobWorkPo jobWorkPo, JobWorkPo runWorkPo, Date turnDate) {
        return runWorkSnapshotHelper.resetRunWorkSnapshot(jobWorkPo, runWorkPo, turnDate);
    }


    public ReturnT<String> resetRunWorkSnapshot(JobWorkPo jobWorkPo, JobWorkPo runWorkPo, Date turnDate,
                                                boolean force) {
        return runWorkSnapshotHelper.resetRunWorkSnapshot(jobWorkPo, runWorkPo, turnDate, force);
    }

    /**
     * 恢复指定异常或停止节点，并重新进入等待状态。
     */
    public ReturnT<String> recoverRunWorkSnapshot(JobWorkPo runWorkPo, List<JobWorkNodePo> resetRunNodeList) {
        return runWorkSnapshotHelper.recoverRunWorkSnapshot(runWorkPo, resetRunNodeList);
    }


    public ReturnT<String> recoverRunWorkSnapshot(JobWorkPo runWorkPo, List<JobWorkNodePo> resetRunNodeList,
                                                  boolean force) {
        return runWorkSnapshotHelper.recoverRunWorkSnapshot(runWorkPo, resetRunNodeList, force);
    }

    /**
     * 停止运行作业及其指定节点。
     */
    public ReturnT<String> stopRunWorkSnapshot(JobWorkPo runWorkPo, List<JobWorkNodePo> stopRunNodeList) {
        return runWorkSnapshotHelper.stopRunWorkSnapshot(runWorkPo, stopRunNodeList);
    }

    /**
     * 查询等待下发、已下发或执行中的作业。
     */
    public List<JobWorkPo> getAllNeedRunWorkList() {
        List<JobWorkPo> workList = jobWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkPo.class)
                .in(JobWorkPo::getRunWorkStatus, FlowRunStatusEnum.WAIT.getCode(),
                        FlowRunStatusEnum.DISPATCHED.getCode(), FlowRunStatusEnum.RUNNING.getCode())
                .orderByAsc(JobWorkPo::getRunCreateTime));
        return CollUtil.isEmpty(workList) ? Collections.emptyList() : workList;
    }

    /**
     * 根据所有节点最终状态汇总并更新作业状态。
     */
    public void updateWorkTurnDate() {
        List<JobWorkPo> workList = getAllNeedRunWorkList();
        for (JobWorkPo work : workList) {
            List<JobWorkNodePo> nodes = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                    .eq(JobWorkNodePo::getWorkId, work.getWorkId()));
            if (CollUtil.isEmpty(nodes)) {
                continue;
            }
            // 只有全部节点都完成当前批次后，才允许更新作业终态。
            boolean allFinished = nodes.stream().allMatch(node -> isFinishedForCurrentTurn(node, work));
            if (!allFinished) {
                continue;
            }
            boolean hasException = nodes.stream().anyMatch(node -> Objects.equals(node.getNodeRunStatus(),
                    FlowRunStatusEnum.EXCEPTION.getCode()));
            boolean hasStopped = nodes.stream().anyMatch(node -> Objects.equals(node.getNodeRunStatus(),
                    FlowRunStatusEnum.STOPPED.getCode()));
            int status = hasException ? FlowRunStatusEnum.EXCEPTION.getCode()
                    : hasStopped ? FlowRunStatusEnum.STOPPED.getCode() : FlowRunStatusEnum.COMPLETE.getCode();
            jobWorkMapper.update(null, Wrappers.lambdaUpdate(JobWorkPo.class)
                    .set(JobWorkPo::getRunWorkStatus, status).eq(JobWorkPo::getWorkId, work.getWorkId()));
        }
    }

    /**
     * 判断节点是否已完成当前批次。（增加逻辑，节点翻牌日期与作业翻牌日期也需要一致）
     */
    private boolean isFinishedForCurrentTurn(JobWorkNodePo node, JobWorkPo work) {
        boolean finished = Objects.equals(node.getNodeRunStatus(), FlowRunStatusEnum.COMPLETE.getCode())
                || Objects.equals(node.getNodeRunStatus(), FlowRunStatusEnum.SKIPPED.getCode());
        return finished && (node.getTurnDate() == null || (work.getTurnDate() != null
                && DateUtil.compare(node.getTurnDate(), work.getTurnDate()) >= 0));
    }
}
