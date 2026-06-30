package com.nbatch.job.handler.thread;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import com.nbatch.job.core.biz.model.RunNodeLogEventParam;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.thread.RunNodeLogEventLog;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.FAIL;
import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.STARTED;
import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.STOPPED;
import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.SUCCESS;

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
            BatchRunnable batchRunnable = (BatchRunnable) r;
            currentRunningTaskList.add(batchRunnable);
            if (!batchRunnable.isStopRequested()) {
                appendRunNodeEvent(batchRunnable, STARTED.getValue(), 0, "运行节点已接收");
            }
        }
    }


    public boolean executeBatch(BatchRunnable runnable) {
        try {
            this.execute(runnable);
        } catch (Exception e) {
            log.error("execute error.", e);
            return false;
        }
        log.debug("execute runnable, hashCode:{}, threadPoolKey:{}, poolSize:{}, largestPoolSize:{}, activeCount:{}, " +
                        "taskCount:{}, completedTaskCount:{}, queueSize:{}",
                runnable.hashCode(), threadPoolKey, this.getPoolSize(), this.getLargestPoolSize(),
                this.getActiveCount(), this.getTaskCount(), this.getCompletedTaskCount(), this.getQueue().size());
        return true;

    }

    @Override
    public void execute(Runnable command) {
        super.execute(command);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        if (t != null) {
            // 如果说发生异常需要针对异常进行处理
            log.error("execute Runnable error, hashCode:{}", r.hashCode(), t);
            if (r instanceof BatchRunnable) {
                BatchRunnable batchRunnable = (BatchRunnable) r;
                if (batchRunnable.isStopRequested()) {
                    appendRunNodeEvent(batchRunnable, STOPPED.getValue(), HandleCodeConstant.HANDLE_CODE_FAIL, "运行节点已停止");
                } else {
                    appendRunNodeEvent(batchRunnable, FAIL.getValue(), HandleCodeConstant.HANDLE_CODE_FAIL, ExceptionUtil.getRootCauseMessage(t));
                }
                RunNodeStopRegistry.clear(batchRunnable.getNodeLogId());
            }
        } else {
            if (r instanceof BatchRunnable) {
                BatchRunnable batchRunnable = (BatchRunnable) r;
                if (batchRunnable.isStopRequested()) {
                    appendRunNodeEvent(batchRunnable, STOPPED.getValue(), HandleCodeConstant.HANDLE_CODE_FAIL, "运行节点已停止");
                } else {
                    appendRunNodeEvent(batchRunnable, SUCCESS.getValue(), HandleCodeConstant.HANDLE_CODE_SUCCESS, "执行成功");
                }
                RunNodeStopRegistry.clear(batchRunnable.getNodeLogId());
            }
        }

        currentRunningTaskList.remove(r);
        super.afterExecute(r, t);
    }

    public int stopRunNodes(List<String> nodeLogIdList) {
        if (CollUtil.isEmpty(nodeLogIdList)) {
            return 0;
        }
        RunNodeStopRegistry.requestStop(nodeLogIdList);
        int stopCount = 0;
        for (Runnable runnable : this.getQueue()) {
            if (runnable instanceof BatchRunnable) {
                BatchRunnable batchRunnable = (BatchRunnable) runnable;
                if (nodeLogIdList.contains(batchRunnable.getNodeLogId()) && this.getQueue().remove(runnable)) {
                    stopWaitingTask(runnable);
                    stopCount++;
                }
            }
        }
        for (BatchRunnable batchRunnable : currentRunningTaskList) {
            if (nodeLogIdList.contains(batchRunnable.getNodeLogId())) {
                batchRunnable.requestStop();
                stopCount++;
            }
        }
        return stopCount;
    }

    /**
     * 清空等待任务队列，执行完成当前任务
     */
    @Override
    public void shutdown() {
        log.info("shutdown threadPool:{}", threadPoolKey);
        List<Runnable> waitingTasks = this.shutdownNow();
        for (Runnable waitingTask : waitingTasks) {
            stopWaitingTask(waitingTask);
        }
    }

    private void stopWaitingTask(Runnable runnable) {
        if (!(runnable instanceof BatchRunnable)) {
            return;
        }
        BatchRunnable batchRunnable = (BatchRunnable) runnable;
        try {
            batchRunnable.requestStop();
            appendRunNodeEvent(batchRunnable, STOPPED.getValue(), HandleCodeConstant.HANDLE_CODE_FAIL, "运行节点已停止");
            batchRunnable.runStop();
        } catch (Exception e) {
            log.error("stop waiting task error, hashCode:{}", runnable.hashCode(), e);
        } finally {
            RunNodeStopRegistry.clear(batchRunnable.getNodeLogId());
        }
    }

    public static void appendRunNodeEvent(BatchRunnable batchRunnable, String eventType, Integer handleCode, String handleMsg) {
        if (batchRunnable == null || batchRunnable.getCacheObj() == null) {
            return;
        }
        RunNodeLogEventParam eventParam = new RunNodeLogEventParam()
                .setEventType(eventType)
                .setWorkId(batchRunnable.getCacheObj().getStr("workId"))
                .setRunWorkId(batchRunnable.getCacheObj().getStr("runWorkId"))
                .setNodeId(batchRunnable.getCacheObj().getStr("nodeId"))
                .setRunNodeId(batchRunnable.getCacheObj().getStr("runNodeId"))
                .setNodeLogId(batchRunnable.getCacheObj().getStr("nodeLogId"))
                .setTurnDate(batchRunnable.getCacheObj().getDate("turnDate"))
                .setWorkType(batchRunnable.getCacheObj().getInt("workType"))
                .setHandleCode(handleCode)
                .setHandleMsg(handleMsg);
        RunNodeLogEventLog.getInstance().append(eventParam);
    }
}
