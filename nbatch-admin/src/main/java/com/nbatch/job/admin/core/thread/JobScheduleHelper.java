package com.nbatch.job.admin.core.thread;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.admin.core.cron.CronExpression;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.enums.MisfireStrategyEnum;
import com.nbatch.job.admin.core.enums.ScheduleTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 懂事任务帮助
 */
@Slf4j
public class JobScheduleHelper {

    private static final JobScheduleHelper INSTANCE = new JobScheduleHelper();

    public static JobScheduleHelper getInstance() {
        return INSTANCE;
    }

    /**
     * 预读时间窗口
     */
    public static final long PRE_READ_MS = 5000;

    private Thread scheduleThread;
    private Thread ringThread;
    private volatile boolean scheduleThreadToStop = false;
    private volatile boolean ringThreadToStop = false;
    private static final Map<Integer, List<String>> RING_DATA = new ConcurrentHashMap<>();

    public void start() {

        // 调度线程
        scheduleThread = new Thread(() -> {

            try {
                TimeUnit.MILLISECONDS.sleep(5000 - System.currentTimeMillis() % 1000);
            } catch (Throwable e) {
                if (!scheduleThreadToStop) {
                    log.error(e.getMessage(), e);
                }
            }
            log.info(">>>>>>>>> 作业管理端调度器初始化成功。");

            // 预读数量：线程池大小乘以触发 QPS（单次触发约 50ms，QPS 为 20）
            int preReadCount = (JobAdminConfig.getAdminConfig().getTriggerPoolFastMax() + JobAdminConfig.getAdminConfig().getTriggerPoolSlowMax()) * 20;


            while (!scheduleThreadToStop) {

                // 扫描待调度作业
                long start = System.currentTimeMillis();

                Connection conn = null;
                Boolean connAutoCommit = null;
                PreparedStatement preparedStatement = null;

                boolean preReadSuc = true;
                try {

                    conn = JobAdminConfig.getAdminConfig().getDataSource().getConnection();
                    connAutoCommit = conn.getAutoCommit();
                    conn.setAutoCommit(false);

                    preparedStatement = conn.prepareStatement("select * from nbatch_lock where lock_name = 'schedule_lock' for update");
                    preparedStatement.execute();

                    // 每次读取数量,这里有个字段 PRE_READ_MS 提前把即将触发的作业捞出来排进时间轮，这里是为了处理延时
                    long nowTime = System.currentTimeMillis();
                    Page<JobWorkPo> schedulePage = JobAdminConfig.getAdminConfig().getJobWorkMapper()
                            .selectPage(new Page<>(0, preReadCount), Wrappers.lambdaQuery(JobWorkPo.class)
                                    .isNotNull(JobWorkPo::getTriggerNextTime)
                                    .le(JobWorkPo::getTriggerNextTime, nowTime + PRE_READ_MS)
                                    .eq(JobWorkPo::getScheduleStatus, 1)
                            );
                    // 得到本次需要调度的任务
                    List<JobWorkPo> scheduleList = schedulePage.getRecords();
                    if (CollUtil.isNotEmpty(scheduleList)) {
                        for (JobWorkPo jobInfo : scheduleList) {
                            // 如果当前时间已经延时5秒执行了
                            if (nowTime > jobInfo.getTriggerNextTime() + PRE_READ_MS) {
                                //
                                log.warn(">>>>>>>>>>> 作业调度错过触发时间，workId = {}", jobInfo.getWorkId());
                                // 确认错过策略
                                MisfireStrategyEnum misfireStrategyEnum = MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), MisfireStrategyEnum.DO_NOTHING);
                                if (MisfireStrategyEnum.FIRE_ONCE_NOW == misfireStrategyEnum) {
                                    // 立即补触发一次
                                    JobTriggerPoolHelper.triggerWork(jobInfo.getWorkId());
                                    log.debug(">>>>>>>>>>> 作业调度推送触发请求，workId = {}", jobInfo.getWorkId());
                                }

                                // 刷新下一次执行时间
                                refreshNextValidTime(jobInfo, new Date());

                            } else if (nowTime > jobInfo.getTriggerNextTime()) {
                                // 2.2、trigger-expire < 5s：direct-trigger && make next-trigger-time

                                // 1、trigger
                                JobTriggerPoolHelper.triggerWork(jobInfo.getWorkId());
                                log.debug(">>>>>>>>>>> 作业调度推送触发请求，workId = {}", jobInfo.getWorkId());

                                // 2、fresh next
                                refreshNextValidTime(jobInfo, new Date());

                                // 下一次触发时间在 5 秒内，重新预读
                                if (jobInfo.getScheduleStatus() == 1 && nowTime + PRE_READ_MS > jobInfo.getTriggerNextTime()) {

                                    // 1、make ring second
                                    int ringSecond = (int) ((jobInfo.getTriggerNextTime() / 1000) % 60);

                                    // 2、push time ring
                                    pushTimeRing(ringSecond, jobInfo.getWorkId());

                                    // 3、fresh next
                                    refreshNextValidTime(jobInfo, new Date(jobInfo.getTriggerNextTime()));

                                }

                            } else {
                                // 2.3、trigger-pre-read：time-ring trigger && make next-trigger-time

                                // 1、make ring second
                                int ringSecond = (int) ((jobInfo.getTriggerNextTime() / 1000) % 60);

                                // 推送到另一个执行队列
                                pushTimeRing(ringSecond, jobInfo.getWorkId());

                                // 刷新下一次执行时间
                                refreshNextValidTime(jobInfo, new Date(jobInfo.getTriggerNextTime()));

                            }

                        }

                        // 更新触发时间和状态
                        for (JobWorkPo jobInfo : scheduleList) {
                            int updateCount = JobAdminConfig.getAdminConfig().getJobWorkMapper().update(null,
                                    Wrappers.lambdaUpdate(JobWorkPo.class)
                                            .set(JobWorkPo::getTriggerLastTime, jobInfo.getTriggerLastTime())
                                            .set(JobWorkPo::getTriggerNextTime, jobInfo.getTriggerNextTime())
                                            .set(JobWorkPo::getScheduleStatus, jobInfo.getScheduleStatus())
                                            .eq(JobWorkPo::getWorkId, jobInfo.getWorkId())
                                            .eq(JobWorkPo::getScheduleStatus, 1));
                            if (updateCount <= 0) {
                                log.warn(">>>>>>>>>>> 更新下次触发时间失败，workId={}，上次触发时间={}，下次触发时间={}",
                                        jobInfo.getWorkId(), jobInfo.getTriggerLastTime(), jobInfo.getTriggerNextTime());
                            }
                        }

                    } else {
                        preReadSuc = false;
                    }

                } catch (Throwable e) {
                    if (!scheduleThreadToStop) {
                        log.error(">>>>>>>>>>> 作业调度线程发生异常：", e);
                    }
                } finally {

                    // 提交事务
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

                    // 关闭预编译语句
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

                // 判断执行时间是否超过1s如果有者执行睡眠
                if (cost < 1000) {
                    try {
                        // 预读周期：有任务时每秒扫描，无任务时跳过本周期
                        TimeUnit.MILLISECONDS.sleep((preReadSuc ? 1000 : PRE_READ_MS) - System.currentTimeMillis() % 1000);
                    } catch (Throwable e) {
                        if (!scheduleThreadToStop) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }

            }

            log.info(">>>>>>>>>>> 作业调度线程已停止。");
        });
        scheduleThread.setDaemon(true);
        scheduleThread.setName("job, admin JobScheduleHelper#scheduleThread");
        scheduleThread.start();


        // 时间轮线程
        ringThread = new Thread(() -> {

            while (!ringThreadToStop) {

                // 对齐到整秒
                try {
                    TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
                } catch (Throwable e) {
                    if (!ringThreadToStop) {
                        log.error(e.getMessage(), e);
                    }
                }

                try {
                    // 获取当前秒对应的数据
                    List<String> ringItemData = new ArrayList<>();
                    int nowSecond = Calendar.getInstance().get(Calendar.SECOND);   // 避免处理耗时太长，跨过刻度，向前校验一个刻度；
                    for (int i = 0; i < 2; i++) {
                        List<String> tmpData = RING_DATA.remove((nowSecond + 60 - i) % 60);
                        if (tmpData != null) {
                            ringItemData.addAll(tmpData);
                        }
                    }

                    // 触发时间轮中的作业
                    log.debug(">>>>>>>>>>> 时间轮心跳：{} = {}", nowSecond, Collections.singletonList(ringItemData));
                    if (CollUtil.isNotEmpty(ringItemData)) {
                        // 逐个触发作业
                        for (String workId : ringItemData) {
                            // 触发作业
                            JobTriggerPoolHelper.triggerWork(workId);
                        }
                        // 清空本轮数据
                        ringItemData.clear();
                    }
                } catch (Throwable e) {
                    if (!ringThreadToStop) {
                        log.error(">>>>>>>>>>> 时间轮线程发生异常：", e);
                    }
                }
            }
            log.info(">>>>>>>>>>> 时间轮线程已停止。");
        });
        ringThread.setDaemon(true);
        ringThread.setName("job, admin JobScheduleHelper#ringThread");
        ringThread.start();
    }

