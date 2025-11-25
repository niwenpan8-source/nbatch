package com.nbatch.job.admin.core.thread;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.admin.core.cron.CronExpression;
import com.nbatch.job.admin.core.domain.po.JobInfoPo;
import com.nbatch.job.admin.core.enums.MisfireStrategyEnum;
import com.nbatch.job.admin.core.enums.ScheduleTypeEnum;
import com.nbatch.job.admin.core.enums.TriggerTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Mr.ni 2019-05-21
 */
@Slf4j
public class JobScheduleHelper {

    private static final JobScheduleHelper INSTANCE = new JobScheduleHelper();

    public static JobScheduleHelper getInstance() {
        return INSTANCE;
    }

    /**
     * pre read
     */
    public static final long PRE_READ_MS = 5000;

    private Thread scheduleThread;
    private Thread ringThread;
    private volatile boolean scheduleThreadToStop = false;
    private volatile boolean ringThreadToStop = false;
    private static final Map<Integer, List<String>> RING_DATA = new ConcurrentHashMap<>();

    public void start() {

        // schedule thread
        scheduleThread = new Thread(() -> {

            try {
                TimeUnit.MILLISECONDS.sleep(5000 - System.currentTimeMillis() % 1000);
            } catch (Throwable e) {
                if (!scheduleThreadToStop) {
                    log.error(e.getMessage(), e);
                }
            }
            log.info(">>>>>>>>> init job admin scheduler success.");

            // pre-read count: treadpool-size * trigger-qps (each trigger cost 50ms, qps = 1000/50 = 20)
            int preReadCount = (JobAdminConfig.getAdminConfig().getTriggerPoolFastMax() + JobAdminConfig.getAdminConfig().getTriggerPoolSlowMax()) * 20;


            while (!scheduleThreadToStop) {

                // Scan Job
                long start = System.currentTimeMillis();

                Connection conn = null;
                Boolean connAutoCommit = null;
                PreparedStatement preparedStatement = null;

                boolean preReadSuc = true;
                try {

                    conn = JobAdminConfig.getAdminConfig().getDataSource().getConnection();
                    connAutoCommit = conn.getAutoCommit();
                    conn.setAutoCommit(false);

                    preparedStatement = conn.prepareStatement("select * from nbatch_job_lock where lock_name = 'schedule_lock' for update");
                    preparedStatement.execute();

                    // tx start

                    // 1、pre read
                    long nowTime = System.currentTimeMillis();
                    Page<JobInfoPo> schedulePage = JobAdminConfig.getAdminConfig().getJobInfoMapper()
                            .selectPage(new Page<>(0, preReadCount), Wrappers.lambdaQuery(JobInfoPo.class)
                                    .le(JobInfoPo::getTriggerNextTime, nowTime + PRE_READ_MS)
                                    .eq(JobInfoPo::getTriggerStatus, 1)
                            );
                    List<JobInfoPo> scheduleList = schedulePage.getRecords();
                    if (CollUtil.isNotEmpty(scheduleList)) {
                        // 2、push time-ring
                        for (JobInfoPo jobInfo : scheduleList) {

                            // time-ring jump
                            if (nowTime > jobInfo.getTriggerNextTime() + PRE_READ_MS) {
                                // 2.1、trigger-expire > 5s：pass && make next-trigger-time
                                log.warn(">>>>>>>>>>> job, schedule misfire, jobId = " + jobInfo.getId());

                                // 1、misfire match
                                MisfireStrategyEnum misfireStrategyEnum = MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), MisfireStrategyEnum.DO_NOTHING);
                                if (MisfireStrategyEnum.FIRE_ONCE_NOW == misfireStrategyEnum) {
                                    // FIRE_ONCE_NOW 》 trigger
                                    JobTriggerPoolHelper.trigger(jobInfo.getId(), TriggerTypeEnum.MISFIRE, -1, null, null, null);
                                    log.debug(">>>>>>>>>>> job, schedule push trigger : jobId = {}", jobInfo.getId());
                                }

                                // 2、fresh next
                                refreshNextValidTime(jobInfo, new Date());

                            } else if (nowTime > jobInfo.getTriggerNextTime()) {
                                // 2.2、trigger-expire < 5s：direct-trigger && make next-trigger-time

                                // 1、trigger
                                JobTriggerPoolHelper.trigger(jobInfo.getId(), TriggerTypeEnum.CRON, -1, null, null, null);
                                log.debug(">>>>>>>>>>> job, schedule push trigger : jobId = {}", jobInfo.getId());

                                // 2、fresh next
                                refreshNextValidTime(jobInfo, new Date());

                                // next-trigger-time in 5s, pre-read again
                                if (jobInfo.getTriggerStatus() == 1 && nowTime + PRE_READ_MS > jobInfo.getTriggerNextTime()) {

                                    // 1、make ring second
                                    int ringSecond = (int) ((jobInfo.getTriggerNextTime() / 1000) % 60);

                                    // 2、push time ring
                                    pushTimeRing(ringSecond, jobInfo.getId());

                                    // 3、fresh next
                                    refreshNextValidTime(jobInfo, new Date(jobInfo.getTriggerNextTime()));

                                }

                            } else {
                                // 2.3、trigger-pre-read：time-ring trigger && make next-trigger-time

                                // 1、make ring second
                                int ringSecond = (int) ((jobInfo.getTriggerNextTime() / 1000) % 60);

                                // 2、push time ring
                                pushTimeRing(ringSecond, jobInfo.getId());

                                // 3、fresh next
                                refreshNextValidTime(jobInfo, new Date(jobInfo.getTriggerNextTime()));

                            }

                        }

                        // 3、update trigger info
                        for (JobInfoPo jobInfo : scheduleList) {
                            JobAdminConfig.getAdminConfig().getJobInfoMapper().update(jobInfo,
                                    Wrappers.lambdaUpdate(jobInfo)
                                            .set(JobInfoPo::getTriggerLastTime, jobInfo.getTriggerLastTime())
                                            .set(JobInfoPo::getTriggerNextTime, jobInfo.getTriggerNextTime())
                                            .set(jobInfo.getTriggerStatus() >= 0, JobInfoPo::getTriggerStatus, jobInfo.getTriggerStatus())
                                            .eq(JobInfoPo::getId, jobInfo.getId())
                                            .eq(JobInfoPo::getTriggerStatus, 1));
                        }

                    } else {
                        preReadSuc = false;
                    }

                    // tx stop


                } catch (Throwable e) {
                    if (!scheduleThreadToStop) {
                        log.error(">>>>>>>>>>> job, JobScheduleHelper#scheduleThread error:", e);
                    }
                } finally {

                    // commit
                    if (conn != null) {
                        try {
                            conn.commit();
                        } catch (Throwable e) {
                            if (!scheduleThreadToStop) {
                                log.error(e.getMessage(), e);
                            }
                        }
                        try {
                            conn.setAutoCommit(Boolean.TRUE.equals(connAutoCommit));
                        } catch (Throwable e) {
                            if (!scheduleThreadToStop) {
                                log.error(e.getMessage(), e);
                            }
                        }
                        try {
                            conn.close();
                        } catch (Throwable e) {
                            if (!scheduleThreadToStop) {
                                log.error(e.getMessage(), e);
                            }
                        }
                    }

                    // close PreparedStatement
                    if (null != preparedStatement) {
                        try {
                            preparedStatement.close();
                        } catch (Throwable e) {
                            if (!scheduleThreadToStop) {
                                log.error(e.getMessage(), e);
                            }
                        }
                    }
                }
                long cost = System.currentTimeMillis() - start;


                // Wait seconds, align second
                if (cost < 1000) {  // scan-overtime, not wait
                    try {
                        // pre-read period: success > scan each second; fail > skip this period;
                        TimeUnit.MILLISECONDS.sleep((preReadSuc ? 1000 : PRE_READ_MS) - System.currentTimeMillis() % 1000);
                    } catch (Throwable e) {
                        if (!scheduleThreadToStop) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }

            }

            log.info(">>>>>>>>>>> job, JobScheduleHelper#scheduleThread stop");
        });
        scheduleThread.setDaemon(true);
        scheduleThread.setName("job, admin JobScheduleHelper#scheduleThread");
        scheduleThread.start();


