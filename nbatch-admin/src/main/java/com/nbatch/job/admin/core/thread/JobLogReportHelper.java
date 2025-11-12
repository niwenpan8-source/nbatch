package com.nbatch.job.admin.core.thread;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.admin.core.domain.po.JobLogPo;
import com.nbatch.job.admin.core.domain.po.JobLogReportPo;
import lombok.extern.slf4j.Slf4j;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * job log report helper
 *
 * @author Mr.ni 2019-11-22
 */
@Slf4j
public class JobLogReportHelper {

    private static final JobLogReportHelper INSTANCE = new JobLogReportHelper();

    public static JobLogReportHelper getInstance() {
        return INSTANCE;
    }


    private Thread logrThread;
    private volatile boolean toStop = false;

    public void start() {
        logrThread = new Thread(() -> {

            // last clean log time
            long lastCleanLogTime = 0;


            while (!toStop) {

                // 1、log-report refresh: refresh log report in 3 days
                try {

                    for (int i = 0; i < 3; i++) {

                        // today
                        Calendar itemDay = Calendar.getInstance();
                        itemDay.add(Calendar.DAY_OF_MONTH, -i);
                        itemDay.set(Calendar.HOUR_OF_DAY, 0);
                        itemDay.set(Calendar.MINUTE, 0);
                        itemDay.set(Calendar.SECOND, 0);
                        itemDay.set(Calendar.MILLISECOND, 0);

                        Date todayFrom = itemDay.getTime();

                        itemDay.set(Calendar.HOUR_OF_DAY, 23);
                        itemDay.set(Calendar.MINUTE, 59);
                        itemDay.set(Calendar.SECOND, 59);
                        itemDay.set(Calendar.MILLISECOND, 999);

                        Date todayTo = itemDay.getTime();

                        // refresh log-report every minute
                        JobLogReportPo jobLogReportPo = new JobLogReportPo();
                        jobLogReportPo.setTriggerDay(todayFrom);
                        jobLogReportPo.setRunningCount(0);
                        jobLogReportPo.setSucCount(0);
                        jobLogReportPo.setFailCount(0);

                        List<JobLogPo> jobLogList = JobAdminConfig.getAdminConfig().getJobLogMapper()
                                .selectList(Wrappers.lambdaQuery(JobLogPo.class).between(JobLogPo::getTriggerTime, todayFrom, todayTo));

                        if (CollUtil.isNotEmpty(jobLogList)) {
                            int triggerDayCount = jobLogList.size();
                            int triggerDayCountRunning =
                                    (int) jobLogList.stream()
                                            .filter(jobLogPo -> (jobLogPo.getTriggerCode() == 200 || jobLogPo.getHandleCode() == 200)
                                                    && jobLogPo.getHandleCode() == 0).count();
                            int triggerDayCountSuc = (int) jobLogList.stream().filter(jobLogPo -> jobLogPo.getHandleCode() == 200).count();
                            int triggerDayCountFail = triggerDayCount - triggerDayCountRunning - triggerDayCountSuc;


                            jobLogReportPo.setRunningCount(triggerDayCountRunning);
                            jobLogReportPo.setSucCount(triggerDayCountSuc);
                            jobLogReportPo.setFailCount(triggerDayCountFail);
                        }

                        // do refresh
                        int ret = JobAdminConfig.getAdminConfig().getJobLogReportMapper().update(jobLogReportPo,
                                Wrappers.lambdaUpdate(JobLogReportPo.class)
                                        .eq(JobLogReportPo::getTriggerDay, jobLogReportPo.getTriggerDay()));
                        if (ret < 1) {
                            JobAdminConfig.getAdminConfig().getJobLogReportMapper().insert(jobLogReportPo);
                        }
                    }

                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(">>>>>>>>>>> job, job log report thread error:", e);
                    }
                }

                // 2、log-clean: switch open & once each day
                if (JobAdminConfig.getAdminConfig().getLogretentiondays() > 0
                        && System.currentTimeMillis() - lastCleanLogTime > 24 * 60 * 60 * 1000) {

                    // expire-time
                    Calendar expiredDay = Calendar.getInstance();
                    expiredDay.add(Calendar.DAY_OF_MONTH, -1 * JobAdminConfig.getAdminConfig().getLogretentiondays());
                    expiredDay.set(Calendar.HOUR_OF_DAY, 0);
                    expiredDay.set(Calendar.MINUTE, 0);
                    expiredDay.set(Calendar.SECOND, 0);
                    expiredDay.set(Calendar.MILLISECOND, 0);
                    Date clearBeforeTime = expiredDay.getTime();
                    List<String> logIds = null;
                    do {
                        Page<JobLogPo> jobLogPoPage = JobAdminConfig.getAdminConfig().getJobLogMapper()
                                .selectPage(new Page<>(0, 1000L), Wrappers.lambdaQuery(JobLogPo.class)
                                        .eq(JobLogPo::getTriggerTime, clearBeforeTime));
                        if (CollUtil.isNotEmpty(jobLogPoPage.getRecords())) {
                            logIds = jobLogPoPage.getRecords().stream().map(JobLogPo::getId).collect(Collectors.toList());
                            JobAdminConfig.getAdminConfig().getJobLogMapper().delete(Wrappers
                                    .lambdaQuery(JobLogPo.class).in(JobLogPo::getId, logIds));
                        }
                    } while (CollUtil.isNotEmpty(logIds));

                    // update clean time
                    lastCleanLogTime = System.currentTimeMillis();
                }

                try {
                    TimeUnit.MINUTES.sleep(1);
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                    }
                }

            }

            log.info(">>>>>>>>>>> job, job log report thread stop");

        });
        logrThread.setDaemon(true);
        logrThread.setName("job, admin JobLogReportHelper");
        logrThread.start();
    }

    public void toStop() {
        toStop = true;
        // interrupt and wait
        logrThread.interrupt();
        try {
            logrThread.join();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }

}
