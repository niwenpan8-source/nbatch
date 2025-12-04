package com.nbatch.job.handler.handler.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.context.BatchJobContext;
import com.nbatch.job.core.context.BatchJobHelper;
import com.nbatch.job.core.enums.ScriptTypeEnum;
import com.nbatch.job.core.log.JobFileAppender;
import com.nbatch.job.core.util.ScriptUtil;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.handler.JobNodeHandlerAdapter;

import java.io.File;

import static cn.hutool.core.date.DatePattern.PURE_DATETIME_FORMAT;
import static com.nbatch.job.handler.enums.ExceptionCodeEnum.SCRIPT_FAIL;
import static com.nbatch.job.handler.enums.NodeTypeEnum.NODE_TYPE_SCRIPT;

/**
 * @description: 脚本处理
 * @author: Mr.ni
 * @date: 2025/11/27
 */
public class ScriptHandler implements JobNodeHandlerAdapter {
    @Override
    public boolean isSupport(String jobType) {
        return StrUtil.equals(NODE_TYPE_SCRIPT.getCode(), jobType);
    }

    @Override
    public void execute(ExecuteNodeParam nodeParam) throws Exception {
        if (!ScriptTypeEnum.isSupport(nodeParam.getScriptType())) {
            BatchJobHelper.handleFail(StrUtil.format("nodeId:{},脚本运行节点,脚本运行类型[{}]不符合规范"
                    , nodeParam.getNodeId(), nodeParam.getScriptType()));
            throw new HandlerException(SCRIPT_FAIL.getCode()
                    , StrUtil.format("nodeId:{},脚本运行节点,脚本运行类型[{}]不符合规范", nodeParam.getNodeId(), nodeParam.getScriptType()));
        }
        nodeParam.pushRunNodeLogDetailCallback("=========================任务已经调用=====================================");
        ScriptTypeEnum scriptTypeEnum = ScriptTypeEnum.getByCode(nodeParam.getScriptType());
        // cmd
        if (scriptTypeEnum == null) {
            return;
        }
        String cmd = scriptTypeEnum.getCmd();

        // make script file
        String scriptFileName = JobFileAppender.getGlueSrcPath()
                .concat(File.separator)
                .concat(nodeParam.getNodeId())
                .concat("_")
                .concat(DateUtil.format(nodeParam.getUpdateTime(), PURE_DATETIME_FORMAT))
                .concat(scriptTypeEnum.getSuffix());
        File scriptFile = new File(scriptFileName);
        if (!scriptFile.exists()) {
            ScriptUtil.markScriptFile(scriptFileName, nodeParam.getExecuteContent());
        }

        // log file
        String logFileName = BatchJobContext.getBatchJobContext().getJobLogFileName();

        // script params：0=param、1=分片序号、2=分片总数
        String[] scriptParams = new String[3];
        scriptParams[0] = nodeParam.getExecuteContentParam();
        scriptParams[1] = String.valueOf(BatchJobContext.getBatchJobContext().getShardIndex());
        scriptParams[2] = String.valueOf(BatchJobContext.getBatchJobContext().getShardTotal());

        // invoke
        BatchJobHelper.log(StrUtil.format("----------- nodeId script file:{}-----------", scriptFileName));
        int exitValue = ScriptUtil.execToFile(nodeParam, cmd, scriptFileName, scriptParams);

        if (exitValue != 0) {
            throw new HandlerException(SCRIPT_FAIL.getCode(), StrUtil.format("script exit value({}) is failed", exitValue));
        }
    }
}
