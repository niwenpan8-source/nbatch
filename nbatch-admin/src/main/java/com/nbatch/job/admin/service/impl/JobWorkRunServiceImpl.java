package com.nbatch.job.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.helper.RunWorkHelper;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.service.IJobWorkNodeRunService;
import com.nbatch.job.admin.service.IJobWorkRunService;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.WorkTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 作业流程运行控制：停止、恢复和流程级重跑。
 */
@Service
@Slf4j
public class JobWorkRunServiceImpl implements IJobWorkRunService {

    @Resource
    private IJobWorkMapper jobWorkMapper;
    @Resource
    private IJobWorkNodeMapper jobWorkNodeMapper;
    @Resource
    private RunWorkHelper runWorkHelper;
    @Resource
    private IJobWorkNodeRunService jobWorkNodeRunService;

    /**
     * 恢复已停止或异常的运行快照；重置前必须确认旧执行尝试已经退出。
     */
    @Override
    public ReturnT<String> recoverRunWork(String workId, boolean force) {
        if (StrUtil.isBlank(workId)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业ID不能为空");
        }
        JobWorkPo currentRunWork = findCurrentRunWorkSnapshot(workId);
        if (currentRunWork == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "运行作业不存在");
        }
        if (Objects.equals(currentRunWork.getRunWorkStatus(), FlowRunStatusEnum.RUNNING.getCode())
                || Objects.equals(currentRunWork.getRunWorkStatus(), FlowRunStatusEnum.DISPATCHED.getCode())) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL,
                    "运行作业仍在下发或执行中，请先停止作业，再等待执行器确认任务退出");
        }
        // 只有停止中、已停止或异常的运行作业可以恢复重跑
        if (!Objects.equals(currentRunWork.getRunWorkStatus(), FlowRunStatusEnum.STOPPED.getCode())
                && !Objects.equals(currentRunWork.getRunWorkStatus(), FlowRunStatusEnum.STOPPING.getCode())
                && !Objects.equals(currentRunWork.getRunWorkStatus(), FlowRunStatusEnum.EXCEPTION.getCode())) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "只有停止中、已停止或异常的运行作业可以恢复重跑");
        }
        List<JobWorkNodePo> resetNodes = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, workId)
                .in(JobWorkNodePo::getNodeRunStatus,
                        FlowRunStatusEnum.EXCEPTION.getCode(), FlowRunStatusEnum.STOPPED.getCode(),
                        FlowRunStatusEnum.STOPPING.getCode(), FlowRunStatusEnum.UNINITIALIZED.getCode()));
        if (CollUtil.isEmpty(resetNodes)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "当前运行作业没有可恢复的异常、停止或未初始化节点");
        }
        // 区分强制重跑和非强制重跑
        if (!force) {
            ReturnT<String> exitCheck = jobWorkNodeRunService.stopAndConfirmRunNodesExited(workId, resetNodes);
            if (!exitCheck.isSuccess()) {
                return exitCheck;
            }
        } else {
            requestStopBeforeForcedReset(currentRunWork, resetNodes, "恢复重跑");
        }
        try {
            return force ? runWorkHelper.recoverRunWorkSnapshot(currentRunWork, resetNodes, true)
                    : runWorkHelper.recoverRunWorkSnapshot(currentRunWork, resetNodes);
        } catch (Exception e) {
            log.error("恢复作业流程状态失败，workId：{}", workId, e);
            return ReturnT.error("恢复运行作业失败，节点状态已变化或数据库更新异常");
        }
    }

    /**
     * 先用短事务冻结运行快照，再在事务外通知执行器停止任务。
     */
    @Override
    public ReturnT<String> stopLatestRunWork(String workId) {
        if (StrUtil.isBlank(workId)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业ID不能为空");
        }
        JobWorkPo currentRunWork = findCurrentRunWorkSnapshot(workId);
        if (currentRunWork == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "暂无运行作业记录");
        }
        Integer status = currentRunWork.getRunWorkStatus();
        // 判断当前作业是否在停止中
        boolean stopping = Objects.equals(status, FlowRunStatusEnum.STOPPING.getCode());
        if (status == null || (!Objects.equals(status, FlowRunStatusEnum.WAIT.getCode())
                && !Objects.equals(status, FlowRunStatusEnum.DISPATCHED.getCode())
                && !Objects.equals(status, FlowRunStatusEnum.RUNNING.getCode())
                && !stopping)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL,
                    "只有待执行、已下发、进行中或停止中的运行作业可以停止");
        }
        List<JobWorkNodePo> stopNodes = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, workId)
                .in(JobWorkNodePo::getNodeRunStatus, stopping
                        ? Collections.singletonList(FlowRunStatusEnum.STOPPING.getCode())
                        : Arrays.asList(FlowRunStatusEnum.WAIT.getCode(),
                                FlowRunStatusEnum.DISPATCHED.getCode(), FlowRunStatusEnum.RUNNING.getCode())));
        if (!stopping) {
            try {
                ReturnT<String> stopResult = runWorkHelper.stopRunWorkSnapshot(currentRunWork, stopNodes);
                if (!stopResult.isSuccess()) {
                    return stopResult;
                }
            } catch (Exception e) {
                log.error("停止作业流程状态更新失败，workId：{}", workId, e);
                return ReturnT.error("停止运行作业失败，节点状态已变化或数据库更新异常");
            }
        }
        ReturnT<String> confirmResult = jobWorkNodeRunService.stopAndConfirmRunNodesExited(workId, stopNodes);
        if (confirmResult.isSuccess()) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_SUCCESS, "作业已停止");
        }
        return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_SUCCESS,
                "停止请求已发送，" + confirmResult.getMsg());
    }

    /**
     * 重跑作业。普通作业重跑最新批次，翻牌作业使用指定翻牌日期。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> rerunRunWork(String workId, String turnDate, boolean force) {
        if (StrUtil.isBlank(workId)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业ID不能为空");
        }
        JobWorkPo jobWork = jobWorkMapper.selectById(workId);
        if (jobWork == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业不存在");
        }
        return jobWork.getWorkType() == WorkTypeEnum.TYPE_TURN.getCode()
                ? rerunFromTurnDate(jobWork, turnDate, force) : rerunCurrentRunWorkSnapshot(jobWork, force);
    }

    /**
     * 查询已初始化的当前运行快照。
     * 作业配置与运行快照共用同一条记录；runWorkStatus 为空表示尚未创建运行快照。
     */
    private JobWorkPo findCurrentRunWorkSnapshot(String workId) {
        JobWorkPo work = jobWorkMapper.selectById(workId);
        return work != null && work.getRunWorkStatus() != null ? work : null;
    }

    /**
     * 重跑当前运行快照。
     */
    private ReturnT<String> rerunCurrentRunWorkSnapshot(JobWorkPo jobWork, boolean force) {
        JobWorkPo currentRunWork = findCurrentRunWorkSnapshot(jobWork.getWorkId());
        if (currentRunWork == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "暂无运行作业记录");
        }
        ReturnT<String> exitCheck = force ? requestStopBeforeForcedReset(currentRunWork, null, "重跑")
                : stopAndConfirmWorkNodesExited(currentRunWork);
        if (!exitCheck.isSuccess()) {
            return exitCheck;
        }
        return force ? runWorkHelper.resetRunWorkSnapshot(jobWork, currentRunWork,
                currentRunWork.getTurnDate(), true) : runWorkHelper.resetRunWorkSnapshot(jobWork,
                currentRunWork, currentRunWork.getTurnDate());
    }

    /**
     * 从指定日期重跑。
     */
    private ReturnT<String> rerunFromTurnDate(JobWorkPo jobWork, String turnDateText, boolean force) {
        JobWorkPo currentRunWork = findCurrentRunWorkSnapshot(jobWork.getWorkId());
        if (currentRunWork == null) {
            ReturnT<JobWorkPo> initResult = initRunWorkForTurnDate(jobWork, DateUtil.beginOfDay(DateUtil.date()));
            if (initResult.getCode() != HandleCodeConstant.HANDLE_CODE_SUCCESS) {
                return new ReturnT<>(initResult.getCode(), initResult.getMsg());
            }
            currentRunWork = initResult.getContent();
        }
        if (currentRunWork.getTurnDate() == null) {
            return ReturnT.error("暂无当前运行批次翻牌日期");
        }
        Date rerunTurnDate;
        try {
            rerunTurnDate = StrUtil.isBlank(turnDateText) ? DateUtil.beginOfDay(currentRunWork.getTurnDate())
                    : DateUtil.beginOfDay(DateUtil.parseDate(turnDateText));
        } catch (Exception e) {
            return ReturnT.error("翻牌日期格式不正确，请使用 yyyy-MM-dd");
        }
        if (DateUtil.compare(rerunTurnDate, DateUtil.beginOfDay(DateUtil.date())) > 0) {
            return ReturnT.error("翻牌日期不能晚于今天");
        }
        Integer status = currentRunWork.getRunWorkStatus();
        if (!force && status != null && (Objects.equals(status, FlowRunStatusEnum.DISPATCHED.getCode())
                || Objects.equals(status, FlowRunStatusEnum.RUNNING.getCode()))) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "当前运行作业正在执行，不能切换翻牌日期重跑");
        }
        ReturnT<String> exitCheck = force ? requestStopBeforeForcedReset(currentRunWork, null, "翻牌重跑")
                : stopAndConfirmWorkNodesExited(currentRunWork);
        if (!exitCheck.isSuccess()) {
            return exitCheck;
        }
        return force ? runWorkHelper.resetRunWorkSnapshot(jobWork, currentRunWork, rerunTurnDate, true)
                : runWorkHelper.resetRunWorkSnapshot(jobWork, currentRunWork, rerunTurnDate);
    }

    /**
     * 重跑前停止并确认当前批次的旧节点执行已经退出。
     */
    private ReturnT<String> stopAndConfirmWorkNodesExited(JobWorkPo currentRunWork) {
        List<JobWorkNodePo> nodes = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, currentRunWork.getWorkId()));
        return jobWorkNodeRunService.stopAndConfirmRunNodesExited(currentRunWork.getWorkId(), nodes);
    }

    private ReturnT<String> requestStopBeforeForcedReset(JobWorkPo currentRunWork,
                                                         List<JobWorkNodePo> nodes, String operation) {
        List<JobWorkNodePo> targetNodes = nodes == null
                ? jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, currentRunWork.getWorkId())) : nodes;
        ReturnT<String> stopResult = jobWorkNodeRunService.stopRunNodes(currentRunWork.getWorkId(), targetNodes);
        if (stopResult == null || !stopResult.isSuccess()) {
            log.warn("强制{}前停止旧任务命令未完全确认，workId：{}，结果：{}", operation,
                    currentRunWork.getWorkId(), stopResult == null ? "无响应" : stopResult.getMsg());
        }
        log.warn("强制{}将跳过旧任务退出确认，可能与旧任务并发，workId：{}", operation,
                currentRunWork.getWorkId());
        return ReturnT.SUCCESS;
    }

    /**
     * 初始化运行快照并设置翻牌日期。
     */
    private ReturnT<JobWorkPo> initRunWorkForTurnDate(JobWorkPo jobWork, Date turnDate) {
        List<JobWorkNodePo> nodes = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, jobWork.getWorkId()));
        if (CollUtil.isEmpty(nodes)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "该运行作业没有运行节点");
        }
        Date runCreateTime = DateUtil.date();
        jobWork.setRunWorkStatus(FlowRunStatusEnum.WAIT.getCode());
        jobWork.setRunCreateTime(runCreateTime);
        if (jobWork.getWorkType() == WorkTypeEnum.TYPE_TURN.getCode()) {
            jobWork.setTurnDate(turnDate);
        }
        jobWorkMapper.update(null, Wrappers.lambdaUpdate(JobWorkPo.class)
                .set(JobWorkPo::getRunWorkStatus, FlowRunStatusEnum.WAIT.getCode())
                .set(JobWorkPo::getTurnDate, jobWork.getTurnDate())
                .set(JobWorkPo::getRunCreateTime, runCreateTime)
                .eq(JobWorkPo::getWorkId, jobWork.getWorkId()));
        for (JobWorkNodePo node : nodes) {
            jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                    .set(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode())
                    .set(JobWorkNodePo::getTurnDate, jobWork.getTurnDate())
                    .set(JobWorkNodePo::getCurrentRetryTimes, node.getRetryTimes())
                    .eq(JobWorkNodePo::getNodeId, node.getNodeId()));
        }
        return ReturnT.success(jobWork);
    }
}
