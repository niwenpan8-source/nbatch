package com.nbatch.job.admin.core.thread;

import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.admin.core.enums.TriggerTypeEnum;
import com.nbatch.job.admin.core.trigger.JobTrigger;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * job trigger thread pool helper
 *
 * @author Mr.ni 2018-07-03 21:08:07
 */
@Slf4j
public class JobTriggerPoolHelper {


    // ---------------------- trigger pool ----------------------

    // fast/slow thread pool
    private ThreadPoolExecutor fastTriggerPool = null;
    private ThreadPoolExecutor slowTriggerPool = null;

    public void start(){
        fastTriggerPool = new ThreadPoolExecutor(
                10,
                JobAdminConfig.getAdminConfig().getTriggerPoolFastMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                r -> new Thread(r, "job, admin JobTriggerPoolHelper-fastTriggerPool-" + r.hashCode()),
                (r, executor) -> log.error(">>>>>>>>>>> job, admin JobTriggerPoolHelper-fastTriggerPool execute too fast, Runnable={}", r.toString()));

        slowTriggerPool = new ThreadPoolExecutor(
                10,
                JobAdminConfig.getAdminConfig().getTriggerPoolSlowMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(5000),
                r -> new Thread(r, "job, admin JobTriggerPoolHelper-slowTriggerPool-" + r.hashCode()),
                (r, executor) -> log.error(">>>>>>>>>>> job, admin JobTriggerPoolHelper-slowTriggerPool execute too fast, Runnable={}", r.toString()));
    }


    public void stop() {
        //triggerPool.shutdown();
        fastTriggerPool.shutdownNow();
        slowTriggerPool.shutdownNow();
        log.info(">>>>>>>>> job trigger thread pool shutdown success.");
    }


    // job timeout count
    // ms > min
    private volatile long minTim = System.currentTimeMillis() / 60000;
    private final ConcurrentMap<String, AtomicInteger> jobTimeoutCountMap = new ConcurrentHashMap<>();


    /**
     * add trigger
     */
    public void addTrigger(final String jobId,
                           final TriggerTypeEnum triggerType,
                           final int failRetryCount,
                           final String executorShardingParam,
                           final String executorParam,
                           final String addressList) {

        // choose thread pool
        ThreadPoolExecutor triggerPool = fastTriggerPool;
        AtomicInteger jobTimeoutCount = jobTimeoutCountMap.get(jobId);
        // job-timeout 10 times in 1 min
        if (jobTimeoutCount!=null && jobTimeoutCount.get() > 10) {
            triggerPool = slowTriggerPool;
        }

        // trigger
        triggerPool.execute(new Runnable() {
            @Override
            public void run() {

                long start = System.currentTimeMillis();

                try {
                    // do trigger
                    JobTrigger.trigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList);
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                } finally {

                    // check timeout-count-map
                    long minTiNow = System.currentTimeMillis()/60000;
                    if (minTim != minTiNow) {
                        minTim = minTiNow;
                        jobTimeoutCountMap.clear();
                    }

                    // incr timeout-count-map
                    long cost = System.currentTimeMillis()-start;
                    if (cost > 500) {       // ob-timeout threshold 500ms
                        AtomicInteger timeoutCount = jobTimeoutCountMap.putIfAbsent(jobId, new AtomicInteger(1));
                        if (timeoutCount != null) {
                            timeoutCount.incrementAndGet();
                        }
                    }

                }

            }
            @Override
            public String toString() {
                return "Job Runnable, jobId:"+jobId;
            }
        });
    }



    // ---------------------- helper ----------------------

    private static final JobTriggerPoolHelper HELPER = new JobTriggerPoolHelper();

    public static void toStart() {
        HELPER.start();
    }
    public static void toStop() {
        HELPER.stop();
    }

    /**
     * @param jobId job id
     * @param triggerType trigger type
     * @param failRetryCount
     * 			>=0: use this param
     * 			<0: use param from job info config
     * @param executorShardingParam null: use job param
     * @param executorParam
     *          null: use job param
     *          not null: cover job param
     */
    public static void trigger(String jobId, TriggerTypeEnum triggerType, int failRetryCount, String executorShardingParam, String executorParam,
                               String addressList) {
        HELPER.addTrigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList);
    }

}
