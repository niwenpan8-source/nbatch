package com.nbatch.job.core.handler;

import com.nbatch.job.core.biz.model.ExecuteWorkParam;

/**
 * job handler 容器
 */
public interface IJobHandlerHolder {

    /**
     * 获取job handler适配器
     */
    void handle(ExecuteWorkParam workNodeParam);
}
