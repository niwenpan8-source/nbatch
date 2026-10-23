package com.nbatch.job.admin.core.helper;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.WorkTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 管理运行节点及所属作业的状态转换和失败策略。
 */
@Component
@RequiredArgsConstructor
public class RunNodeStateHelper {

    private static final String ERROR_STRATEGY_SKIP = "skip";

    private static final String ERROR_STRATEGY_RETRY = "retry";

    private static final int RETRY_INITIAL_DELAY_SECONDS = 1;

    private static final int RETRY_MAX_DELAY_SECONDS = 10;

    private final IJobWorkNodeMapper jobWorkNodeMapper;

    private final IJobWorkMapper jobWorkMapper;

    /**
     * 节点重试失败次数。增加这个变量的目的是为了掩饰重试失败次数，避免重试失败次数过多导致节点被频繁重试。
     */
    private final ConcurrentMap<String, Integer> retryFailureCountMap = new ConcurrentHashMap<>();

    /**
     * 节点重试退避时间。增加这个变量的目的是为了掩饰重试失败次数，避免重试失败次数过多导致节点被频繁重试。
     */
    private final ConcurrentMap<String, Long> retryReadyTimeMap = new ConcurrentHashMap<>();

    /**
     * 判断节点是否已经达到下一次重试允许下发的时间。
     */
    public boolean isReadyToDispatch(String nodeId) {
        if (StrUtil.isBlank(nodeId)) {
            return true;
        }
        Long readyTime = retryReadyTimeMap.get(nodeId);
        if (readyTime == null) {
            return true;
        }
        if (System.currentTimeMillis() < readyTime) {
            return false;
        }
        retryReadyTimeMap.remove(nodeId, readyTime);
        return true;
    }

    /**
     * 清理节点完成、跳过或停止后的重试退避状态。
     */
    public void clearRetryState(String nodeId) {
        if (StrUtil.isBlank(nodeId)) {
            return;
        }
        retryReadyTimeMap.remove(nodeId);
        retryFailureCountMap.remove(nodeId);
    }

    /**
     * 按节点事件推进运行作业状态；下发状态只允许从待执行推进，仍有停止中节点时不允许推进为已停止。
     */
    public void transitionWorkStatus(String workId, Integer status) {
        if (StrUtil.isBlank(workId) || status == null) {
            return;
        }
        if (Objects.equals(status, FlowRunStatusEnum.STOPPED.getCode()) && hasStoppingNode(workId)) {
            return;
        }
        jobWorkMapper.update(null, Wrappers.lambdaUpdate(JobWorkPo.class)
                .set(JobWorkPo::getRunWorkStatus, status)
                .eq(JobWorkPo::getWorkId, workId)
                .eq(Objects.equals(status, FlowRunStatusEnum.DISPATCHED.getCode()),
                        JobWorkPo::getRunWorkStatus, FlowRunStatusEnum.WAIT.getCode()));
    }

    /**
     * 仅在作业尚未进入异常、停止中或已停止状态时，按节点事件推进作业状态。
     */
    public void transitionNonTerminalWorkStatus(String workId, Integer status) {
        if (StrUtil.isBlank(workId) || status == null) {
            return;
        }
        if (Objects.equals(status, FlowRunStatusEnum.STOPPED.getCode()) && hasStoppingNode(workId)) {
            return;
        }
        jobWorkMapper.update(null, Wrappers.lambdaUpdate(JobWorkPo.class)
                .set(JobWorkPo::getRunWorkStatus, status)
                .eq(JobWorkPo::getWorkId, workId)
                .ne(JobWorkPo::getRunWorkStatus, FlowRunStatusEnum.EXCEPTION.getCode())
                .ne(JobWorkPo::getRunWorkStatus, FlowRunStatusEnum.STOPPED.getCode())
                .ne(JobWorkPo::getRunWorkStatus, FlowRunStatusEnum.STOPPING.getCode()));
    }

