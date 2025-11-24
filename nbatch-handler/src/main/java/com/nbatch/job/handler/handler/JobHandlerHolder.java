package com.nbatch.job.handler.handler;

import cn.hutool.extra.spring.SpringUtil;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.handler.IJobHandlerHolder;
import com.nbatch.job.handler.exception.HandlerException;

import java.util.Map;

import static com.nbatch.job.handler.enums.ExceptionCodeEnum.NOT_SUPPORT_NODE_TYPE;

/**
 * @description: job handler 容器
 * @author: Mr.ni
 * @date: 2025/11/19
 */
public class JobHandlerHolder implements IJobHandlerHolder {

    /**
     * 获取job handler适配器
     * @param jobType 作业类型
     * @return job handler适配器
     */
    public JobHandlerAdapter getHandlerAdapter(String jobType) {
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

    /**
     * 测试
     */
    @Override
    public void handle(ExecuteWorkParam workNodeParam) {
        System.out.println("测试");
    }
}


