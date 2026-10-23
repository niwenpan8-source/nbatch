package com.nbatch.job.handler.thread;

import cn.hutool.core.text.StrPool;
import lombok.extern.log4j.Log4j2;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 使用统一的线程命名和拒绝策略创建 Batch 线程池。
 */
@Log4j2
public final class BatchThreadPoolFactory {

    private static final AtomicInteger THREAD_OFFSET = new AtomicInteger();

    private BatchThreadPoolFactory() {
    }

    public static BatchThreadPoolExecutor createDiscardExecutor(String threadPoolKey,
                                                                  int corePoolSize,
                                                                  int maxPoolSize,
                                                                  int keepAliveTime,
                                                                  TimeUnit timeUnit,
                                                                  int queueSize) {
        return new BatchThreadPoolExecutor(threadPoolKey, corePoolSize, maxPoolSize, keepAliveTime, timeUnit,
                new ArrayBlockingQueue<>(queueSize), newThreadFactory(threadPoolKey), new RejectingPolicy());
    }

    private static ThreadFactory newThreadFactory(String threadPrefix) {
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(threadPrefix + StrPool.UNDERLINE + THREAD_OFFSET.incrementAndGet());
            thread.setDaemon(true);
            log.info("创建新线程：{}", threadPrefix);
            return thread;
        };
    }

    /**
     * 如果被拒绝，直接丢弃
     */
    private static final class RejectingPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor executor) {
            log.error("线程池拒绝任务，任务：{}", runnable);
            throw new RejectedExecutionException("thread pool queue is full");
        }
    }
}
