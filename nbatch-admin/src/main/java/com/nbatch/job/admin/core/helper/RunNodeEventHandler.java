package com.nbatch.job.admin.core.helper;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogPo;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.core.biz.model.RunNodeLogEventParam;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.WorkTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;

import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.FAIL;
import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.HEARTBEAT;
import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.STARTED;
import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.STOPPED;
import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.SUCCESS;

/**
 * 负责处理执行器发送的节点事件，并完成节点与日志状态转换。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RunNodeEventHandler {

    private final IJobWorkNodeMapper jobWorkNodeMapper;
    private final IJobWorkMapper jobWorkMapper;
    private final RunNodeLogHelper runNodeLogHelper;
    private final RunNodeStateHelper runNodeStateHelper;
    private final RunNodeStatusReconciler runNodeStatusReconciler;

    /**
     * 处理执行器事件，保证节点状态与日志状态在同一事务内完成。
     */
    @Transactional(rollbackFor = Exception.class)
    public void handle(RunNodeLogEventParam event) {
        if (event == null || StrUtil.isBlank(event.getNodeLogId())) {
            return;
        }
        // 得到该返回事件的日志
        JobWorkRunNodeLogPo oldLog = fillInEventParam(event);
        // 判断时间和日志（日志回调时间不是空代表现在存在未闭环的事件）是否合规
        if (StrUtil.isBlank(event.getWorkId()) || StrUtil.isBlank(event.getNodeId())
                || (oldLog != null && oldLog.getCallBackTime() != null)) {
            return;
        }
        if (!isCurrentEvent(event, oldLog)) {
            log.debug("忽略过期的运行节点事件，nodeId：{}，nodeLogId：{}，事件类型：{}，offset：{}",
                    event.getNodeId(), event.getNodeLogId(), event.getEventType(), event.getOffset());
            return;
        }
        runNodeStatusReconciler.clearConfirmCount(event.getNodeLogId());
        // 得到事件类型
        String eventType = event.getEventType();
        if (STARTED.getValue().equals(eventType)) {
            if (oldLog == null || oldLog.getHandleCode() == null || oldLog.getHandleCode() <= 0) {
                markStarted(event, "运行节点执行中");
            }
        // 心跳时间，更新心跳时间
        } else if (HEARTBEAT.getValue().equals(eventType)) {
            runNodeLogHelper.heartbeat(event.getNodeLogId());
        } else if (SUCCESS.getValue().equals(eventType) || FAIL.getValue().equals(eventType)) {
            handleFinished(event);
        } else if (STOPPED.getValue().equals(eventType)) {
            markStopped(event);
        } else {
            log.warn("未知的运行节点事件类型，nodeId：{}，nodeLogId：{}，事件类型：{}，offset：{}",
                    event.getNodeId(), event.getNodeLogId(), eventType, event.getOffset());
        }
    }

    /**
     * 处理成功或失败事件，并完成节点状态和日志闭环。
     */
    private void handleFinished(RunNodeLogEventParam event) {
        JobWorkRunNodeLogPo oldLog = runNodeLogHelper.get(event.getNodeLogId());
        if (oldLog != null && oldLog.getCallBackTime() != null || isStopped(event)) {
            return;
        }
        int handleCode = event.getHandleCode() == null ? HandleCodeConstant.HANDLE_CODE_FAIL : event.getHandleCode();
        if (handleCode == HandleCodeConstant.HANDLE_CODE_SUCCESS) {
            markCompleted(event);
        } else {
            JobWorkNodePo node = findCurrentNode(event);
            if (node != null) {
                runNodeStateHelper.handleFailure(node);
            }
        }
        // 完成本次日志闭环，对于失败时间重试需要重新创建日志
        finishLog(event, handleCode, event.getHandleMsg());
    }

    /**
     * 节点成功后按当前批次完成，或推进到下一天继续补跑。
     */
    private void markCompleted(RunNodeLogEventParam event) {
        JobWorkNodePo node = jobWorkNodeMapper.selectById(event.getNodeId());
        JobWorkPo work = jobWorkMapper.selectById(event.getWorkId());
        if (node == null || work == null) {
            return;
        }

        // 如果节点翻牌时间小于作业日期，表明想在节点还没有追齐，则将节点推进到下一天继续补跑
        if ((work.getWorkType() == WorkTypeEnum.TYPE_TURN.getCode())
                && node.getTurnDate() != null && work.getTurnDate() != null
                && DateUtil.compare(node.getTurnDate(), work.getTurnDate()) < 0) {
            advanceTurnNode(node, work);
            return;
        }
        jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                .set(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.COMPLETE.getCode())
                .eq(JobWorkNodePo::getWorkId, event.getWorkId())
                .eq(JobWorkNodePo::getNodeId, event.getNodeId())
                .eq(event.getTurnDate() != null, JobWorkNodePo::getTurnDate, event.getTurnDate())
                .in(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode(),
                        FlowRunStatusEnum.DISPATCHED.getCode(), FlowRunStatusEnum.RUNNING.getCode()));
        runNodeStateHelper.clearRetryState(event.getNodeId());
    }

    /**
     * 翻牌节点尚未追平作业日期时，将节点推进到下一天等待执行。
     */
    private void advanceTurnNode(JobWorkNodePo node, JobWorkPo work) {
        Date nextTurnDate = DateUtil.offsetDay(DateUtil.beginOfDay(node.getTurnDate()), 1);
        int updated = jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                .set(JobWorkNodePo::getTurnDate, nextTurnDate)
                .set(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode())
                .set(JobWorkNodePo::getCurrentRetryTimes, node.getRetryTimes())
                .eq(JobWorkNodePo::getWorkId, work.getWorkId())
                .eq(JobWorkNodePo::getNodeId, node.getNodeId())
                .eq(JobWorkNodePo::getTurnDate, node.getTurnDate())
                .in(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode(),
                        FlowRunStatusEnum.DISPATCHED.getCode(), FlowRunStatusEnum.RUNNING.getCode()));
        if (updated == 1) {
            runNodeStateHelper.clearRetryState(node.getNodeId());
            runNodeStateHelper.transitionNonTerminalWorkStatus(work.getWorkId(), FlowRunStatusEnum.WAIT.getCode());
        }
    }

    /**
     * 更新已有日志；日志不存在时补建一条已完成日志。
     */
    private void finishLog(RunNodeLogEventParam event, Integer handleCode, String handleMsg) {
        if (runNodeLogHelper.get(event.getNodeLogId()) == null) {
            runNodeLogHelper.create(event, handleCode, handleMsg, LocalDateTime.now(), null, event.getTimestamp());
        } else {
            runNodeLogHelper.finish(event.getNodeLogId(), handleCode, handleMsg, event.getTimestamp());
        }
    }

    /**
     * 将节点从等待或已下发状态更新为执行中。
     */
    private void markStarted(RunNodeLogEventParam event, String handleMsg) {
        int updated = jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                .set(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.RUNNING.getCode())
                .eq(JobWorkNodePo::getWorkId, event.getWorkId())
                .eq(JobWorkNodePo::getNodeId, event.getNodeId())
                .in(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.DISPATCHED.getCode(),
                        FlowRunStatusEnum.WAIT.getCode()));
        // 如果修改没有成功
        if (updated <= 0) {
            if (!isStopped(event) && runNodeLogHelper.get(event.getNodeLogId()) != null) {
                runNodeLogHelper.markStarted(event.getNodeLogId(), event.getTimestamp(), handleMsg);
            }
            return;
        }
        if (runNodeLogHelper.get(event.getNodeLogId()) == null) {
            runNodeLogHelper.create(event, 0, handleMsg, null, event.getTimestamp(), null);
        } else {
            runNodeLogHelper.markStarted(event.getNodeLogId(), event.getTimestamp(), handleMsg);
        }
        runNodeStateHelper.transitionWorkStatus(event.getWorkId(), FlowRunStatusEnum.RUNNING.getCode());
    }

    /**
     * 处理停止事件，关闭节点日志并同步作业状态。
     */
    private void markStopped(RunNodeLogEventParam event) {
        // 设置作业状态为停止
        jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                .set(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.STOPPED.getCode())
                .eq(JobWorkNodePo::getWorkId, event.getWorkId()).eq(JobWorkNodePo::getNodeId, event.getNodeId())
                .in(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode(),
                        FlowRunStatusEnum.DISPATCHED.getCode(), FlowRunStatusEnum.RUNNING.getCode(),
                        FlowRunStatusEnum.STOPPING.getCode(), FlowRunStatusEnum.STOPPED.getCode()));
        String handleMsg = StrUtil.blankToDefault(event.getHandleMsg(), "运行节点已停止");
        int handleCode = event.getHandleCode() == null ? HandleCodeConstant.HANDLE_CODE_FAIL : event.getHandleCode();
        // 日志完成闭环
        finishLog(event, handleCode, handleMsg);
        runNodeStateHelper.transitionWorkStatus(event.getWorkId(), FlowRunStatusEnum.STOPPED.getCode());
    }

    private JobWorkNodePo findCurrentNode(RunNodeLogEventParam event) {
        JobWorkNodePo node = jobWorkNodeMapper.selectById(event.getNodeId());
        return node != null && StrUtil.equals(node.getWorkId(), event.getWorkId()) ? node : null;
    }

    /**
     * 判断节点是否已停止。
     */
    private boolean isStopped(RunNodeLogEventParam event) {
        JobWorkNodePo node = jobWorkNodeMapper.selectById(event.getNodeId());
        return node != null && Objects.equals(node.getNodeRunStatus(), FlowRunStatusEnum.STOPPED.getCode());
    }

    /**
     * 使用日志和作业记录 补全事件的作业、节点和批次信息。
     */
    private JobWorkRunNodeLogPo fillInEventParam(RunNodeLogEventParam event) {
        JobWorkRunNodeLogPo log = runNodeLogHelper.get(event.getNodeLogId());
        if (log != null) {
            if (StrUtil.isBlank(event.getWorkId())) {
                event.setWorkId(log.getWorkId());
            }
            if (StrUtil.isBlank(event.getNodeId())) {
                event.setNodeId(log.getNodeId());
            }
            if (event.getTurnDate() == null) {
                event.setTurnDate(log.getTurnDate());
            }
        }
        if (StrUtil.isNotBlank(event.getWorkId()) && event.getWorkType() == null) {
            JobWorkPo work = jobWorkMapper.selectById(event.getWorkId());
            if (work != null) {
                event.setWorkType(work.getWorkType());
            }
        }
        return log;
    }

    /**
     * 仅允许当前未闭环日志对应的事件修改节点状态。
     */
    private boolean isCurrentEvent(RunNodeLogEventParam event, JobWorkRunNodeLogPo oldLog) {
        JobWorkRunNodeLogPo currentLog = runNodeLogHelper.findCurrentUnclosed(event);
        // 判断该未闭环日志与当前系统的未闭环一直是否一致，如果一直，返回 true 继续执行
        if (currentLog != null) {
            return StrUtil.equals(currentLog.getNodeLogId(), event.getNodeLogId());
        }
        // 如果不存在闭环日志，但是还可以根据事件日志查询到，说明该事件是过期的，返回 false 忽略
        if (oldLog != null) {
            return false;
        }
        JobWorkNodePo node = jobWorkNodeMapper.selectById(event.getNodeId());
        // 如果都不满足，则判断节点状态是否为运行状态
        return node != null && StrUtil.equals(node.getWorkId(), event.getWorkId())
                && (node.getNodeRunStatus() == FlowRunStatusEnum.WAIT.getCode()
                || node.getNodeRunStatus() == FlowRunStatusEnum.DISPATCHED.getCode()
                || node.getNodeRunStatus() == FlowRunStatusEnum.RUNNING.getCode());
    }
}
