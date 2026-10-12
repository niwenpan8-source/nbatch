package com.nbatch.job.admin.core.thread;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.admin.core.executor.ExecutorBizProxy;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.RunNodeLogAckParam;
import com.nbatch.job.core.biz.model.RunNodeLogEventParam;
import com.nbatch.job.core.biz.model.RunNodeLogPullParam;
import com.nbatch.job.core.biz.model.RunNodeLogPullResult;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.RegistryConfig;
import com.nbatch.job.core.util.GsonTool;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 从执行器拉取运行节点本地事件日志。
 * 只负责拉取、顺序提交和确认偏移量，业务状态统一由 RunNodeHelper 处理。
 */
@Slf4j
public class JobRunNodeLogPullHelper {

    private static final JobRunNodeLogPullHelper INSTANCE = new JobRunNodeLogPullHelper();

    /** 拉取日志条数 */
    private static final int PULL_SIZE = 200;

    /** 拉取消费标识，用于 ACK 确认。 */
    private static final String PULL_CONSUMER_ID = UUID.randomUUID().toString();

    /** 单轮限制每个执行器的拉取批次和耗时，避免慢执行器长期独占单线程。 */
    private static final int MAX_PULL_BATCHES_PER_ADDRESS = 5;
    private static final long MAX_PULL_MILLIS_PER_ADDRESS = 1000L;

    /** ACK 短暂失败时最多尝试三次，相邻请求固定间隔一秒。 */
    private static final int ACK_RETRY_TIMES = 3;

    /** 相邻请求间隔 1 秒。 */
    private static final long ACK_RETRY_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(1);

    private Thread pullThread;
    private volatile boolean toStop = false;

    public static JobRunNodeLogPullHelper getInstance() {
        return INSTANCE;
    }

    public void start() {
        pullThread = new Thread(() -> {
            while (!toStop) {
                try {
                    pullExecutorRunNodeLog();
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error("拉取运行节点事件日志失败。", e);
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
            log.info(">>>>>>>>>>> 运行节点事件日志拉取线程已销毁。");
        });
        pullThread.setDaemon(true);
        pullThread.setName("job, admin run node event log pull");
        pullThread.start();
    }

    public void toStop() {
        toStop = true;
        if (pullThread != null) {
            pullThread.interrupt();
            try {
                pullThread.join();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 拉取执行器运行节点事件日志
     */
    private void pullExecutorRunNodeLog() {
        List<String> addressList = JobAdminConfig.getAdminConfig().getRegistryAddressHelper()
                .getActiveExecutorAddressList();
        for (String address : addressList) {
            pullAddressRunNodeLog(address);
        }
    }

    /**
     * 拉取执行器运行节点事件日志
     *
     * @param address 执行器地址
     */
    private void pullAddressRunNodeLog(String address) {
        long deadline = System.currentTimeMillis() + MAX_PULL_MILLIS_PER_ADDRESS;
        for (int batch = 0; batch < MAX_PULL_BATCHES_PER_ADDRESS; batch++) {
            RunNodeLogPullParam pullParam = new RunNodeLogPullParam();
            pullParam.setMaxSize(PULL_SIZE);
            pullParam.setConsumerId(PULL_CONSUMER_ID);
            ReturnT<RunNodeLogPullResult> pullResult = ExecutorBizProxy.pullRunNodeLog(address, pullParam);
            if (pullResult.getCode() != HandleCodeConstant.HANDLE_CODE_SUCCESS || pullResult.getContent() == null) {
                return;
            }
            List<RunNodeLogPullResult.LogRecord> logList = pullResult.getContent().getLogList();
            if (CollUtil.isEmpty(logList)) {
                return;
            }
            String leaseToken = pullResult.getContent().getLeaseToken();
            if (StrUtil.isBlank(leaseToken)) {
                log.warn("运行节点事件日志租约为空，地址：{}", address);
                return;
            }
            Long ackOffset = handleEventBatch(address, logList);
            if (ackOffset == null || !ackRunNodeLog(address, leaseToken, ackOffset)) {
                return;
            }
            if (logList.size() < PULL_SIZE || System.currentTimeMillis() >= deadline) {
                return;
            }
        }
    }

    /**
     * 顺序处理一批事件，只返回已经成功处理到的最后 offset。
     */
    private Long handleEventBatch(String address, List<RunNodeLogPullResult.LogRecord> logList) {
        Long ackOffset = null;
        for (RunNodeLogPullResult.LogRecord logRecord : logList) {
            Long eventOffset = logRecord == null ? null : logRecord.getOffset();
            try {
                RunNodeLogEventParam eventParam = logRecord == null ? null
                        : GsonTool.fromJson(logRecord.getPayload(), RunNodeLogEventParam.class);
                if (eventParam == null) {
                    throw new IllegalArgumentException("运行节点事件日志内容为空");
                }
                eventParam.setOffset(eventOffset).setTimestamp(logRecord.getTimestamp());
                JobAdminConfig.getAdminConfig().getRunNodeHelper().handleRunNodeEvent(eventParam);
                if (eventOffset != null) {
                    ackOffset = eventOffset;
                }
            } catch (Throwable e) {
                log.error("处理运行节点事件日志失败，地址：{}，offset：{}，事件：{}",
                        address, eventOffset, logRecord, e);
                // 当前事件失败后不能跳过，否则 ACK 会永久丢失该事件。
                break;
            }
        }
        return ackOffset;
    }

    /** ACK 失败时停止继续拉取，下一轮由执行器重发未确认事件。 */
    private boolean ackRunNodeLog(String address, String leaseToken, Long ackOffset) {
        RunNodeLogAckParam ackParam = new RunNodeLogAckParam();
        ackParam.setOffset(ackOffset);
        ackParam.setConsumerId(PULL_CONSUMER_ID);
        ackParam.setLeaseToken(leaseToken);
        for (int i = 1; i <= ACK_RETRY_TIMES; i++) {
            try {
                ReturnT<String> ackResult = ExecutorBizProxy.ackRunNodeLog(address, ackParam);
                if (ackResult.getCode() == HandleCodeConstant.HANDLE_CODE_SUCCESS) {
                    log.debug("确认运行节点事件日志，地址：{}，offset：{}，结果：{}", address, ackOffset, ackResult);
                    return true;
                }
                log.warn("确认运行节点事件日志失败，地址：{}，offset：{}，重试次数：{}，结果：{}",
                        address, ackOffset, i, ackResult);
            } catch (Throwable e) {
                log.warn("确认运行节点事件日志异常，地址：{}，offset：{}，重试次数：{}",
                        address, ackOffset, i, e);
            }
            if (i < ACK_RETRY_TIMES && !waitAckRetryInterval()) {
                return false;
            }
        }
        return false;
    }

    /** 等待下一次 ACK，线程中断时交给下一轮拉取继续处理。 */
    private boolean waitAckRetryInterval() {
        try {
            TimeUnit.MILLISECONDS.sleep(ACK_RETRY_INTERVAL_MILLIS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

}
