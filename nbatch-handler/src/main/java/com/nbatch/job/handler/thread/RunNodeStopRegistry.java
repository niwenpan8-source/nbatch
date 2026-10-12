package com.nbatch.job.handler.thread;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 记录已经确认停止的执行尝试，用于拦截晚于停止请求到达的 /run 请求。
 */
public final class RunNodeStopRegistry {

    /**
     * 限制停止屏障数量，避免管理端异常请求耗尽内存。
     */
    private static final int MAX_ENTRY_COUNT = 200_000;

    /**
     * 覆盖延迟请求和短期重试，同时允许旧屏障最终回收。
     */
    private static final long RETENTION_MILLIS = TimeUnit.HOURS.toMillis(24);

    /**
     * 限制全量过期扫描频率。
     */
    private static final long CLEANUP_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1);

    /**
     * nodeLogId -> 过期时间；任务退出后仍需保留，用于拒绝迟到的 /run。
     */
    private static final Map<String, Long> STOPPED_ATTEMPT_MAP = new LinkedHashMap<>();

    /**
     * 下一次允许执行全量过期清理的时间。
     */
    private static long nextCleanupTime;

    private RunNodeStopRegistry() {
    }

    /**
     * 在停止实际任务前建立屏障，处理停止请求先于 /run 到达的乱序场景。
     */
    public static synchronized void requestStop(List<String> nodeLogIdList) {
        if (CollUtil.isEmpty(nodeLogIdList)) {
            return;
        }
        long now = System.currentTimeMillis();
        // 通过当前时间清理过期的注册
        cleanupExpired(now);
        // 设置过期时间，24小时
        long expireTime = now + RETENTION_MILLIS;
        for (String nodeLogId : nodeLogIdList) {
            if (StrUtil.isNotEmpty(nodeLogId)) {
                // 将节点日志id存储于内存当中
                STOPPED_ATTEMPT_MAP.put(nodeLogId, expireTime);
            }
        }
        // 如果当前map大于最大内存数量，则删除最早的元素
        while (STOPPED_ATTEMPT_MAP.size() > MAX_ENTRY_COUNT) {
            Iterator<String> iterator = STOPPED_ATTEMPT_MAP.keySet().iterator();
            if (!iterator.hasNext()) {
                break;
            }
            iterator.next();
            iterator.remove();
        }
    }

    /**
     * 判断本次执行尝试是否已被停止；如果nodeLogId为空或者已经过期，则视为未知，主要是默认认为任务不可能运行24小时
     */
    public static synchronized boolean isStopRequested(String nodeLogId) {
        if (StrUtil.isEmpty(nodeLogId)) {
            return false;
        }
        Long expireTime = STOPPED_ATTEMPT_MAP.get(nodeLogId);
        if (expireTime == null) {
            return false;
        }
        if (expireTime <= System.currentTimeMillis()) {
            STOPPED_ATTEMPT_MAP.remove(nodeLogId);
            return false;
        }
        return true;
    }

    /**
     * 按固定周期批量清理过期屏障，避免每次请求都遍历整个 Map。
     */
    private static void cleanupExpired(long now) {
        if (now < nextCleanupTime) {
            return;
        }
        STOPPED_ATTEMPT_MAP.entrySet().removeIf(entry -> entry.getValue() <= now);
        nextCleanupTime = now + CLEANUP_INTERVAL_MILLIS;
    }

    /**
     * 仅供测试清理指定停止屏障，生产流程不会提前删除。
     */
    static synchronized void clearForTest(String nodeLogId) {
        STOPPED_ATTEMPT_MAP.remove(nodeLogId);
    }
}
