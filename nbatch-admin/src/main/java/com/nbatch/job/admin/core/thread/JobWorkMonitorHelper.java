package com.nbatch.job.admin.core.thread;

import com.nbatch.job.admin.core.conf.JobAdminConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * job work-monitor instance
 *
 * @author Mr.ni
 */
@Slf4j
public class JobWorkMonitorHelper {

    private static final JobWorkMonitorHelper INSTANCE = new JobWorkMonitorHelper();

    public static JobWorkMonitorHelper getInstance() {
        return INSTANCE;
    }

    // ---------------------- monitor ----------------------

    private Thread workThread;
    private volatile boolean toStop = false;

    public void start() {

        // for monitor
        workThread = new Thread(() -> {
            // wait for JobWorkMonitorHelper-init
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
                    // 修改节点翻牌时间
                    JobAdminConfig.getAdminConfig().getRunNodeHelper().updateWorkTurnDate();
                    // 只保留30天的运行数据
                    JobAdminConfig.getAdminConfig().getRunWorkHelper().deleteRunWork();
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(">>>>>>>>>>> job, job fail work thread error:", e);
                    }
                }

                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                    }
                }

            }

            log.info(">>>>>>>>>>> job, JobWorkMonitorHelper stop");

        });
        workThread.setDaemon(true);
        workThread.setName("job, admin JobWorkMonitorHelper");
        workThread.start();
    }

    public void toStop() {
        toStop = true;

        workThread.interrupt();
        try {
            workThread.join();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }


}
