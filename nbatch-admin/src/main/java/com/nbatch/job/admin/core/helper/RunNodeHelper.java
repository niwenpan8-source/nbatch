package com.nbatch.job.admin.core.helper;

import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.biz.model.RunNodeLogEventParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 运行节点的兼容入口，具体职责分别由下发、事件和状态核对组件处理。
 */
@Component
@RequiredArgsConstructor
public class RunNodeHelper {

    private final RunNodeDispatchHelper runNodeDispatchHelper;

    private final RunNodeEventHandler runNodeEventHandler;

    private final RunNodeStatusReconciler runNodeStatusReconciler;

    /**
     * 将待执行节点标记为已下发，并创建本次执行日志。
     */
    public void markRunNodesDispatched(ExecuteWorkParam executeWorkParam) {
        runNodeDispatchHelper.markDispatched(executeWorkParam);
    }

    /**
     * 获取当前批次中满足时间、重试和依赖条件的节点。（可以执行的节点）
     */
    public ExecuteWorkParam getEnableExecuteWork(JobWorkPo jobWorkPo) {
        return runNodeDispatchHelper.findEnabled(jobWorkPo);
    }

    /**
     * 下发结果未知时保留已下发状态，等待执行器事件确认。为什么需要有这个逻辑，
     * 因为当客户端由于一些原因下服务运行过慢，然而客户端并没有挂掉，然后强行闭环，会导致不停地循环执行该任务
     */
    public void markRunNodesDispatchUnknown(ExecuteWorkParam executeWorkParam, String handleMsg) {
        runNodeDispatchHelper.markDispatchUnknown(executeWorkParam, handleMsg);
    }

    /**
     * 下发明确失败时关闭日志，并按节点失败策略处理。
     */
    public void markRunNodesDispatchFailed(ExecuteWorkParam executeWorkParam, String handleMsg) {
        runNodeDispatchHelper.markDispatchFailed(executeWorkParam, handleMsg);
    }

    /**
     * 处理拉取的执行器节点日志事件。
     */
    public void handleRunNodeEvent(RunNodeLogEventParam eventParam) {
        runNodeEventHandler.handle(eventParam);
    }

    /**
     * 定时核对长时间没有事件的运行节点。
     */
    public void updateTimeOutRunNodeStatus() {
        runNodeStatusReconciler.reconcileTimeoutNodes();
    }
}
