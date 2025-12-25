package com.nbatch.job.executor.service.helper;

import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.handler.BeanHandlerContext;
import lombok.extern.slf4j.Slf4j;

import static com.nbatch.job.handler.enums.ExceptionCodeEnum.LUA_SCRIPT_FAIL;

/**
 * lua脚本执行帮助类
 * @author: Mr.ni
 * @date: 2025/12/22
 */
@Slf4j
public class ExecuteLuaScriptHelper {

    /**
     * 执行更新sql
     */
    public static void addLog(String logStr) {
        System.out.println(logStr);
        ExecuteNodeParam param = BeanHandlerContext.getBeanThreadLocal();
        if (param == null) {
            log.warn("执行日志参数为空");
            throw new HandlerException(LUA_SCRIPT_FAIL.getCode(), "执行日志参数为空");
        }
        param.pushRunNodeLogDetailCallback(logStr);
    }
}
