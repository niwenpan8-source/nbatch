package com.nbatch.job.handler.handler;

import cn.hutool.json.JSONObject;

/**
 * @description: 适配器
 * @author: Mr.ni
 * @date: 2025/11/19
 */
public interface JobHandlerAdapter {

    /**
     * 是否支持该作业类型
     * @param jobType 作业类型
     * @return true:支持该作业类型
     */
    boolean isSupport(String jobType);

    /**
     * 执行作业
     * @param executeParam 执行参数
     */
    void execute(JSONObject executeParam);

}
