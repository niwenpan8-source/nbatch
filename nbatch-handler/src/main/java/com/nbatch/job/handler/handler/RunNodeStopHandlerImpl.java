package com.nbatch.job.handler.handler;

import cn.hutool.core.collection.CollUtil;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.StopRunNodeParam;
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
        int stopCount = BatchThreadPoolUtil.stopRunNodes(stopRunNodeParam.getNodeLogIdList());
        log.info("runWorkId:{} stop run node request accepted, nodeCount:{}, affected:{}",
                stopRunNodeParam.getRunWorkId(), stopRunNodeParam.getNodeLogIdList().size(), stopCount);
        return ReturnT.SUCCESS;
    }
}
