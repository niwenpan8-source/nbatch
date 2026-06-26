package com.nbatch.job.handler.thread;

import cn.hutool.json.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * @description: 批量处理任务
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@Log4j2
@Getter
@NoArgsConstructor
@AllArgsConstructor
public abstract class BatchRunnable implements Runnable{

    private JSONObject cacheObj;

    private volatile boolean stopRequested;

    private volatile Thread runningThread;

    public BatchRunnable(JSONObject cacheObj) {
        this.cacheObj = cacheObj;
    }

    public abstract void runBefore();

    @Override
    public void run() {
        runningThread = Thread.currentThread();
        boolean runAfterRequired = false;
        boolean stopped = false;
        try {
            if (isStopRequested()) {
                runStop();
                stopped = true;
                return;
            }
            runAfterRequired = true;
            runBefore();
            if (isStopRequested()) {
                runStop();
                stopped = true;
                return;
            }
            runBatch();
        } finally {
            try {
                if (runAfterRequired && !stopped) {
                    runAfter();
                }
            } finally {
                runningThread = null;
            }
        }
    }

    public abstract void runBatch();

    public abstract void runAfter();

    public void requestStop() {
        stopRequested = true;
        Thread thread = runningThread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    public boolean isStopRequested() {
        return stopRequested || RunNodeStopRegistry.isStopRequested(getNodeLogId());
    }

    public String getRunNodeId() {
        return cacheObj == null ? null : cacheObj.getStr("runNodeId");
    }

    public String getNodeLogId() {
        return cacheObj == null ? null : cacheObj.getStr("nodeLogId");
    }

    public void runStop() {
    }
}
