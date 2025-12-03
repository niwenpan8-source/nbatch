package com.nbatch.job.admin.controller;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.admin.controller.annotation.PermissionLimit;
import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.core.biz.AdminBiz;
import com.nbatch.job.core.biz.model.HandleCallbackParam;
import com.nbatch.job.core.biz.model.RegistryParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.RunNodeLogDetailParam;
import com.nbatch.job.core.util.GsonTool;
import com.nbatch.job.core.util.JobRemotingUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * api
 * @author Mr.ni
 * @date 2025/11/05
 */
@Controller
@RequestMapping("/api")
public class JobApiController {

    @Resource
    private AdminBiz adminBiz;

    /**
     * api
     *
     * @param uri 地址
     * @param data 数据
     */
    @RequestMapping("/{uri}")
    @ResponseBody
    @PermissionLimit(limit=false)
    public ReturnT<String> api(HttpServletRequest request, @PathVariable("uri") String uri, @RequestBody(required = false) String data) {

        // valid
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, HttpMethod not support.");
        }
        if (StrUtil.isBlank(uri)) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, uri-mapping empty.");
        }
        if (StrUtil.isNotBlank(JobAdminConfig.getAdminConfig().getAccessToken())
                && !JobAdminConfig.getAdminConfig().getAccessToken().equals(request.getHeader(JobRemotingUtil.XXL_JOB_ACCESS_TOKEN))) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "The access token is wrong.");
        }

        // services mapping
        if ("callback".equals(uri)) {
            List<HandleCallbackParam> callbackParamList = GsonTool.fromJson(data, List.class, HandleCallbackParam.class);
            return adminBiz.callback(callbackParamList);
        } else if ("registry".equals(uri)) {
            RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
            return adminBiz.registry(registryParam);
        } else if ("registryRemove".equals(uri)) {
            RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
            return adminBiz.registryRemove(registryParam);
        } else if ("callbackRunNodeLogDetail".equals(uri)) {
            List<RunNodeLogDetailParam> callbackParamList = GsonTool.fromJson(data, List.class, RunNodeLogDetailParam.class);
            return adminBiz.callbackRunNodeLogDetail(callbackParamList);
        } else {
            return new ReturnT<>(ReturnT.FAIL_CODE, "invalid request, uri-mapping(" + uri + ") not found.");
        }

    }

}