    private void refreshNextValidTime(JobWorkPo jobInfo, Date fromTime) {
        try {
            Date nextValidTime = generateNextValidTime(jobInfo, fromTime);
            if (nextValidTime != null) {
                // 设置下一次执行时间
                jobInfo.setTriggerLastTime(jobInfo.getTriggerNextTime());
                jobInfo.setTriggerNextTime(nextValidTime.getTime());
            } else {
                // 如果为空设置为0挺重视执行
                jobInfo.setTriggerLastTime(0L);
                jobInfo.setTriggerNextTime(0L);
                jobInfo.setScheduleStatus(0);
                log.error(">>>>>>>>>>> 计算作业下次执行时间失败，workId={}，调度类型={}，调度配置={}",
                        jobInfo.getWorkId(), jobInfo.getScheduleType(), jobInfo.getScheduleConf());
            }
        } catch (Throwable e) {
            // 计算下次执行时间失败，停止调度
            jobInfo.setTriggerLastTime(0L);
            jobInfo.setTriggerNextTime(0L);
            jobInfo.setScheduleStatus(0);

            log.error(">>>>>>>>>>> 计算作业下次执行时间异常，workId={}，调度类型={}，调度配置={}",
                    jobInfo.getWorkId(), jobInfo.getScheduleType(), jobInfo.getScheduleConf(), e);
        }
    }

