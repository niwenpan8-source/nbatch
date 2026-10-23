package com.nbatch.job.core.files;

import com.nbatch.job.core.biz.model.RunNodeLogEventParam;
import com.nbatch.job.core.biz.model.RunNodeLogPullResult;
import com.nbatch.job.core.util.GsonTool;

/**
 * 运行节点状态事件日志。
 * 只处理状态事件的序列化，可靠存储由 LocalPullLogStore 统一负责。
 */
public class RunNodeLogEventLog {

    private static final RunNodeLogEventLog INSTANCE = new RunNodeLogEventLog();

    private final LocalPullLogStore localStore = new LocalPullLogStore("run-node-event");

    public static RunNodeLogEventLog getInstance() {
        return INSTANCE;
    }

    /**
     * 启动状态事件本地队列。
     */
    public void start() {
        localStore.start();
    }

    /**
     * 停止状态事件本地队列。
     */
    public void toStop() {
        localStore.toStop();
    }

    /**
     * 追加状态事件。
     */
    public long append(RunNodeLogEventParam event) {
        return event == null ? -1L : localStore.append(GsonTool.toJson(event));
    }

    /**
     * 获取状态事件拉取租约。
     */
    public RunNodeLogPullResult tryPull(String consumerId, Integer maxSize) {
        return localStore.tryPull(consumerId, maxSize);
    }

    /**
     * 读取已落盘状态事件，不创建拉取租约。
     * 仅用于本地测试和诊断。
     */
    public RunNodeLogPullResult pull(Integer maxSize) {
        return localStore.pull(maxSize);
    }

    /**
     * 确认已处理的状态事件。
     */
    public boolean ack(String consumerId, String leaseToken, Long offset) {
        return localStore.ack(consumerId, leaseToken, offset);
    }

    /**
     * 推进本地确认位置。
     * 仅用于本地测试和诊断，远程拉取必须使用租约确认。
     */
    public void ack(Long offset) {
        localStore.ack(offset);
    }
}
