package com.nbatch.job.admin.controller;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.admin.controller.annotation.PermissionLimit;
import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.core.biz.AdminBiz;
import com.nbatch.job.core.biz.model.RegistryParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.util.GsonTool;
import com.nbatch.job.core.util.JobRemotingUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;


/**
 * API 接口控制器
 */
@Controller
@RequestMapping("/api")
public class JobApiController {

    @Resource
    private AdminBiz adminBiz;

    /**
     * API 请求入口
     *
     * @param uri 地址
     * @param data 数据
     */
    @RequestMapping("/{uri}")
    @ResponseBody
    @PermissionLimit(limit=false)
    public ReturnT<String> api(HttpServletRequest request, @PathVariable("uri") String uri, @RequestBody(required = false) String data) {

        // 校验请求
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "无效请求，不支持该 HTTP 方法。");
        }
        if (StrUtil.isBlank(uri)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "无效请求，URI 映射不能为空。");
        }
        if (StrUtil.isNotBlank(JobAdminConfig.getAdminConfig().getAccessToken())
                && !JobAdminConfig.getAdminConfig().getAccessToken().equals(request.getHeader(JobRemotingUtil.BATCH_JOB_ACCESS_TOKEN))) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "访问令牌错误。");
        }

        // 根据 URI 映射服务
        switch (uri) {
            case "registry": {
                RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
                return adminBiz.registry(registryParam);
            }
            case "registryRemove": {
                RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
                return adminBiz.registryRemove(registryParam);
            }
            default:
                return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "无效请求，未找到 URI 映射：" + uri);
        }

    }

}
