package com.nbatch.job.core.handler;

import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.StopRunNodeParam;

public interface RunNodeStopHandler {

    ReturnT<String> stopRunNode(StopRunNodeParam stopRunNodeParam);
}
