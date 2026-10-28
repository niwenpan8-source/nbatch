package com.nbatch.job.admin.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 作业流程运行态事件服务。
 */
public interface IJobWorkFlowEventService {

    /**
     * 订阅指定作业的轻量运行态事件。
     *
     * @param workId 作业 ID
     * @return SSE 连接
     */
    SseEmitter subscribe(String workId);
}
