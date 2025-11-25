package com.nbatch.job.handler.utils;


import cn.hutool.core.text.StrPool;
import com.nbatch.job.core.biz.model.HandleCallbackParam;
import com.nbatch.job.core.thread.TriggerCallbackThread;
import com.nbatch.job.handler.thread.BatchRunnable;
import com.nbatch.job.handler.thread.BatchThreadPoolExecutor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.nbatch.job.core.enums.CallbackTypeEnum.NODE_STATUS_CALLBACK;

/**
 * 线程池 工具类
 *
 * @author Mr.ni
 */
@Log4j2
@Component
public class BatchThreadPoolUtil {

    /**
     * 线程池map
     */
    private static final Map<String, BatchThreadPoolExecutor> THREAD_POOL_EXECUTOR = new ConcurrentHashMap<>();

    /**
     * 线程偏移量
     */
    private static final AtomicInteger DEFAULT_THREAD_POOL_OFFSET = new AtomicInteger();

    /**
     * 如果被拒绝，直接丢弃
     */
    public static BatchThreadPoolExecutor newThreadPoolExecutorDiscard(String threadPoolKey, int corePoolSize,
                                                                       int maxPoolSize, int keepAliveTime,
                                                                       TimeUnit timeUnit, int queueSize) {
        BatchThreadPoolExecutor executor = new BatchThreadPoolExecutor(threadPoolKey,
                corePoolSize, maxPoolSize,
                keepAliveTime, timeUnit,
                new ArrayBlockingQueue<>(queueSize), newThreadFactory(threadPoolKey),
                new DiscardRejectedExecutionPolicy());
        THREAD_POOL_EXECUTOR.put(threadPoolKey, executor);
        return executor;
    }

    public static BatchThreadPoolExecutor getBatchThreadPoolExecutor(String threadPoolKey) {
        return THREAD_POOL_EXECUTOR.get(threadPoolKey);
    }

    /**
     * 中断所有指定线程池
     */
    public static void shutdownAllThreadPool() {
        for (Map.Entry<String, BatchThreadPoolExecutor> entry : THREAD_POOL_EXECUTOR.entrySet()) {
            ThreadPoolExecutor executor = entry.getValue();
            if (!executor.isShutdown()) {
                // 中断线程池,这里使用shutdown而不是shutdownNow,将线程池状态设为SHUTDOWN，不再接受新任务，但允许正在执行的任务完成。
                // shutdown()方法会将现在正在运行的任务以及队列中的任务执行完成，然后关闭线程池。
                executor.shutdown();
                log.info("Thread pool [{}] has been shutdown now.", entry.getKey());
            }
        }
        THREAD_POOL_EXECUTOR.clear();
    }

    /**
     * 判断线程池是否有线程在运行
     */
    public static boolean isThreadPoolIdle(String threadPoolKey) {
        ThreadPoolExecutor executor = THREAD_POOL_EXECUTOR.get(threadPoolKey);
        if (executor == null || executor.isShutdown()) {
            // 已关闭或不存在，认为是空闲
            return true;
        }
        return executor.getActiveCount() == 0;
    }

    /**
     * 新建线程工厂
     */
    public static ThreadFactory newThreadFactory(String threadPrefix) {
        return r -> {
            Thread thread = new Thread(r);
            thread.setName(threadPrefix + StrPool.UNDERLINE + DEFAULT_THREAD_POOL_OFFSET.incrementAndGet());
            thread.setDaemon(true);
            log.info("new thread:{}", threadPrefix);
            return thread;
        };
    }

    /**
     * 拒绝策略 -- activateMQ的做法
     * 重新尝试加入队列，等待超时，影响线程执行效率
     */
    static class DiscardRejectedExecutionPolicy implements RejectedExecutionHandler {

        public DiscardRejectedExecutionPolicy() {

        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.error("线程被丢弃，{}", r);
            HandleCallbackParam handleCallbackParam = new HandleCallbackParam();
            handleCallbackParam.setCallBackType(NODE_STATUS_CALLBACK.getValue());
            BatchRunnable batchRunnable = (BatchRunnable) r;
            handleCallbackParam.getNodeStatusCallbackParam()
                    .setWorkId(batchRunnable.getCacheObj().getStr("workId"))
                    .setNodeId(batchRunnable.getCacheObj().getStr("nodeId"))
                    .setHandleCode(0)
                    .setHandleMsg("线程被抛弃");
            TriggerCallbackThread.pushCallBack(handleCallbackParam);
        }
    }


}
