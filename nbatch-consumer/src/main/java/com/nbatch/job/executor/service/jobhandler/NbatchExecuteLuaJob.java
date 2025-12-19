package com.nbatch.job.executor.service.jobhandler;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.handler.annotation.BatchJob;
import com.nbatch.job.executor.service.helper.ExecuteGaussSqlHelper;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.handler.BeanHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.springframework.stereotype.Component;

import static com.nbatch.job.handler.enums.ExceptionCodeEnum.LUA_SCRIPT_FAIL;

/**
 * 执行 lua 脚本
 *
 * @author Mr.ni
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NbatchExecuteLuaJob {

    private static final String HELPER_NAME = "ExecuteGaussSqlHelper";

    /**
     * 执行lua脚本
     */
    @BatchJob(value = "executeLuaScript")
    public void handleComputation() {
        ExecuteNodeParam param = BeanHandlerContext.getBeanThreadLocal();
        if (param == null || StrUtil.isEmpty(param.getExecuteContentParam())) {
            log.warn("Lua script path is empty.");
            throw new HandlerException(LUA_SCRIPT_FAIL.getCode(), "Lua 脚本路径为空");
        }
        try {
            String scriptPath = param.getExecuteContentParam();
            Globals globals = JsePlatform.standardGlobals();
            // 确保类名也注入到Lua环境中
            globals.set(HELPER_NAME, CoerceJavaToLua.coerce(ExecuteGaussSqlHelper.class));
            // 加载并执行文件（相对路径或绝对路径）
            LuaValue chunk = globals.loadfile(scriptPath);
            if (chunk.isnil()) {
                log.error("Failed to load Lua script from path: {}", scriptPath);
                return;
            }
            chunk.call();

            // 获取 selectRes 并安全地转为字符串
            // 调用 Lua 函数
            LuaValue executionResult = globals.get("executionResult");
            if (executionResult.istable()) {
                // 获取status字段
                LuaValue status = executionResult.get("status");
                // 获取message字段
                LuaValue message = executionResult.get("message");

                String statusStr = StrUtil.toString(status);
                String messageStr = StrUtil.toString(message);
                // 如果为0，则表示成功
                if (!StrUtil.equals("0", statusStr)) {
                    throw new HandlerException(LUA_SCRIPT_FAIL.getCode(), "Lua 脚本执行失败Status:" + statusStr + ", Message:" + messageStr);
                }
                param.pushRunNodeLogDetailCallback(messageStr);
            }
        } catch (Exception e) {
            param.pushRunNodeLogDetailCallback(e.getMessage());
            throw new HandlerException(LUA_SCRIPT_FAIL.getCode(), e);
        }
    }
}
