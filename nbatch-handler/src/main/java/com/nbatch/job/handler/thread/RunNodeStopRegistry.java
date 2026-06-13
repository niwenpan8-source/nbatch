package com.nbatch.job.handler.thread;

import cn.hutool.core.collection.CollUtil;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RunNodeStopRegistry {

    private static final Set<String> STOP_NODE_LOG_ID_SET = ConcurrentHashMap.newKeySet();

    private RunNodeStopRegistry() {
    }

    public static void requestStop(Collection<String> nodeLogIdList) {
        if (CollUtil.isNotEmpty(nodeLogIdList)) {
            STOP_NODE_LOG_ID_SET.addAll(nodeLogIdList);
        }
    }

    public static boolean isStopRequested(String nodeLogId) {
        return nodeLogId != null && STOP_NODE_LOG_ID_SET.contains(nodeLogId);
    }

    public static void clear(String nodeLogId) {
        if (nodeLogId != null) {
            STOP_NODE_LOG_ID_SET.remove(nodeLogId);
        }
    }
}
