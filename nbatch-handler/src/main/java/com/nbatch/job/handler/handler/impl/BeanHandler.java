package com.nbatch.job.handler.handler.impl;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.executor.BatchJobExecutor;
import com.nbatch.job.core.handler.IJobHandler;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.handler.BeanHandlerContext;
import com.nbatch.job.handler.handler.JobNodeHandlerAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import static com.nbatch.job.core.enums.NodeTypeEnum.NODE_TYPE_BEAN;
import static com.nbatch.job.handler.enums.ExceptionCodeEnum.SYSTEM_ERROR;

/**
 * 方法 job 作业节点 处理
 */
@RequiredArgsConstructor
@Log
public class BeanHandler implements JobNodeHandlerAdapter {

    @Override
    public boolean isSupport(String jobType) {
        return StrUtil.equals(NODE_TYPE_BEAN.getCode(), jobType);
    }

    @Override
    public void execute(ExecuteNodeParam nodeParam) throws Exception {
        try {
            BeanHandlerContext.setBeanThreadLocal(nodeParam);
            IJobHandler iJobHandler = BatchJobExecutor.loadJobHandler(nodeParam.getExecuteHandler());
            if (iJobHandler == null) {
                throw new HandlerException(SYSTEM_ERROR.getCode(), "job bean handler is null");
            }
            iJobHandler.execute();
        } finally {
            BeanHandlerContext.removeBeanThreadLocal();
        }

    }
}
