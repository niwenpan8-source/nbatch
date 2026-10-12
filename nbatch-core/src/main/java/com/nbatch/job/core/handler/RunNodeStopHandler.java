package com.nbatch.job.core.handler;

import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.RunNodeStatusQueryParam;
import com.nbatch.job.core.biz.model.RunNodeStatusQueryResult;
import com.nbatch.job.core.biz.model.StopRunNodeParam;

public interface RunNodeStopHandler {

    ReturnT<String> stopRunNode(StopRunNodeParam stopRunNodeParam);

    /**
     * 查询当前执行器进程中的运行节点状态。
     */
    ReturnT<RunNodeStatusQueryResult> queryRunNodeStatus(RunNodeStatusQueryParam queryParam);

    /**
     * 系统关闭时停止全部运行节点；默认实现兼容未启用作业节点模块的执行器。
     */
    default void shutdownRunNodes() {
    }
}
