package com.nbatch.job.admin.core.thread;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogDetailPo;
import com.nbatch.job.admin.core.executor.ExecutorBizProxy;
import com.nbatch.job.core.biz.model.*;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.util.GsonTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 从执行器拉取运行节点详细日志。
 * 明细成功写入数据库后才确认 offset，重复明细按已成功处理。
 */
@Slf4j
public class JobRunNodeLogDetailPullHelper {

    private static final JobRunNodeLogDetailPullHelper INSTANCE = new JobRunNodeLogDetailPullHelper();

    /**
     * 每次拉取的日志条数。
     */
    private static final int PULL_SIZE = 200;

    /**
     * 每个执行器最多拉取的批次数。
     */
    private static final int MAX_PULL_BATCHES_PER_ADDRESS = 5;

    private static final long MAX_PULL_MILLIS_PER_ADDRESS = 5000L;

    /**
     * 拉取间隔。
     */
    private static final long PULL_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(5);

    private static final String PULL_CONSUMER_ID = "detail-" + UUID.randomUUID();

    private Thread pullThread;

    private volatile boolean toStop;

    public static JobRunNodeLogDetailPullHelper getInstance() {
        return INSTANCE;
    }

    /**
     * 启动详细日志拉取线程。
     */
    public void start() {
        toStop = false;
        pullThread = new Thread(() -> {
            while (!toStop) {
                try {
                    pullExecutorRunNodeLogDetail();
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error("拉取运行节点详细日志失败。", e);
                    }
                }
                // 等待下一轮
                waitNextRound();
            }
            log.info("运行节点详细日志拉取线程已销毁。");
        }, "job, admin run node detail log pull");
        pullThread.setDaemon(true);
        pullThread.start();
    }

    /**
     * 停止详细日志拉取线程。
     */
    public void toStop() {
        toStop = true;
        if (pullThread != null) {
            pullThread.interrupt();
            try {
                pullThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 逐个执行器拉取详细日志。
     */
    private void pullExecutorRunNodeLogDetail() {
        List<String> addressList = JobAdminConfig.getAdminConfig().getRegistryAddressHelper()
                .getActiveExecutorAddressList();
        for (String address : addressList) {
            pullAddressRunNodeLogDetail(address);
        }
    }

    /**
     * 拉取单个执行器的多批详细日志。
     */
    private void pullAddressRunNodeLogDetail(String address) {
        // 这里需要做一个超时控制，防止一个执行器拉取时间过长 -> 现在设置为1s
        long deadline = System.currentTimeMillis() + MAX_PULL_MILLIS_PER_ADDRESS;
        for (int batch = 0; batch < MAX_PULL_BATCHES_PER_ADDRESS; batch++) {
            RunNodeLogPullParam pullParam = new RunNodeLogPullParam();
            // 这里需要设置一个 consumerId，用于区分不同的拉取请求，避免多个拉取请求同时拉取同一个执行器的日志
            pullParam.setConsumerId(PULL_CONSUMER_ID);
            pullParam.setMaxSize(PULL_SIZE);
            ReturnT<RunNodeLogPullResult> result = ExecutorBizProxy.pullRunNodeLogDetail(address, pullParam);
            if (result.getCode() != HandleCodeConstant.HANDLE_CODE_SUCCESS || result.getContent() == null || result.getContent().getLogList() == null || result.getContent().getLogList().isEmpty()) {
                return;
            }
            String leaseToken = result.getContent().getLeaseToken();
            if (StrUtil.isBlank(leaseToken)) {
                log.warn("运行节点详细日志租约为空，地址：{}", address);
                return;
            }
            Long ackOffset = saveDetailLogBatch(address, result.getContent().getLogList());
            if (ackOffset == null || !ackRunNodeLogDetail(address, leaseToken, ackOffset)) {
                return;
            }
            if (result.getContent().getLogList().size() < PULL_SIZE || System.currentTimeMillis() >= deadline) {
                return;
            }
        }
    }

    /**
     * 顺序保存一批详细日志，任一条失败时停止确认后续 offset。
     */
    private Long saveDetailLogBatch(String address, List<RunNodeLogPullResult.LogRecord> logList) {
        Long ackOffset = null;
        Map<String, JobWorkRunNodeLogDetailPo> detailLogMap = new LinkedHashMap<>();
        for (RunNodeLogPullResult.LogRecord logRecord : logList) {
            try {
                RunNodeLogDetailParam detailParam = logRecord == null ? null
                        : GsonTool.fromJson(logRecord.getPayload(), RunNodeLogDetailParam.class);
                JobWorkRunNodeLogDetailPo detailLog = toDetailLog(detailParam);
                if (detailLog == null || logRecord.getOffset() == null) {
                    log.error("运行节点详细日志参数不完整，地址：{}，日志：{}", address, logRecord);
                    return null;
                }
                // 同一批中相同明细只插入一次，后续 offset 仍可一起确认。
                detailLogMap.putIfAbsent(detailLog.getDetailLogId(), detailLog);
                ackOffset = logRecord.getOffset();
            } catch (Throwable e) {
                log.error("保存运行节点详细日志失败，地址：{}，offset：{}，日志：{}",
                        address, logRecord == null ? null : logRecord.getOffset(), logRecord, e);
                return null;
            }
        }
        if (!insertMissingDetailLogs(detailLogMap)) {
            return null;
        }
        return ackOffset;
    }

    /**
     * 将传输参数转换为日志明细实体，并校验幂等标识。
     */
    private JobWorkRunNodeLogDetailPo toDetailLog(RunNodeLogDetailParam detailParam) {
        if (detailParam == null || StrUtil.isBlank(detailParam.getDetailLogId()) || StrUtil.isBlank(detailParam.getWorkId())
                || StrUtil.isBlank(detailParam.getNodeId()) || StrUtil.isBlank(detailParam.getNodeLogId())) {
            return null;
        }
        JobWorkRunNodeLogDetailPo detailLog = new JobWorkRunNodeLogDetailPo();
        detailLog.setDetailLogId(detailParam.getDetailLogId());
        detailLog.setWorkId(detailParam.getWorkId());
        detailLog.setNodeId(detailParam.getNodeId());
        detailLog.setNodeLogId(detailParam.getNodeLogId());
        detailLog.setTurnDate(detailParam.getTurnDate());
        detailLog.setHandleMsg(detailParam.getHandleMsg());
        detailLog.setExecuteTime(detailParam.getExecuteTime());
        detailLog.setCreateTime(LocalDateTime.now());
        detailLog.setCallBackTime(LocalDateTime.now());
        return detailLog;
    }

    /**
     * 先批量查询已有明细，再逐条写入缺失记录，避免 ACK 重试频繁触发主键异常。
     */
    private boolean insertMissingDetailLogs(Map<String, JobWorkRunNodeLogDetailPo> detailLogMap) {
        if (detailLogMap.isEmpty()) {
            return true;
        }
        List<JobWorkRunNodeLogDetailPo> existingLogs = JobAdminConfig.getAdminConfig().getLogDetailMapper()
                .selectByIds(new ArrayList<>(detailLogMap.keySet()));
        // 记录在库中已经存在的日志id
        Set<String> existingIdSet = new HashSet<>();
        if (existingLogs != null) {
            for (JobWorkRunNodeLogDetailPo existingLog : existingLogs) {
                existingIdSet.add(existingLog.getDetailLogId());
            }
        }
        for (JobWorkRunNodeLogDetailPo detailLog : detailLogMap.values()) {
            if (existingIdSet.contains(detailLog.getDetailLogId())) {
                continue;
            }
            try {
                JobAdminConfig.getAdminConfig().getLogDetailMapper().insert(detailLog);
            } catch (DuplicateKeyException e) {
                // 查询与插入之间可能有其他管理端完成写入，只确认当前幂等键已存在。
                if (JobAdminConfig.getAdminConfig().getLogDetailMapper().selectById(detailLog.getDetailLogId()) == null) {
                    log.error("运行节点详细日志写入发生主键冲突，且未找到已有记录，detailLogId：{}",
                            detailLog.getDetailLogId(), e);
                    return false;
                }
            } catch (Throwable e) {
                log.error("写入运行节点详细日志失败，detailLogId：{}", detailLog.getDetailLogId(), e);
                return false;
            }
        }
        return true;
    }

    /**
     * 确认已经成功保存的详细日志位置。
     */
    private boolean ackRunNodeLogDetail(String address, String leaseToken, Long offset) {
        RunNodeLogAckParam ackParam = new RunNodeLogAckParam();
        ackParam.setConsumerId(PULL_CONSUMER_ID);
        ackParam.setLeaseToken(leaseToken);
        ackParam.setOffset(offset);
        ReturnT<String> result = ExecutorBizProxy.ackRunNodeLogDetail(address, ackParam);
        if (result.getCode() == HandleCodeConstant.HANDLE_CODE_SUCCESS) {
            return true;
        }
        log.warn("确认运行节点详细日志失败，地址：{}，offset：{}，结果：{}", address, offset, result);
        return false;
    }

    /**
     * 等待下一次拉取，线程中断时立即退出。
     */
    private void waitNextRound() {
        try {
            TimeUnit.MILLISECONDS.sleep(PULL_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
