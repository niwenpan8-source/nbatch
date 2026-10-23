package com.nbatch.job.core.files;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.RunNodeLogDetailParam;
import com.nbatch.job.core.biz.model.RunNodeLogPullResult;
import com.nbatch.job.core.util.GsonTool;
import lombok.extern.slf4j.Slf4j;

/**
 * 运行节点详细日志本地队列。
 * 与节点状态事件使用同一套分段、租约和 ACK 实现，但保留独立通道和确认位点。
 */
@Slf4j
public class RunNodeLogDetailLog {

    private static final String DETAIL_LOG_DIR_NAME = "run-node-detail";

    private static final RunNodeLogDetailLog INSTANCE = new RunNodeLogDetailLog();

    private final LocalPullLogStore localLog = new LocalPullLogStore(DETAIL_LOG_DIR_NAME);

    public static RunNodeLogDetailLog getInstance() {
        return INSTANCE;
    }

    /**
     * 启动详细日志本地队列。
     */
    public void start() {
        localLog.start();
    }

    /**
     * 停止详细日志本地队列，并刷出已接收的剩余数据。
     */
    public void toStop() {
        localLog.toStop();
    }

    /**
     * 将详细日志持久化到执行器本地队列，等待管理端主动拉取。
     */
    public long append(RunNodeLogDetailParam detailLog) {
        if (detailLog == null || StrUtil.isBlank(detailLog.getWorkId())
                || StrUtil.isBlank(detailLog.getNodeId()) || StrUtil.isBlank(detailLog.getNodeLogId())) {
            return -1L;
        }
        if (StrUtil.isBlank(detailLog.getDetailLogId())) {
            detailLog.setDetailLogId(IdUtil.getSnowflakeNextIdStr());
        }
        return localLog.append(GsonTool.toJson(detailLog));
    }

    /**
     * 获取详细日志拉取租约。
     */
    public RunNodeLogPullResult tryPull(String consumerId, Integer maxSize) {
        return localLog.tryPull(consumerId, maxSize);
    }

    /**
     * 确认已成功入库的详细日志位置。
     */
    public boolean ack(String consumerId, String leaseToken, Long offset) {
        return localLog.ack(consumerId, leaseToken, offset);
    }

}
