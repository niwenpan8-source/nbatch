package com.nbatch.job.handler.handler.impl;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.executor.BatchJobExecutor;
import com.nbatch.job.handler.handler.BeanHandlerContext;
import com.nbatch.job.handler.handler.JobNodeHandlerAdapter;
import lombok.RequiredArgsConstructor;

import static com.nbatch.job.handler.enums.NodeTypeEnum.NODE_TYPE_BEAN;

/**
 * @description: 方法 job 作业节点 处理
 * @author: Mr.ni
 * @date: 2025/11/27
 */
@RequiredArgsConstructor
public class BeanHandler implements JobNodeHandlerAdapter {

    @Override
    public boolean isSupport(String jobType) {
        return StrUtil.equals(NODE_TYPE_BEAN.getCode(), jobType);
    }

    @Override
    public void execute(ExecuteNodeParam nodeParam) throws Exception {
        try {
            BeanHandlerContext.setBeanThreadLocal(nodeParam);
            BatchJobExecutor.loadJobHandler(nodeParam.getExecuteHandler()).execute();
        } finally {
            BeanHandlerContext.removeBeanThreadLocal();
        }


    }
}
