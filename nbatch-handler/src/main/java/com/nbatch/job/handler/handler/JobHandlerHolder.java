package com.nbatch.job.handler.handler;

import cn.hutool.extra.spring.SpringUtil;
import com.nbatch.job.handler.exception.HandlerException;

import java.util.Map;

import static com.nbatch.job.handler.enums.ExceptionCodeEnum.NOT_SUPPORT_NODE_TYPE;

/**
 * @description: job handler 容器
 * @author: Mr.ni
 * @date: 2025/11/19
 */
public class JobHandlerHolder {

    public static JobHandlerAdapter getHandlerAdapter(String jobType) {
        Map<String, JobHandlerAdapter> jobHandlerAdapterMap = SpringUtil.getBeansOfType(JobHandlerAdapter.class);
        if (null != jobHandlerAdapterMap) {
            for(JobHandlerAdapter jobHandlerAdapter : jobHandlerAdapterMap.values()) {
                if (jobHandlerAdapter.isSupport(jobType)) {
                    return jobHandlerAdapter;
                }
            }
        }
        throw new HandlerException(NOT_SUPPORT_NODE_TYPE.getCode(), "不支持该节点类型");
    }
}
