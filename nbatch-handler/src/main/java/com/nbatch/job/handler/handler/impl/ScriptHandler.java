package com.nbatch.job.handler.handler.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.enums.ScriptTypeEnum;
import com.nbatch.job.core.files.RunNodeFilePath;
import com.nbatch.job.core.util.ScriptUtil;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.handler.JobNodeHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

import static cn.hutool.core.date.DatePattern.PURE_DATETIME_FORMAT;
import static com.nbatch.job.core.enums.NodeTypeEnum.NODE_TYPE_SCRIPT;
import static com.nbatch.job.handler.enums.ExceptionCodeEnum.SCRIPT_FAIL;

/**
 * @description: 脚本处理
 * @author: Mr.ni
 * @date: 2025/11/27
 */
@Slf4j
public class ScriptHandler implements JobNodeHandlerAdapter {
    @Override
    public boolean isSupport(String jobType) {
        return StrUtil.equals(NODE_TYPE_SCRIPT.getCode(), jobType);
    }

    @Override
    public void execute(ExecuteNodeParam nodeParam) throws Exception {
        if (!ScriptTypeEnum.isSupport(nodeParam.getScriptType())) {
            log.error("nodeId:{},脚本运行节点,脚本运行类型[{}]不符合规范", nodeParam.getNodeId(), nodeParam.getScriptType());
            throw new HandlerException(SCRIPT_FAIL.getCode()
                    , StrUtil.format("nodeId:{},脚本运行节点,脚本运行类型[{}]不符合规范", nodeParam.getNodeId(), nodeParam.getScriptType()));
        }
        nodeParam.pushRunNodeLogDetailCallback("=========================任务已经调用=====================================");
        ScriptTypeEnum scriptTypeEnum = ScriptTypeEnum.getByCode(nodeParam.getScriptType());
        // 命令参数
        if (scriptTypeEnum == null) {
            return;
        }
        String cmd = scriptTypeEnum.getCmd();

        // 创建脚本文件
        String scriptFileName = RunNodeFilePath.getScriptPath()
                .concat(File.separator)
                .concat(nodeParam.getNodeId())
                .concat("_")
                .concat(DateUtil.format(nodeParam.getUpdateTime(), PURE_DATETIME_FORMAT))
                .concat(scriptTypeEnum.getSuffix());
        File scriptFile = new File(scriptFileName);
        if (!scriptFile.exists()) {
            ScriptUtil.markScriptFile(scriptFileName, nodeParam.getExecuteContent());
        }

        // 调用脚本
        log.info("----------- nodeId script file:{}-----------", scriptFileName);
        int exitValue = StrUtil.isBlank(nodeParam.getExecuteContentParam())
                ? ScriptUtil.execToFile(nodeParam, cmd, scriptFileName)
                : ScriptUtil.execToFile(nodeParam, cmd, scriptFileName, nodeParam.getExecuteContentParam());

        if (exitValue != 0) {
            throw new HandlerException(SCRIPT_FAIL.getCode(), StrUtil.format("script exit value({}) is failed", exitValue));
        }
    }

    /**
     * 根据 nodeLogId 定位并停止该节点启动的脚本进程。
     */
    @Override
    public void stop(ExecuteNodeParam nodeParam) {
        if (nodeParam != null) {
            ScriptUtil.stopRunNodeProcess(nodeParam.getNodeLogId());
        }
    }
}
