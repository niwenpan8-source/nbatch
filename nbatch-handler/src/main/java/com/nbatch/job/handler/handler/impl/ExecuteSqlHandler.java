package com.nbatch.job.handler.handler.impl;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.handler.handler.JobNodeHandlerAdapter;
import com.nbatch.job.handler.helper.DialectHelper;
import lombok.RequiredArgsConstructor;

import static com.nbatch.job.handler.enums.NodeTypeEnum.NODE_TYPE_EXECUTE_SQL;

/**
 * @description: 执行sql
 * @author: Mr.ni
 * @date: 2025/11/27
 */
@RequiredArgsConstructor
public class ExecuteSqlHandler implements JobNodeHandlerAdapter {

    private final DialectHelper dialectHelper;

    @Override
    public boolean isSupport(String jobType) {
        return StrUtil.equals(NODE_TYPE_EXECUTE_SQL.getCode(), jobType);
    }

    @Override
    public void execute(ExecuteNodeParam nodeParam) throws Exception {
        System.out.println("执行sql：" + nodeParam.getExecuteContent());
        // 暂时用不到，不做考虑

    }
}
