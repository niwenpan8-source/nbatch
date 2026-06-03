package com.nbatch.job.core.handler.impl;

import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.context.BatchJobHelper;
import com.nbatch.job.core.handler.IJobHandler;
import com.nbatch.job.core.handler.IJobHandlerHolder;
import com.nbatch.job.core.util.SpringUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 作业执行器
 *
 * @author Mr.ni
 */
@Slf4j
public class WorkJobHandler extends IJobHandler {

    @Setter
    private ExecuteWorkParam workNodeParam;

    public WorkJobHandler(ExecuteWorkParam workNodeParam) {
        this.workNodeParam = workNodeParam;
    }

    @Override
    public void execute() throws Exception {
        BatchJobHelper.log("作业执行器开始执行作业：{}", workNodeParam.getJobId());
        IJobHandlerHolder handlerHolder = (IJobHandlerHolder) SpringUtil.getBean("jobHandlerHolder");
        workNodeParam.setWorkId(workNodeParam.getWorkId());
        handlerHolder.handle(workNodeParam);
        BatchJobHelper.handleSuccess("作业节点执行完成");
        BatchJobHelper.log("作业执行器执行作业结束：{}", workNodeParam.getJobId());
    }
}
