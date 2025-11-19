package com.nbatch.job.handler.thread;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @description: 线程池
 * @author: Mr.ni
 * @date: 2025-07-07
 */
@Log4j2
public class BatchThreadPoolExecutor extends ThreadPoolExecutor {

    private final String threadPoolKey;

    /**
     * 当前正在执行的任务
     */
    private final List<BatchRunnable> currentRunningTaskList = new CopyOnWriteArrayList<>();

    BatchThreadPoolExecutor(String threadPoolKey,
                            int corePoolSize,
                            int maximumPoolSize,
                            long keepAliveTime,
                            TimeUnit unit,
                            BlockingQueue<Runnable> workQueue,
                            ThreadFactory threadFactory,
                            RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        this.threadPoolKey = threadPoolKey;
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        if (r instanceof BatchRunnable) {
            currentRunningTaskList.add((BatchRunnable) r);
        }
    }


    public void executeBatch(BatchRunnable runnable) {
        this.execute(runnable);
    }

    @Override
    public void execute(Runnable command) {
        try {
            super.execute(command);
        } catch (Exception e) {
            log.error("execute error.", e);
        }
        log.debug("execute runnable, hashCode:{}, threadPoolKey:{}, poolSize:{}, largestPoolSize:{}, activeCount:{}, " +
                        "taskCount:{}, completedTaskCount:{}, queueSize:{}",
                command.hashCode(), threadPoolKey, this.getPoolSize(), this.getLargestPoolSize(),
                this.getActiveCount(), this.getTaskCount(), this.getCompletedTaskCount(), this.getQueue().size());
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        if (t != null) {
            log.error("execute Runnable error, hashCode:{}", r.hashCode(), t);
        }
        if (r instanceof BatchRunnable) {
            currentRunningTaskList.remove(r);
        }
        super.afterExecute(r, t);
    }

    /**
     * 清空等待任务队列，执行完成当前任务
     */
    public void shutdown() {
        log.info("shutdown threadPool:{}", threadPoolKey);
        super.shutdownNow();
        if (CollUtil.isNotEmpty(currentRunningTaskList)) {
            for (BatchRunnable batchRunnable : currentRunningTaskList) {
                log.info("shutdown threadPool:{}, task:{}", threadPoolKey, batchRunnable);
            }
            currentRunningTaskList.clear();
        }
    }
}
