package com.nbatch.job.admin.core.thread;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.admin.core.domain.po.JobInfoPo;
import com.nbatch.job.admin.core.domain.po.JobLogPo;
import com.nbatch.job.admin.core.trigger.TriggerTypeEnum;
import com.nbatch.job.admin.core.util.I18nUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * job monitor instance
 *
 * @author Mr.ni 2015-9-1 18:05:56
 */
@Slf4j
public class JobFailMonitorHelper {

    private static final JobFailMonitorHelper INSTANCE = new JobFailMonitorHelper();

    public static JobFailMonitorHelper getInstance() {
        return INSTANCE;
    }

    // ---------------------- monitor ----------------------

    private Thread monitorThread;
    private volatile boolean toStop = false;

    public void start() {
        monitorThread = new Thread(() -> {

            // monitor
            while (!toStop) {
                try {
                    List<String> failLogIds = null;
                    Page<JobLogPo> failLogPage = JobAdminConfig.getAdminConfig().getJobLogMapper()
                            .selectPage(new Page<>(0, 1000L), Wrappers.lambdaQuery(JobLogPo.class)
                                    .and(x -> x.and(x1 -> x1.in(JobLogPo::getTriggerCode, 0, 200)).or()
                                            .eq(JobLogPo::getHandleCode, 200))
                                    .eq(JobLogPo::getAlarmStatus, 0)
                                    .orderByAsc(JobLogPo::getId)
                            );
                    if (CollUtil.isNotEmpty(failLogPage.getRecords())) {
                        failLogIds = failLogPage.convert(JobLogPo::getId).getRecords();
                    }
                    if (CollUtil.isNotEmpty(failLogIds)) {
                        for (String failLogId : failLogIds) {
                            JobLogPo updateAlarmLock = new JobLogPo();
                            updateAlarmLock.setAlarmStatus(-1);
                            int lockRet = JobAdminConfig.getAdminConfig().getJobLogMapper().update(updateAlarmLock, Wrappers
                                    .lambdaUpdate(JobLogPo.class).eq(JobLogPo::getId, failLogId)
                                    .eq(JobLogPo::getAlarmStatus, 0));
                            if (lockRet < 1) {
                                continue;
                            }
                            JobLogPo logInfo = JobAdminConfig.getAdminConfig().getJobLogMapper().selectById(failLogId);
                            JobInfoPo info = JobAdminConfig.getAdminConfig().getJobInfoMapper().selectById(logInfo.getJobId());

                            // 1、fail retry monitor
                            if (logInfo.getExecutorFailRetryCount() > 0) {
                                JobTriggerPoolHelper.trigger(logInfo.getJobId(), TriggerTypeEnum.RETRY, (logInfo.getExecutorFailRetryCount() - 1), logInfo.getExecutorShardingParam(), logInfo.getExecutorParam(), null);
                                String retryMsg = "<br><br><span style=\"color:#F39C12;\" > >>>>>>>>>>>" + I18nUtil.getString("jobconf_trigger_type_retry") + "<<<<<<<<<<< </span><br>";
                                logInfo.setTriggerMsg(logInfo.getTriggerMsg() + retryMsg);
                                JobAdminConfig.getAdminConfig().getJobLogMapper().updateById(logInfo);
                            }

                            // 2、fail alarm monitor
                            int newAlarmStatus;        // 告警状态：0-默认、-1=锁定状态、1-无需告警、2-告警成功、3-告警失败
                            if (info != null) {
                                boolean alarmResult = JobAdminConfig.getAdminConfig().getJobAlarmer().alarm(info, logInfo);
                                newAlarmStatus = alarmResult ? 2 : 3;
                            } else {
                                newAlarmStatus = 1;
                            }

                            JobLogPo updateAlarmUnlock = new JobLogPo();
                            updateAlarmUnlock.setAlarmStatus(newAlarmStatus);
                            JobAdminConfig.getAdminConfig().getJobLogMapper().update(updateAlarmUnlock, Wrappers
                                    .lambdaUpdate(JobLogPo.class).eq(JobLogPo::getId, failLogId)
                                    .eq(JobLogPo::getAlarmStatus, -1));
                        }
                    }

                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(">>>>>>>>>>> xxl-job, job fail monitor thread error:", e);
                    }
                }

                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                    }
                }

            }

            log.info(">>>>>>>>>>> xxl-job, job fail monitor thread stop");

        });
        monitorThread.setDaemon(true);
        monitorThread.setName("xxl-job, admin JobFailMonitorHelper");
        monitorThread.start();
    }

    public void toStop() {
        toStop = true;
        // interrupt and wait
        monitorThread.interrupt();
        try {
            monitorThread.join();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

}
