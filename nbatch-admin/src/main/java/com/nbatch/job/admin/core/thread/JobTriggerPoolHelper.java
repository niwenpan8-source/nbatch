package com.nbatch.job.admin.core.thread;

import com.nbatch.job.admin.core.conf.JobAdminConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
public class JobTriggerPoolHelper {
    private static final JobTriggerPoolHelper INSTANCE = new JobTriggerPoolHelper();
    private final ExecutorService triggerPool = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "nbatch-admin-work-trigger");
        thread.setDaemon(true);
        return thread;
    });

    public static void toStart() {
    }

    public static void toStop() {
        INSTANCE.triggerPool.shutdownNow();
    }

    public static void triggerWork(final String workId) {
        INSTANCE.triggerPool.execute(() -> {
            try {
                JobAdminConfig.getAdminConfig().getRunWorkHelper().initRunWork(workId);
            } catch (Throwable e) {
                log.error("作业流程触发失败，workId：{}", workId, e);
            }
        });
    }
}
