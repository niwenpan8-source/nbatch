package com.nbatch.job.handler.thread;

import cn.hutool.core.collection.CollUtil;
import com.nbatch.job.core.biz.AdminBiz;
import com.nbatch.job.core.biz.model.HandleCallbackParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.executor.BatchJobExecutor;
import com.nbatch.job.core.thread.TriggerCallbackThread;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.nbatch.job.core.enums.CallbackTypeEnum.NODE_STATUS_CALLBACK;

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

    public BatchThreadPoolExecutor(String threadPoolKey,
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
        HandleCallbackParam handleCallbackParam = new HandleCallbackParam();
        handleCallbackParam.setCallBackType(NODE_STATUS_CALLBACK.getValue());
        if (t != null) {
            log.error("execute Runnable error, hashCode:{}", r.hashCode(), t);
            if (r instanceof BatchRunnable) {
                BatchRunnable batchRunnable = (BatchRunnable) r;
                handleCallbackParam.setLogId(batchRunnable.getCacheObj().getStr("logId"));
                handleCallbackParam.getNodeStatusCallbackParam()
                        .setWorkId(batchRunnable.getCacheObj().getStr("workId"))
                        .setNodeId(batchRunnable.getCacheObj().getStr("nodeId"))
                        .setRunWorkId(batchRunnable.getCacheObj().getStr("runWorkId"))
                        .setRunNodeId(batchRunnable.getCacheObj().getStr("runNodeId"))
                        .setNodeLogId(batchRunnable.getCacheObj().getStr("nodeLogId"))
                        .setWorkType(batchRunnable.getCacheObj().getInt("workType"))
                        .setHandleCode(HandleCodeConstant.HANDLE_CODE_FAIL)
                        .setHandleMsg(t.getMessage());
            }
        } else {
            if (r instanceof BatchRunnable) {
                BatchRunnable batchRunnable = (BatchRunnable) r;
                handleCallbackParam.setLogId(batchRunnable.getCacheObj().getStr("logId"));
                handleCallbackParam.getNodeStatusCallbackParam()
                        .setWorkId(batchRunnable.getCacheObj().getStr("workId"))
                        .setNodeId(batchRunnable.getCacheObj().getStr("nodeId"))
                        .setRunWorkId(batchRunnable.getCacheObj().getStr("runWorkId"))
                        .setRunNodeId(batchRunnable.getCacheObj().getStr("runNodeId"))
                        .setNodeLogId(batchRunnable.getCacheObj().getStr("nodeLogId"))
                        .setWorkType(batchRunnable.getCacheObj().getInt("workType"))
                        .setHandleCode(HandleCodeConstant.HANDLE_CODE_SUCCESS)
                        .setHandleMsg("执行成功");
                currentRunningTaskList.remove(r);
            }
        }

        TriggerCallbackThread.pushCallBack(handleCallbackParam);
        super.afterExecute(r, t);
    }

    /**
     * 清空等待任务队列，执行完成当前任务
     */
    @Override
    public void shutdown() {
        log.info("shutdown threadPool:{}", threadPoolKey);
        List<HandleCallbackParam> currentNotRunningFinishTaskList = new ArrayList<>();
        if (CollUtil.isNotEmpty(currentRunningTaskList)) {
            for (BatchRunnable batchRunnable : currentRunningTaskList) {
                HandleCallbackParam handleCallbackParam = new HandleCallbackParam();
                handleCallbackParam.setCallBackType(NODE_STATUS_CALLBACK.getValue());
                handleCallbackParam.setLogId(batchRunnable.getCacheObj().getStr("logId"));
                generateHandleCallbackParam(currentNotRunningFinishTaskList, batchRunnable, handleCallbackParam);
            }
        }
        BlockingQueue<Runnable> queue = this.getQueue();
        if (CollUtil.isNotEmpty(queue)) {
            for (Runnable runnable : queue) {
                if (runnable instanceof BatchRunnable) {
                    BatchRunnable batchRunnable = (BatchRunnable) runnable;
                    HandleCallbackParam handleCallbackParam = new HandleCallbackParam();
                    handleCallbackParam.setCallBackType(NODE_STATUS_CALLBACK.getValue());
                    handleCallbackParam.setLogId(batchRunnable.getCacheObj().getStr("logId"));
                    handleCallbackParam.setLogId(batchRunnable.getCacheObj().getStr("logId"));
                    handleCallbackParam.setCallBackType(NODE_STATUS_CALLBACK.getValue());
                    generateHandleCallbackParam(currentNotRunningFinishTaskList, batchRunnable, handleCallbackParam);
                }
            }
        }
        log.info("currentNotRunningFinishTaskList:{}", currentNotRunningFinishTaskList);
        for (AdminBiz adminBiz : BatchJobExecutor.getAdminBizList()) {
            try {
                ReturnT<String> callbackResult = adminBiz.callback(currentNotRunningFinishTaskList);
                if (callbackResult != null && HandleCodeConstant.HANDLE_CODE_SUCCESS == callbackResult.getCode()) {
                    log.error("<br>----------- graceful shutdown finish.");
                } else {
                    log.error("<br>----------- graceful shutdown fail, callbackResult:{}", callbackResult);
                }
            } catch (Throwable e) {
                log.error("<br>----------- graceful shutdown error, errorMsg:{}", e.getMessage());
            }
        }
        this.shutdownNow();
    }

    private void generateHandleCallbackParam(List<HandleCallbackParam> currentNotRunningFinishTaskList, BatchRunnable batchRunnable, HandleCallbackParam handleCallbackParam) {
        handleCallbackParam.getNodeStatusCallbackParam()
                .setWorkId(batchRunnable.getCacheObj().getStr("workId"))
                .setNodeId(batchRunnable.getCacheObj().getStr("nodeId"))
                .setRunWorkId(batchRunnable.getCacheObj().getStr("runWorkId"))
                .setRunNodeId(batchRunnable.getCacheObj().getStr("runNodeId"))
                .setNodeLogId(batchRunnable.getCacheObj().getStr("nodeLogId"))
                .setWorkType(batchRunnable.getCacheObj().getInt("workType"))
                .setHandleCode(HandleCodeConstant.HANDLE_CODE_FAIL)
                .setHandleMsg("系统优雅关闭，任务停止");
        currentNotRunningFinishTaskList.add(handleCallbackParam);
    }
}
