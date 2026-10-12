package com.nbatch.job.handler.handler;

import cn.hutool.core.collection.CollUtil;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.RunNodeStatusQueryParam;
import com.nbatch.job.core.biz.model.RunNodeStatusQueryResult;
import com.nbatch.job.core.biz.model.StopRunNodeParam;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.handler.RunNodeStopHandler;
import com.nbatch.job.handler.utils.BatchThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RunNodeStopHandlerImpl implements RunNodeStopHandler {

    @Override
    public ReturnT<String> stopRunNode(StopRunNodeParam stopRunNodeParam) {
        if (stopRunNodeParam == null || CollUtil.isEmpty(stopRunNodeParam.getNodeLogIdList())) {
            return ReturnT.SUCCESS;
        }
        boolean stopRunning = !Boolean.FALSE.equals(stopRunNodeParam.getStopRunning());
        int stopCount = BatchThreadPoolUtil.stopRunNodes(stopRunNodeParam.getThreadPoolKey(),
                stopRunNodeParam.getNodeLogIdList(), stopRunning);
        log.info("已接受停止运行节点请求，线程池键：{}，节点数量：{}，受影响数量：{}，正在停止：{}",
                stopRunNodeParam.getThreadPoolKey(),
                stopRunNodeParam.getNodeLogIdList().size(), stopCount, stopRunning);
        return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_SUCCESS, "affected:" + stopCount);
    }

    /**
     * 返回每个节点日志标识对应的实时状态。
     */
    @Override
    public ReturnT<RunNodeStatusQueryResult> queryRunNodeStatus(RunNodeStatusQueryParam queryParam) {
        RunNodeStatusQueryResult result = new RunNodeStatusQueryResult()
                .setNodeStateMap(BatchThreadPoolUtil.queryRunNodeStates(
                        queryParam == null ? null : queryParam.getNodeLogIdList()));
        return new ReturnT<>(result);
    }

    /** 先停止节点线程池，确保终态事件可以在事件日志关闭前落盘。 */
    @Override
    public void shutdownRunNodes() {
        BatchThreadPoolUtil.shutdownAllThreadPool();
    }
}
