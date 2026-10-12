package com.nbatch.job.core.biz.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
public class StopRunNodeParam implements Serializable {

    private static final long serialVersionUID = 42L;

    private List<String> nodeLogIdList;

    /**
     * 执行器线程池 key，当前与节点类型保持一致；为空时兼容旧逻辑扫描所有线程池。
     */
    private String threadPoolKey;

    /**
     * 是否同时中断正在运行的任务；false 时只移除等待队列中的任务。
     */
    private Boolean stopRunning;
}
