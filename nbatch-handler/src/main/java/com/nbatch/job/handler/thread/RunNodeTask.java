package com.nbatch.job.handler.thread;

import cn.hutool.json.JSONObject;
import lombok.Getter;

import java.util.Date;
import java.util.Objects;

/**
 * 运行节点的具体任务，负责执行生命周期和停止信号传递。
 */
public final class RunNodeTask extends AbstractThreadPoolExecutor.ManagedTask {

    @Getter
    private final JSONObject cacheObj;
    private final Runnable executeAction;
    private final Runnable stopAction;

    public RunNodeTask(JSONObject cacheObj, Runnable executeAction) {
        this(cacheObj, executeAction, () -> { });
    }

    public RunNodeTask(JSONObject cacheObj, Runnable executeAction, Runnable stopAction) {
        this.cacheObj = cacheObj;
        this.executeAction = Objects.requireNonNull(executeAction, "executeAction must not be null");
        this.stopAction = Objects.requireNonNull(stopAction, "stopAction must not be null");
    }

    @Override
    protected void executeTask() {
        if (isStopRequested()) {
            return;
        }
        executeAction.run();
    }

    @Override
    public String getTaskId() {
        return getNodeLogId();
    }

    @Override
    protected void onStopRequested() {
        stopAction.run();
    }

    public String getNodeId() {
        return cacheObj == null ? null : cacheObj.getStr("nodeId");
    }

    public String getNodeLogId() {
        return cacheObj == null ? null : cacheObj.getStr("nodeLogId");
    }

    public Date getTurnDate() {
        return cacheObj == null ? null : cacheObj.getDate("turnDate");
    }

}
