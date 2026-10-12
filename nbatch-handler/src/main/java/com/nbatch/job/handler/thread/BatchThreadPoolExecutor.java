package com.nbatch.job.handler.thread;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.RunNodeLogEventParam;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.files.RunNodeLogEventLog;
import lombok.extern.log4j.Log4j2;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.FAIL;
import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.HEARTBEAT;
import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.STARTED;
import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.STOPPED;
import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.SUCCESS;

/**
 * 线程池执行框架的 Batch 实现，负责节点准入、运行事件和心跳上报。
 */
@Log4j2
public class BatchThreadPoolExecutor extends AbstractThreadPoolExecutor<RunNodeTask> {

    private static final long HEARTBEAT_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1);
    private volatile boolean heartbeatThreadToStop;
    private Thread heartbeatThread;

    public BatchThreadPoolExecutor(String threadPoolKey,
                                   int corePoolSize,
                                   int maximumPoolSize,
                                   long keepAliveTime,
                                   TimeUnit unit,
                                   BlockingQueue<Runnable> workQueue,
                                   ThreadFactory threadFactory,
                                   RejectedExecutionHandler handler) {
        super(threadPoolKey, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        startHeartbeatThread();
    }

    public boolean executeBatch(RunNodeTask task) {
        return executeTask(task);
    }

    public int stopRunNodes(List<String> nodeLogIdList, boolean stopRunning) {
        return stopTasks(nodeLogIdList, stopRunning);
    }

    /**
     * 执行器关闭等待超时后，为仍未退出的节点补写终态事件。
     * 此时执行器进程即将结束，节点不可能继续在本次客户端生命周期内运行。
     */
    public int appendShutdownStoppedEvents() {
        int count = 0;
        for (RunNodeTask task : getUnexitedTasks()) {
            System.out.println("task ============== " + task);
            if (appendRunNodeEvent(task, STOPPED.getValue(), HandleCodeConstant.HANDLE_CODE_FAIL,
                    "执行器正在关闭，运行节点被中断") >= 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * 收集运行节点状态
     * @param nodeLogIdList 运行节点日志id列表
     * @param stateMap 状态map
     */
    public void collectRunNodeStates(List<String> nodeLogIdList, Map<String, String> stateMap) {
        collectTaskStates(nodeLogIdList, stateMap);
    }

    @Override
    protected boolean canSubmit(RunNodeTask task, List<RunNodeTask> activeTasks) {
        if (RunNodeStopRegistry.isStopRequested(task.getNodeLogId())) {
            log.warn("忽略已停止运行节点的执行请求，nodeId：{}，nodeLogId：{}",
                    task.getNodeId(), task.getNodeLogId());
            appendRunNodeEvent(task, STOPPED.getValue(), HandleCodeConstant.HANDLE_CODE_FAIL,
                    "运行节点已收到停止请求，忽略迟到的执行请求");
            return false;
        }
        for (RunNodeTask active : activeTasks) {
            if (StrUtil.equals(task.getNodeId(), active.getNodeId())
                    && StrUtil.equals(formatTurnDate(task.getTurnDate()), formatTurnDate(active.getTurnDate()))) {
            log.warn("拒绝重复运行节点，nodeId：{}，nodeLogId：{}，当前活动 nodeLogId：{}",
                        task.getNodeId(), task.getNodeLogId(), active.getNodeLogId());
                appendRunNodeEvent(task, FAIL.getValue(), HandleCodeConstant.HANDLE_CODE_FAIL,
                        "同一运行节点仍在执行，拒绝本次重复请求");
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onTaskStateChanged(RunNodeTask task, TaskEvent event, Throwable failure) {
        if (event == TaskEvent.STARTED) {
            appendRunNodeEvent(task, STARTED.getValue(), 0, "运行节点开始执行");
        } else if (event == TaskEvent.SUCCEEDED) {
            appendRunNodeEvent(task, SUCCESS.getValue(), HandleCodeConstant.HANDLE_CODE_SUCCESS, "执行成功");
        } else if (event == TaskEvent.STOPPED) {
            appendRunNodeEvent(task, STOPPED.getValue(), HandleCodeConstant.HANDLE_CODE_FAIL, "运行节点已停止");
        } else if (failure instanceof InterruptedException) {
            appendRunNodeEvent(task, FAIL.getValue(), HandleCodeConstant.HANDLE_CODE_FAIL, "读取运行节点执行结果被中断");
        } else {
            appendRunNodeEvent(task, FAIL.getValue(), HandleCodeConstant.HANDLE_CODE_FAIL,
                    ExceptionUtil.getRootCauseMessage(failure));
        }
    }

    @Override
    public void shutdown() {
        stopHeartbeatThread();
        super.shutdown();
    }

    private String formatTurnDate(Date turnDate) {
        return turnDate == null ? "" : DateUtil.formatDate(turnDate);
    }

    /**
     * 运行节点心跳上报线程,这个是为了刷新长节点任务失联设置的，这个类似于监听线程
     */
    private void startHeartbeatThread() {
        heartbeatThreadToStop = false;
        heartbeatThread = new Thread(() -> {
            while (!heartbeatThreadToStop) {
                try {
                    TimeUnit.MILLISECONDS.sleep(HEARTBEAT_INTERVAL_MILLIS);
                    for (RunNodeTask task : getRunningTasks()) {
                        appendRunNodeEvent(task, HEARTBEAT.getValue(), 0, "运行节点心跳");
                    }
                } catch (InterruptedException e) {
                    if (!heartbeatThreadToStop) {
                        log.warn("运行节点心跳线程被中断，线程池键：{}", getThreadPoolKey(), e);
                    }
                } catch (Throwable e) {
                    log.error("运行节点心跳线程发生异常，线程池键：{}", getThreadPoolKey(), e);
                }
            }
        });
        heartbeatThread.setDaemon(true);
        heartbeatThread.setName("job, run node heartbeat-" + getThreadPoolKey());
        heartbeatThread.start();
    }

    private void stopHeartbeatThread() {
        heartbeatThreadToStop = true;
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
        }
    }

    public static long appendRunNodeEvent(RunNodeTask task, String eventType, Integer handleCode, String handleMsg) {
        if (task == null || task.getCacheObj() == null) {
            return -1L;
        }
        RunNodeLogEventParam eventParam = new RunNodeLogEventParam()

                .setEventType(eventType)
                .setWorkId(task.getCacheObj().getStr("workId"))
                .setNodeId(task.getCacheObj().getStr("nodeId"))
                .setNodeLogId(task.getCacheObj().getStr("nodeLogId"))
                .setTurnDate(task.getCacheObj().getDate("turnDate"))
                .setWorkType(task.getCacheObj().getInt("workType"))
                .setHandleCode(handleCode)
                .setHandleMsg(handleMsg);
        return RunNodeLogEventLog.getInstance().append(eventParam);
    }
}
