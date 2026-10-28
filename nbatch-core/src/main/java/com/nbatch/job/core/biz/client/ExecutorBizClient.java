package com.nbatch.job.core.biz.client;

import com.nbatch.job.core.biz.ExecutorBiz;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.RunNodeLogAckParam;
import com.nbatch.job.core.biz.model.RunNodeLogPullParam;
import com.nbatch.job.core.biz.model.RunNodeLogPullResult;
import com.nbatch.job.core.biz.model.RunNodeStatusQueryParam;
import com.nbatch.job.core.biz.model.RunNodeStatusQueryResult;
import com.nbatch.job.core.biz.model.StopRunNodeParam;
import com.nbatch.job.core.util.JobRemotingUtil;

/**
 * 执行器接口客户端
 */
public class ExecutorBizClient implements ExecutorBiz {

    public ExecutorBizClient(String addressUrl, String accessToken, int timeout) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;
        this.timeout = timeout;

        // 校验地址
        if (!this.addressUrl.endsWith("/")) {
            this.addressUrl = this.addressUrl + "/";
        }
        if (!(this.timeout >=1 && this.timeout <= 10)) {
            this.timeout = 3;
        }
    }

    private String addressUrl ;
    private final String accessToken;
    private int timeout;


    @Override
    public ReturnT<String> beat() {
        return JobRemotingUtil.postBody(addressUrl+"beat", accessToken, timeout, "", String.class);
    }



    @Override
    public ReturnT<String> runWork(ExecuteWorkParam executeWorkParam) {
        return JobRemotingUtil.postBody(addressUrl + "runWork", accessToken, timeout, executeWorkParam, String.class);
    }


    @Override
    public ReturnT<String> stopRunNode(StopRunNodeParam stopRunNodeParam) {
        return JobRemotingUtil.postBody(addressUrl + "stopRunNode", accessToken, timeout, stopRunNodeParam, String.class);
    }

    /** 调用执行器状态查询接口。 */
    @Override
    public ReturnT<RunNodeStatusQueryResult> queryRunNodeStatus(RunNodeStatusQueryParam queryParam) {
        return JobRemotingUtil.postBody(addressUrl + "queryRunNodeStatus", accessToken, timeout,
                queryParam, RunNodeStatusQueryResult.class);
    }


    @Override
    public ReturnT<RunNodeLogPullResult> pullRunNodeLog(RunNodeLogPullParam pullParam) {
        return JobRemotingUtil.postBody(addressUrl + "pullRunNodeLog", accessToken, timeout, pullParam, RunNodeLogPullResult.class);
    }

    @Override
    public ReturnT<String> ackRunNodeLog(RunNodeLogAckParam ackParam) {
        return JobRemotingUtil.postBody(addressUrl + "ackRunNodeLog", accessToken, timeout, ackParam, String.class);
    }

    @Override
    public ReturnT<RunNodeLogPullResult> pullRunNodeLogDetail(RunNodeLogPullParam pullParam) {
        return JobRemotingUtil.postBody(addressUrl + "pullRunNodeLogDetail", accessToken, timeout,
                pullParam, RunNodeLogPullResult.class);
    }

    @Override
    public ReturnT<String> ackRunNodeLogDetail(RunNodeLogAckParam ackParam) {
        return JobRemotingUtil.postBody(addressUrl + "ackRunNodeLogDetail", accessToken, timeout,
                ackParam, String.class);
    }

}
