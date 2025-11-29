package com.nbatch.job.handler.handler;

import com.nbatch.job.core.biz.model.ExecuteNodeParam;

/**
 * @description: 适配器
 * @author: Mr.ni
 * @date: 2025/11/19
 */
public interface JobNodeHandlerAdapter {

    /**
     * 是否支持该作业类型
     * @param jobType 作业类型
     * @return true:支持该作业类型
     */
    boolean isSupport(String jobType);

    /**
     * 执行作业
     * @param nodeParam 执行参数
     */
    void execute(ExecuteNodeParam nodeParam) throws Exception;

}
