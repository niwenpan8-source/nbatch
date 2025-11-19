package com.nbatch.job.handler.listener;

import com.nbatch.job.handler.domain.param.TransferObjParam;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @description: 监听器容器
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@Component
public class JobListenerHolder {

    private static final Queue<TransferObjParam> CALLBACK_JOB_EXECUTE_RESULT_QUEUE = new ConcurrentLinkedQueue<>();

    public static void addJobListener(TransferObjParam param) {
        CALLBACK_JOB_EXECUTE_RESULT_QUEUE.add(param);
    }

    public static TransferObjParam getJobListener() {
        return CALLBACK_JOB_EXECUTE_RESULT_QUEUE.poll();
    }



}
