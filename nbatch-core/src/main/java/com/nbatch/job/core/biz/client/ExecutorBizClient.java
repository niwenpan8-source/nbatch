package com.nbatch.job.core.biz.client;

import com.nbatch.job.core.biz.ExecutorBiz;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.biz.model.IdleBeatParam;
import com.nbatch.job.core.biz.model.KillParam;
import com.nbatch.job.core.biz.model.LogParam;
import com.nbatch.job.core.biz.model.LogResult;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.RunNodeLogAckParam;
import com.nbatch.job.core.biz.model.RunNodeLogPullParam;
import com.nbatch.job.core.biz.model.RunNodeLogPullResult;
import com.nbatch.job.core.biz.model.StopRunNodeParam;
import com.nbatch.job.core.biz.model.TriggerParam;
import com.nbatch.job.core.util.JobRemotingUtil;

/**
 * admin api test
 *
 * @author Mr.ni
 */
public class ExecutorBizClient implements ExecutorBiz {

    public ExecutorBizClient(String addressUrl, String accessToken, int timeout) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;
        this.timeout = timeout;

        // valid
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
    public ReturnT<String> idleBeat(IdleBeatParam idleBeatParam){
        return JobRemotingUtil.postBody(addressUrl+"idleBeat", accessToken, timeout, idleBeatParam, String.class);
    }

    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        return JobRemotingUtil.postBody(addressUrl + "run", accessToken, timeout, triggerParam, String.class);
    }

    @Override
    public ReturnT<String> kill(KillParam killParam) {
        return JobRemotingUtil.postBody(addressUrl + "kill", accessToken, timeout, killParam, String.class);
    }

    @Override
    public ReturnT<String> stopRunNode(StopRunNodeParam stopRunNodeParam) {
        return JobRemotingUtil.postBody(addressUrl + "stopRunNode", accessToken, timeout, stopRunNodeParam, String.class);
    }

    @Override
    public ReturnT<LogResult> log(LogParam logParam) {
        return JobRemotingUtil.postBody(addressUrl + "log", accessToken, timeout, logParam, LogResult.class);
    }

    @Override
    public ReturnT<RunNodeLogPullResult> pullRunNodeLog(RunNodeLogPullParam pullParam) {
        return JobRemotingUtil.postBody(addressUrl + "pullRunNodeLog", accessToken, timeout, pullParam, RunNodeLogPullResult.class);
    }

    @Override
    public ReturnT<String> ackRunNodeLog(RunNodeLogAckParam ackParam) {
        return JobRemotingUtil.postBody(addressUrl + "ackRunNodeLog", accessToken, timeout, ackParam, String.class);
    }

}
