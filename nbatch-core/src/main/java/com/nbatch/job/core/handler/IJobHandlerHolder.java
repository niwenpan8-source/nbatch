package com.nbatch.job.core.handler;

import com.nbatch.job.core.biz.model.ExecuteWorkParam;

/**
 * @description: job handler 容器
 * @author: Mr.ni
 * @date: 2025/11/21
 */
public interface IJobHandlerHolder {

    /**
     * 获取job handler适配器
     */
    void handle(ExecuteWorkParam workNodeParam);
}
