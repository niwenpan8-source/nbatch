package com.nbatch.job.core.biz.client;

import com.nbatch.job.core.biz.AdminBiz;
import com.nbatch.job.core.biz.model.HandleCallbackParam;
import com.nbatch.job.core.biz.model.RegistryParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.RunNodeLogDetailParam;
import com.nbatch.job.core.util.JobRemotingUtil;

import java.util.List;

/**
 * admin api test
 *
 * @author Mr.ni
 */
public class AdminBizClient implements AdminBiz {

    public AdminBizClient(String addressUrl, String accessToken, int timeout) {
        this.addressUrl = addressUrl;
        this.accessToken = accessToken;
        this.timeout = timeout;

        // valid
        if (!this.addressUrl.endsWith("/")) {
            this.addressUrl = this.addressUrl + "/";
        }
        if (!(this.timeout >= 1 && this.timeout <= 10)) {
            this.timeout = 3;
        }
    }

    private String addressUrl;
    private final String accessToken;
    private int timeout;


    @Override
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        return JobRemotingUtil.postBody(addressUrl + "api/callback", accessToken, timeout, callbackParamList, String.class);
    }

    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return JobRemotingUtil.postBody(addressUrl + "api/registry", accessToken, timeout, registryParam, String.class);
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return JobRemotingUtil.postBody(addressUrl + "api/registryRemove", accessToken, timeout, registryParam, String.class);
    }


    @Override
    public ReturnT<String> callbackRunNodeLogDetail(List<RunNodeLogDetailParam> callbackParamList) {
        return JobRemotingUtil.postBody(addressUrl + "api/callbackRunNodeLogDetail", accessToken, timeout, callbackParamList, String.class);
    }

}
