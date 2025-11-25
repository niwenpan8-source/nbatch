package com.xxl.job.adminbiz;

import com.nbatch.job.core.biz.AdminBiz;
import com.nbatch.job.core.biz.client.AdminBizClient;
import com.nbatch.job.core.biz.model.HandleCallbackParam;
import com.nbatch.job.core.biz.model.RegistryParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.context.BatchJobContext;
import com.nbatch.job.core.enums.RegistryConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * admin api test
 *
 * @author Mr.ni 2017-07-28 22:14:52
 */
public class AdminBizTest {

    // admin-client
    private static String addressUrl = "http://127.0.0.1:8080/job-admin/";
    private static String accessToken = null;
    private static int timeoutSecond = 3;


    @Test
    public void callback() {
        AdminBiz adminBiz = new AdminBizClient(addressUrl, accessToken, timeoutSecond);

        HandleCallbackParam param = new HandleCallbackParam();
        param.setLogId("1");
        param.getLogCallBackParam().setHandleCode(BatchJobContext.HANDLE_CODE_SUCCESS);

        List<HandleCallbackParam> callbackParamList = Arrays.asList(param);

        ReturnT<String> returnT = adminBiz.callback(callbackParamList);

        assertTrue(returnT.getCode() == ReturnT.SUCCESS_CODE);
    }

    /**
     * registry executor
     *
     */
    @Test
    public void registry() {
        AdminBiz adminBiz = new AdminBizClient(addressUrl, accessToken, timeoutSecond);

        RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), "job-executor-example", "127.0.0.1:9999");
        ReturnT<String> returnT = adminBiz.registry(registryParam);

        assertTrue(returnT.getCode() == ReturnT.SUCCESS_CODE);
    }

    /**
     * registry executor remove
     *
     */
    @Test
    public void registryRemove() {
        AdminBiz adminBiz = new AdminBizClient(addressUrl, accessToken, timeoutSecond);

        RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), "job-executor-example", "127.0.0.1:9999");
        ReturnT<String> returnT = adminBiz.registryRemove(registryParam);

        assertTrue(returnT.getCode() == ReturnT.SUCCESS_CODE);

    }

}
