package com.nbatch.job.handler.utils;


import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.enums.RunNodeExecutionStateEnum;
import com.nbatch.job.handler.thread.BatchThreadPoolExecutor;
import com.nbatch.job.handler.thread.BatchThreadPoolFactory;
import com.nbatch.job.handler.thread.RunNodeStopRegistry;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 线程池 工具类
 */
@Log4j2
@Component
public class BatchThreadPoolUtil {

    /**
     * 线程池map
     */
    private static final Map<String, BatchThreadPoolExecutor> THREAD_POOL_EXECUTOR = new ConcurrentHashMap<>();

    /** 执行器正在关闭时拒绝接收新的运行节点。 */
    @Getter
    private static volatile boolean shuttingDown;

    /**
     * 系统关闭时等待任务响应中断的总时长，避免关闭过程无限阻塞。
     */
    private static final long SHUTDOWN_WAIT_MILLIS = TimeUnit.SECONDS.toMillis(5);

    /**
     * 创建并按线程池标识缓存执行器。
     */
    public static BatchThreadPoolExecutor newThreadPoolExecutorDiscard(String threadPoolKey, int corePoolSize,
                                                                       int maxPoolSize, int keepAliveTime,
                                                                       TimeUnit timeUnit, int queueSize) {
        return THREAD_POOL_EXECUTOR.computeIfAbsent(threadPoolKey, key ->
                BatchThreadPoolFactory.createDiscardExecutor(key, corePoolSize, maxPoolSize,
                        keepAliveTime, timeUnit, queueSize));
    }

    public static BatchThreadPoolExecutor getBatchThreadPoolExecutor(String threadPoolKey) {
        return THREAD_POOL_EXECUTOR.get(threadPoolKey);
    }

    /**
     * 优先按线程池定位任务；未提供 threadPoolKey 时兼容扫描全部线程池。
     */
    public static int stopRunNodes(String threadPoolKey, List<String> nodeLogIdList, boolean stopRunning) {
        // 先登记停止，确保尚未到达的旧 /run 请求随后也会被拒绝。
        RunNodeStopRegistry.requestStop(nodeLogIdList);
        if (StrUtil.isNotBlank(threadPoolKey)) {
            BatchThreadPoolExecutor executor = THREAD_POOL_EXECUTOR.get(threadPoolKey);
            if (executor != null) {
                return executor.stopRunNodes(nodeLogIdList, stopRunning);
            }
            log.warn("停止运行节点失败，未找到目标线程池，线程池键：{}", threadPoolKey);
            return 0;
        }
        int stopCount = 0;
        for (BatchThreadPoolExecutor executor : THREAD_POOL_EXECUTOR.values()) {
            stopCount += executor.stopRunNodes(nodeLogIdList, stopRunning);
        }
        return stopCount;
    }

    /**
     * 先按停止屏障填充 STOPPED/ABSENT，再由线程池中的真实 Future 状态覆盖。
     */
    public static Map<String, String> queryRunNodeStates(List<String> nodeLogIdList) {
        Map<String, String> stateMap = new LinkedHashMap<>();
        if (nodeLogIdList == null) {
            return stateMap;
        }
        for (String nodeLogId : nodeLogIdList) {
            String state = RunNodeStopRegistry.isStopRequested(nodeLogId)
                    ? RunNodeExecutionStateEnum.STOPPED.getValue()
                    : RunNodeExecutionStateEnum.ABSENT.getValue();
            stateMap.put(nodeLogId, state);
        }
        for (BatchThreadPoolExecutor executor : THREAD_POOL_EXECUTOR.values()) {
            executor.collectRunNodeStates(nodeLogIdList, stateMap);
        }
        return stateMap;
    }

    /**
     * 中断所有指定线程池
     */
    public static void shutdownAllThreadPool() {
        shuttingDown = true;
        List<BatchThreadPoolExecutor> executorList = new ArrayList<>(THREAD_POOL_EXECUTOR.values());
        for (BatchThreadPoolExecutor executor : executorList) {
            if (!executor.isShutdown()) {
                // BatchThreadPoolExecutor.shutdown 会停止排队和运行中的节点并写入闭环事件。
                executor.shutdown();
            }
        }
        long deadline = System.currentTimeMillis() + SHUTDOWN_WAIT_MILLIS;
        for (BatchThreadPoolExecutor executor : executorList) {
            long remainingMillis = deadline - System.currentTimeMillis();
            if (remainingMillis <= 0) {
                break;
            }
            try {
                executor.awaitTermination(remainingMillis, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        int interruptedNodeCount = 0;
        // 这里做了保底，关闭事件时，需要向日志中插入节点关闭
        for (BatchThreadPoolExecutor executor : executorList) {
            if (!executor.isTerminated()) {
                interruptedNodeCount += executor.appendShutdownStoppedEvents();
            }
        }
        THREAD_POOL_EXECUTOR.clear();
        if (interruptedNodeCount > 0) {
            log.warn("执行器关闭等待超时，已写入 {} 个运行节点的中断停止事件。", interruptedNodeCount);
        } else {
            log.info("所有运行节点线程池已停止。");
        }
    }


}
