package com.nbatch.job.handler.handler.impl;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.handler.handler.JobNodeHandlerAdapter;

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

    }
}
