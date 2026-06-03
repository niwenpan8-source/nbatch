package com.nbatch.job.admin.core.thread;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.complete.JobCompleter;
import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.admin.core.domain.po.JobLogPo;
import com.nbatch.job.admin.core.domain.po.JobRegistryPo;
import com.nbatch.job.admin.core.util.I18nUtil;
import com.nbatch.job.core.biz.model.HandleCallbackParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.RunWorkStatusEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.nbatch.job.core.enums.CallbackTypeEnum.LOG_CALLBACK;
import static com.nbatch.job.core.enums.CallbackTypeEnum.NODE_STATUS_CALLBACK;

/**
 * job lose-monitor instance
 *
 * @author Mr.ni
 */
@Slf4j
public class JobCompleteHelper {

    private static final JobCompleteHelper INSTANCE = new JobCompleteHelper();

    public static JobCompleteHelper getInstance() {
        return INSTANCE;
    }

    // ---------------------- monitor ----------------------

    private ThreadPoolExecutor callbackThreadPool = null;
    private Thread monitorThread;
    private volatile boolean toStop = false;

    public void start() {

        // for callback
        callbackThreadPool = new ThreadPoolExecutor(
                2,
                20,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(3000),
                r -> new Thread(r, "job, admin JobLosedMonitorHelper-callbackThreadPool-" + r.hashCode()),
                (r, executor) -> {
                    r.run();
                    log.warn(">>>>>>>>>>> job, callback too fast, match threadpool rejected handler(run now).");
                });


        // for monitor
        monitorThread = new Thread(() -> {

            // wait for JobTriggerPoolHelper-init
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (Throwable e) {
                if (!toStop) {
                    log.error(e.getMessage(), e);
                }
            }

            // monitor
            while (!toStop) {
                try {
                    // 任务结果丢失处理：调度记录停留在 "运行中" 状态超过10min，且对应执行器心跳注册失败不在线，则将本地调度主动标记失败；
                    Date losedTime = DateUtil.offsetMinute(new Date(), -10);

                    List<JobLogPo> jobLogList = JobAdminConfig.getAdminConfig().getJobLogMapper()
                            .selectList(Wrappers.lambdaQuery(JobLogPo.class)
                                    .eq(JobLogPo::getTriggerCode, 200)
                                    .eq(JobLogPo::getHandleCode, 0)
                                    .le(JobLogPo::getTriggerTime, losedTime));
                    List<JobRegistryPo> jobRegistryPos = JobAdminConfig.getAdminConfig().getJobRegistryMapper()
                            .selectList(Wrappers.lambdaQuery(JobRegistryPo.class));
                    List<String> registryValueList = jobRegistryPos.stream().map(JobRegistryPo::getRegistryValue)
                            .filter(Objects::nonNull).collect(Collectors.toList());
                    List<String> loseJobIds = jobLogList.stream()
                            .filter(jobLogPo -> !registryValueList.contains(jobLogPo.getExecutorAddress()))
                            .map(JobLogPo::getId).collect(Collectors.toList());


                    if (CollUtil.isNotEmpty(loseJobIds)) {
                        for (String logId : loseJobIds) {

                            JobLogPo jobLog = new JobLogPo();
                            jobLog.setId(logId);

                            jobLog.setHandleTime(new Date());
                            jobLog.setHandleCode(HandleCodeConstant.HANDLE_CODE_FAIL);
                            jobLog.setHandleMsg(I18nUtil.getString("joblog_lost_fail"));

                            JobCompleter.updateHandleInfoAndFinish(jobLog);
                        }

                    }
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(">>>>>>>>>>> job, job fail monitor thread error:", e);
                    }
                }

                try {
                    TimeUnit.SECONDS.sleep(60);
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                    }
                }

            }

            log.info(">>>>>>>>>>> job, JobLosedMonitorHelper stop");

        });
        monitorThread.setDaemon(true);
        monitorThread.setName("job, admin JobLosedMonitorHelper");
        monitorThread.start();
    }

    public void toStop() {
        toStop = true;

        // stop registryOrRemoveThreadPool
        callbackThreadPool.shutdownNow();

        // stop monitorThread (interrupt and wait)
        monitorThread.interrupt();
        try {
            monitorThread.join();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }


    // ---------------------- helper ----------------------

    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {

        callbackThreadPool.execute(() -> {
            for (HandleCallbackParam handleCallbackParam : callbackParamList) {
                ReturnT<String> callbackResult = callback(handleCallbackParam);
                log.debug(">>>>>>>>> JobApiController.callback {}, handleCallbackParam={}, callbackResult={}",
                        (callbackResult.getCode() == HandleCodeConstant.HANDLE_CODE_SUCCESS ? "success" : "fail"), handleCallbackParam, callbackResult);
            }
        });

        return ReturnT.SUCCESS;
    }

    private ReturnT<String> callback(HandleCallbackParam handleCallbackParam) {
        if (StrUtil.equals(handleCallbackParam.getCallBackType(), LOG_CALLBACK.getValue())) {
            return logCallback(handleCallbackParam);
        } else if (StrUtil.equals(handleCallbackParam.getCallBackType(), NODE_STATUS_CALLBACK.getValue())) {
            return nodeStatusCallback(handleCallbackParam);
        }
        return ReturnT.SUCCESS;
    }

    /**
     * log callback
     */
    private ReturnT<String> logCallback(HandleCallbackParam handleCallbackParam) {
        // valid log item
        JobLogPo logInfo = JobAdminConfig.getAdminConfig().getJobLogMapper().selectById(handleCallbackParam.getLogId());
        if (logInfo == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "log item not found.");
        }
        if (logInfo.getHandleCode() > 0) {
            // avoid repeat callback, trigger child job etc
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "log repeat callback.");
        }
        // handle msg
        StringBuilder handleMsg = new StringBuilder();
        if (logInfo.getHandleMsg() != null) {
            handleMsg.append(logInfo.getHandleMsg()).append("<br>");
        }
        if (handleCallbackParam.getLogCallBackParam().getHandleMsg() != null) {
            handleMsg.append(handleCallbackParam.getLogCallBackParam().getHandleMsg());
        }

        // success, save log
        logInfo.setHandleTime(new Date());
        log.info(">>>>>>>>>>> handleCallbackParam.getLogCallBackParam().getHandleCode():{}"
                , handleCallbackParam.getLogCallBackParam().getHandleCode());
        logInfo.setHandleCode(handleCallbackParam.getLogCallBackParam().getHandleCode());
        logInfo.setHandleMsg(handleMsg.toString());
        JobCompleter.updateHandleInfoAndFinish(logInfo);
        return ReturnT.SUCCESS;
    }

    /**
     * node status callback
     */
    private ReturnT<String> nodeStatusCallback(HandleCallbackParam handleCallbackParam) {
        log.info(">>>>>>>>>>> job, node status callback, handleCallbackParam: {}", handleCallbackParam);
        HandleCallbackParam.NodeStatusCallbackParam nodeStatusCallbackParam
                = handleCallbackParam.getNodeStatusCallbackParam();
        if (nodeStatusCallbackParam.getHandleCode() == HandleCodeConstant.HANDLE_CODE_SUCCESS) {
            JobAdminConfig.getAdminConfig().getRunNodeHelper()
                    .updateNodeTurnDate(nodeStatusCallbackParam.getRunNodeId(),
                            nodeStatusCallbackParam.getRunWorkId(), nodeStatusCallbackParam.getWorkType());
        } else {
            JobAdminConfig.getAdminConfig().getRunNodeHelper()
                    .updateNodeStatusById(nodeStatusCallbackParam.getRunNodeId(),
                            RunWorkStatusEnum.FAIL.getCode());
        }
        JobAdminConfig.getAdminConfig().getRunNodeHelper()
                .updateCallBackRunNodeLog(nodeStatusCallbackParam.getNodeLogId()
                        , nodeStatusCallbackParam.getHandleCode()
                        , nodeStatusCallbackParam.getHandleMsg());
        return ReturnT.SUCCESS;
    }


}