        // ring thread
        ringThread = new Thread(() -> {

            while (!ringThreadToStop) {

                // align second
                try {
                    TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
                } catch (Throwable e) {
                    if (!ringThreadToStop) {
                        log.error(e.getMessage(), e);
                    }
                }

                try {
                    // second data
                    List<String> ringItemData = new ArrayList<>();
                    int nowSecond = Calendar.getInstance().get(Calendar.SECOND);   // 避免处理耗时太长，跨过刻度，向前校验一个刻度；
                    for (int i = 0; i < 2; i++) {
                        List<String> tmpData = RING_DATA.remove((nowSecond + 60 - i) % 60);
                        if (tmpData != null) {
                            ringItemData.addAll(tmpData);
                        }
                    }

                    // ring trigger
                    log.debug(">>>>>>>>>>> job, time-ring beat : " + nowSecond + " = " + Collections.singletonList(ringItemData));
                    if (CollUtil.isNotEmpty(ringItemData)) {
                        // do trigger
                        for (String jobId : ringItemData) {
                            // do trigger
                            JobTriggerPoolHelper.trigger(jobId, TriggerTypeEnum.CRON, -1, null, null, null);
                        }
                        // clear
                        ringItemData.clear();
                    }
                } catch (Throwable e) {
                    if (!ringThreadToStop) {
                        log.error(">>>>>>>>>>> job, JobScheduleHelper#ringThread error:", e);
                    }
                }
            }
            log.info(">>>>>>>>>>> job, JobScheduleHelper#ringThread stop");
        });
        ringThread.setDaemon(true);
        ringThread.setName("job, admin JobScheduleHelper#ringThread");
        ringThread.start();
    }

    private void refreshNextValidTime(JobInfoPo jobInfo, Date fromTime) {
        try {
            Date nextValidTime = generateNextValidTime(jobInfo, fromTime);
            if (nextValidTime != null) {
                // pass, may be Inaccurate
                jobInfo.setTriggerStatus(-1);
                jobInfo.setTriggerLastTime(jobInfo.getTriggerNextTime());
                jobInfo.setTriggerNextTime(nextValidTime.getTime());
            } else {
                // generateNextValidTime fail, stop job
                jobInfo.setTriggerStatus(0);
                jobInfo.setTriggerLastTime(0L);
                jobInfo.setTriggerNextTime(0L);
                log.error(">>>>>>>>>>> job, refreshNextValidTime fail for job: jobId={}, scheduleType={}, scheduleConf={}",
                        jobInfo.getId(), jobInfo.getScheduleType(), jobInfo.getScheduleConf());
            }
        } catch (Throwable e) {
            // generateNextValidTime error, stop job
            jobInfo.setTriggerStatus(0);
            jobInfo.setTriggerLastTime(0L);
            jobInfo.setTriggerNextTime(0L);

            log.error(">>>>>>>>>>> job, refreshNextValidTime error for job: jobId={}, scheduleType={}, scheduleConf={}",
                    jobInfo.getId(), jobInfo.getScheduleType(), jobInfo.getScheduleConf(), e);
        }
    }

    private void pushTimeRing(int ringSecond, String jobId) {
        // push async ring
        List<String> ringItemData = RING_DATA.computeIfAbsent(ringSecond, k -> new ArrayList<>());
        ringItemData.add(jobId);

        log.debug(">>>>>>>>>>> job, schedule push time-ring : {} = {}", ringSecond, Collections.singletonList(ringItemData));
    }

    public void toStop() {
        // 1、stop schedule
        scheduleThreadToStop = true;
        try {
            // wait
            TimeUnit.SECONDS.sleep(1);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
        if (scheduleThread.getState() != Thread.State.TERMINATED) {
            // interrupt and wait
            scheduleThread.interrupt();
            try {
                scheduleThread.join();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }

        // if has ring data
        boolean hasRingData = false;
        if (!RING_DATA.isEmpty()) {
            for (int second : RING_DATA.keySet()) {
                List<String> tmpData = RING_DATA.get(second);
                if (CollUtil.isNotEmpty(tmpData)) {
                    hasRingData = true;
                    break;
                }
            }
        }
        if (hasRingData) {
            try {
                TimeUnit.SECONDS.sleep(8);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }

        // stop ring (wait job-in-memory stop)
        ringThreadToStop = true;
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
        if (ringThread.getState() != Thread.State.TERMINATED) {
            // interrupt and wait
            ringThread.interrupt();
            try {
                ringThread.join();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }

        log.info(">>>>>>>>>>> job, JobScheduleHelper stop");
    }


    // ---------------------- tools ----------------------
    public static Date generateNextValidTime(JobInfoPo jobInfo, Date fromTime) throws Exception {
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
        if (ScheduleTypeEnum.CRON == scheduleTypeEnum) {
            return new CronExpression(jobInfo.getScheduleConf()).getNextValidTimeAfter(fromTime);
        } else if (ScheduleTypeEnum.FIX_RATE == scheduleTypeEnum) {
            return new Date(fromTime.getTime() + Integer.parseInt(jobInfo.getScheduleConf()) * 1000L);
        }
        return null;
    }

}