    private void pushTimeRing(int ringSecond, String workId) {
        // 将任务异步放入时间轮
        List<String> ringItemData = RING_DATA.computeIfAbsent(ringSecond, k -> new ArrayList<>());
        ringItemData.add(workId);

        log.debug(">>>>>>>>>>> 作业已推送到时间轮：{} = {}", ringSecond, Collections.singletonList(ringItemData));
    }

    public void toStop() {
        // 停止调度线程
        scheduleThreadToStop = true;
        try {
            // 等待调度线程结束
            TimeUnit.SECONDS.sleep(1);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
        if (scheduleThread.getState() != Thread.State.TERMINATED) {
            // 中断并等待调度线程结束
            scheduleThread.interrupt();
            try {
                scheduleThread.join();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }

        // 检查时间轮中是否还有任务
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

        // 停止时间轮并等待内存任务结束
        ringThreadToStop = true;
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
        if (ringThread.getState() != Thread.State.TERMINATED) {
            // 中断并等待时间轮线程结束
            ringThread.interrupt();
            try {
                ringThread.join();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }

        log.info(">>>>>>>>>>> 作业调度辅助线程已停止。");
    }


    // ---------------------- 工具方法 ----------------------
    public static Date generateNextValidTime(JobWorkPo work, Date fromTime) throws Exception {
        // 得到定时任务类型
        ScheduleTypeEnum scheduleType = ScheduleTypeEnum.match(work.getScheduleType(), null);
        if (ScheduleTypeEnum.CRON == scheduleType) {
            return new CronExpression(work.getScheduleConf()).getNextValidTimeAfter(fromTime);
        } else if (ScheduleTypeEnum.FIX_RATE == scheduleType) {
            return new Date(fromTime.getTime() + Integer.parseInt(work.getScheduleConf()) * 1000L);
        }
        return null;
    }

}