    /**
     * 是否仍有节点等待执行器确认退出。
     */
    private boolean hasStoppingNode(String workId) {
        return jobWorkNodeMapper.selectCount(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, workId)
                .eq(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.STOPPING.getCode())) > 0;
    }

    /**
     * 根据节点失败策略选择跳过、重试或停止。
     */
    public void handleFailure(JobWorkNodePo node) {
        if (stopIfRequested(node)) {
            return;
        }
        String strategy = node.getErrorStrategy();
        // 根据策略处理失败
        if (StrUtil.equals(strategy, ERROR_STRATEGY_SKIP)) {
            skip(node);
        } else if (StrUtil.equals(strategy, ERROR_STRATEGY_RETRY)
                || (StrUtil.isBlank(strategy) && hasRetryTimes(node))) {
            // 如果重试次数没有到0则继续
            if (hasRetryTimes(node)) {
                retry(node);
            } else {
                stop(node);
            }
        } else {
            stop(node);
        }
    }

    /**
     * 检查作业或节点是否已被停止，并在需要时完成节点闭环。
     */
    public boolean stopIfRequested(JobWorkNodePo node) {
        if (node == null) {
            return false;
        }
        if (!(node.getNodeRunStatus() == FlowRunStatusEnum.STOPPED.getCode()
                || node.getNodeRunStatus() == FlowRunStatusEnum.STOPPING.getCode())) {
            JobWorkPo work = jobWorkMapper.selectById(node.getWorkId());
            if (work == null || !(work.getRunWorkStatus() == FlowRunStatusEnum.STOPPED.getCode()
                    || work.getRunWorkStatus() == FlowRunStatusEnum.STOPPING.getCode())) {
                return false;
            }
        }
        // 停止节点以及作业
        stop(node);
        return true;
    }

    /**
     * 强制将仍在执行的节点标记为异常。
     */
    public boolean markException(String nodeId, String workId) {
        int updated = jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                .set(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.EXCEPTION.getCode())
                .eq(JobWorkNodePo::getNodeId, nodeId)
                .in(JobWorkNodePo::getNodeRunStatus,
                        FlowRunStatusEnum.DISPATCHED.getCode(), FlowRunStatusEnum.RUNNING.getCode()));
        if (updated > 0) {
            clearRetryState(nodeId);
            transitionNonTerminalWorkStatus(workId, FlowRunStatusEnum.EXCEPTION.getCode());
        }
        return updated > 0;
    }

    private boolean hasRetryTimes(JobWorkNodePo node) {
        return node.getCurrentRetryTimes() != null
                && (node.getCurrentRetryTimes() == -1 || node.getCurrentRetryTimes() > 0);
    }

    /**
     * 记录失败退避，并将节点重置为等待状态。
     */
    private void retry(JobWorkNodePo node) {
        registerRetryFailure(node);
        Integer retries = node.getCurrentRetryTimes();
        // 如果为-1则不限制重试次数，否则减1
        Integer nextRetries = retries == null || retries == -1 ? retries : Integer.valueOf(retries - 1);
        jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                .set(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode())
                .set(JobWorkNodePo::getCurrentRetryTimes, nextRetries)
                .eq(JobWorkNodePo::getWorkId, node.getWorkId())
                .eq(JobWorkNodePo::getNodeId, node.getNodeId()));
        // 设置作业状态
        transitionNonTerminalWorkStatus(node.getWorkId(), FlowRunStatusEnum.WAIT.getCode());
    }

    /**
     * 按跳过策略结束当前节点。（由于需要兼容节点翻牌日期和作业翻牌日期不一致的情况，所以需要当节点发生错误且类型为跳过，需要修改翻牌时间）
     */
    private void skip(JobWorkNodePo node) {
        JobWorkPo work = jobWorkMapper.selectById(node.getWorkId());
        boolean needsCatchUp = work != null
                && (work.getWorkType() == WorkTypeEnum.TYPE_TURN.getCode())
                && node.getTurnDate() != null && work.getTurnDate() != null
                && DateUtil.compare(node.getTurnDate(), work.getTurnDate()) < 0;
        jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                .set(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.SKIPPED.getCode())
                .set(needsCatchUp, JobWorkNodePo::getTurnDate, work == null ? null : work.getTurnDate())
                .eq(JobWorkNodePo::getWorkId, node.getWorkId())
                .eq(JobWorkNodePo::getNodeId, node.getNodeId()));
        // 清理重试状态
        clearRetryState(node.getNodeId());
    }

    /**
     * 按停止策略结束节点并停止所属作业。
     */
    private void stop(JobWorkNodePo node) {
        jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                .set(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.STOPPED.getCode())
                .eq(JobWorkNodePo::getWorkId, node.getWorkId())
                .eq(JobWorkNodePo::getNodeId, node.getNodeId()));
        clearRetryState(node.getNodeId());
        jobWorkMapper.update(null, Wrappers.lambdaUpdate(JobWorkPo.class)
                .set(JobWorkPo::getRunWorkStatus, FlowRunStatusEnum.STOPPED.getCode())
                .eq(JobWorkPo::getWorkId, node.getWorkId()));
    }

    /**
     * 使用指数退避计算下一次允许下发时间，最长不超过上限。
     */
    private void registerRetryFailure(JobWorkNodePo node) {
        if (node == null || StrUtil.isBlank(node.getNodeId())) {
            return;
        }
        int failures = retryFailureCountMap.merge(node.getNodeId(), 1, Integer::sum);
        long delay = RETRY_INITIAL_DELAY_SECONDS * (1L << Math.min(Math.max(failures - 1, 0), 30));
        int delaySeconds = (int) Math.min(delay, RETRY_MAX_DELAY_SECONDS);
        retryReadyTimeMap.put(node.getNodeId(), System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(delaySeconds));
    }
}
