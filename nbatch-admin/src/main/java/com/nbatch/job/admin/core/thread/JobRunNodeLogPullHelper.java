package com.nbatch.job.admin.core.thread;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.admin.core.domain.po.JobRegistryPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogPo;
import com.nbatch.job.admin.core.executor.ExecutorBizProxy;
import com.nbatch.job.admin.core.helper.RunNodeHelper.NodeStatusContext;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.RunNodeLogAckParam;
import com.nbatch.job.core.biz.model.RunNodeLogEventParam;
import com.nbatch.job.core.biz.model.RunNodeLogPullParam;
import com.nbatch.job.core.biz.model.RunNodeLogPullResult;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.RegistryConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.FAIL;
import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.STARTED;
import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.STOPPED;
import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.SUCCESS;

/**
 * 从执行器拉取运行节点本地事件日志。
 * 对于日志进行分级进行处理
 */
@Slf4j
public class JobRunNodeLogPullHelper {

    private static final JobRunNodeLogPullHelper INSTANCE = new JobRunNodeLogPullHelper();
    private static final int PULL_SIZE = 200;

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
                        log.error("pull run node event log error", e);
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
            log.info(">>>>>>>>>>> job, run node event log pull thread destroy.");
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

    private void pullExecutorRunNodeLog() {
        List<JobRegistryPo> registryList = JobAdminConfig.getAdminConfig().getJobRegistryMapper()
                .selectList(Wrappers.lambdaQuery(JobRegistryPo.class)
                        .eq(JobRegistryPo::getRegistryGroup, RegistryConfig.RegistType.EXECUTOR.name())
                        .ge(JobRegistryPo::getUpdateTime, DateUtil.offsetSecond(new Date(), -RegistryConfig.DEAD_TIMEOUT)));
        if (CollUtil.isEmpty(registryList)) {
            return;
        }
        Set<String> addressSet = new HashSet<>();
        for (JobRegistryPo registryPo : registryList) {
            if (StrUtil.isBlank(registryPo.getRegistryValue())) {
                continue;
            }
            for (String address : registryPo.getRegistryValue().split(StrPool.COMMA)) {
                if (StrUtil.isNotBlank(address)) {
                    addressSet.add(address.trim());
                }
            }
        }
        for (String address : addressSet) {
            pullAddressRunNodeLog(address);
        }
    }

    /**
     * 拉取执行器运行节点事件日志
     *
     * @param address 执行器地址
     */
    private void pullAddressRunNodeLog(String address) {
        RunNodeLogPullParam pullParam = new RunNodeLogPullParam();
        pullParam.setMaxSize(PULL_SIZE);
        ReturnT<RunNodeLogPullResult> pullResult = ExecutorBizProxy.pullRunNodeLog(address, pullParam);
        if (pullResult == null || pullResult.getCode() != HandleCodeConstant.HANDLE_CODE_SUCCESS || pullResult.getContent() == null) {
            return;
        }
        List<RunNodeLogEventParam> eventList = pullResult.getContent().getEventList();
        if (CollUtil.isEmpty(eventList)) {
            return;
        }
        Long ackOffset = null;
        for (RunNodeLogEventParam eventParam : eventList) {
            handleRunNodeLogEvent(eventParam);
            ackOffset = eventParam.getOffset();
        }
        if (ackOffset != null) {
            RunNodeLogAckParam ackParam = new RunNodeLogAckParam();
            ackParam.setOffset(ackOffset);
            ReturnT<String> ackResult = ExecutorBizProxy.ackRunNodeLog(address, ackParam);
            log.debug("ack run node event log, address:{}, offset:{}, result:{}", address, ackOffset, ackResult);
        }
    }

    private void handleRunNodeLogEvent(RunNodeLogEventParam eventParam) {
        if (eventParam == null || eventParam.getNodeLogId() == null) {
            return;
        }
        JobWorkRunNodeLogPo oldLog = JobAdminConfig.getAdminConfig().getRunNodeHelper().getRunNodeLog(eventParam.getNodeLogId());
        if (oldLog != null && oldLog.getCallBackTime() != null && !STARTED.getValue().equals(eventParam.getEventType())) {
            return;
        }
        if (STARTED.getValue().equals(eventParam.getEventType())) {
            handleStartedEvent(eventParam, oldLog);
        } else if (SUCCESS.getValue().equals(eventParam.getEventType())
                || FAIL.getValue().equals(eventParam.getEventType())) {
            handleFinishEvent(eventParam);
        } else if (STOPPED.getValue().equals(eventParam.getEventType())) {
            handleStoppedEvent(eventParam);
        }
    }

    /**
     * 处理开始事件。
     */
    private void handleStartedEvent(RunNodeLogEventParam eventParam, JobWorkRunNodeLogPo oldLog) {
        if (oldLog != null && oldLog.getHandleCode() != null && oldLog.getHandleCode() > 0) {
            return;
        }
        JobAdminConfig.getAdminConfig().getRunNodeHelper()
                .markRunNodeStarted(eventParam, "运行节点执行中");
    }

    /**
     * 处理完成事件。
     */
    private void handleFinishEvent(RunNodeLogEventParam eventParam) {
        if (JobAdminConfig.getAdminConfig().getRunNodeHelper().isRunNodeStopped(eventParam.getRunNodeId())) {
            return;
        }
        int handleCode = eventParam.getHandleCode() == null ? HandleCodeConstant.HANDLE_CODE_FAIL : eventParam.getHandleCode();
        if (handleCode == HandleCodeConstant.HANDLE_CODE_SUCCESS) {
            JobAdminConfig.getAdminConfig().getRunNodeHelper()
                    .handleNodeStatus(NodeStatusContext.complete(eventParam.getRunNodeId(),
                            eventParam.getRunWorkId(), eventParam.getWorkType()));
        } else {
            JobAdminConfig.getAdminConfig().getRunNodeHelper()
                    .handleNodeStatus(NodeStatusContext.retryFail(eventParam.getRunNodeId()));
        }
        JobAdminConfig.getAdminConfig().getRunNodeHelper()
                .updateCallBackRunNodeLog(eventParam.getNodeLogId(), handleCode, eventParam.getHandleMsg());
    }

    private void handleStoppedEvent(RunNodeLogEventParam eventParam) {
        JobAdminConfig.getAdminConfig().getRunNodeHelper().markRunNodeStopped(eventParam);
    }
}
