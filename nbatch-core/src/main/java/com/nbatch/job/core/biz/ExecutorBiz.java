package com.nbatch.job.core.biz;

import com.nbatch.job.core.biz.model.*;

/**
 * 执行器服务
 */
public interface ExecutorBiz {

    /**
     * 心跳
     */
    ReturnT<String> beat();

    /**
     * 执行作业
     */
    ReturnT<String> runWork(ExecuteWorkParam executeWorkParam);

    /**
     * 停止运行节点
     */
    ReturnT<String> stopRunNode(StopRunNodeParam stopRunNodeParam);

    /**
     * 批量查询运行节点在执行器中的真实状态。
     */
    ReturnT<RunNodeStatusQueryResult> queryRunNodeStatus(RunNodeStatusQueryParam queryParam);

    /**
     * 拉取运行节点本地事件日志。
     */
    ReturnT<RunNodeLogPullResult> pullRunNodeLog(RunNodeLogPullParam pullParam);

    /**
     * 确认运行节点本地事件日志。
     */
    ReturnT<String> ackRunNodeLog(RunNodeLogAckParam ackParam);

    /**
     * 拉取运行节点本地详细日志。
     */
    ReturnT<RunNodeLogPullResult> pullRunNodeLogDetail(RunNodeLogPullParam pullParam);

    /**
     * 确认运行节点本地详细日志。
     */
    ReturnT<String> ackRunNodeLogDetail(RunNodeLogAckParam ackParam);

}
