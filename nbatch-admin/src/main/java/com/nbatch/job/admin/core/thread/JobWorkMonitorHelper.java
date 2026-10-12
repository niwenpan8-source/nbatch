package com.nbatch.job.admin.core.thread;

import com.nbatch.job.admin.core.conf.JobAdminConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 作业运行监控线程
 *
 * @author Mr.ni
 */
@Slf4j
public class JobWorkMonitorHelper {

    private static final JobWorkMonitorHelper INSTANCE = new JobWorkMonitorHelper();

    public static JobWorkMonitorHelper getInstance() {
        return INSTANCE;
    }

    private Thread turnDateThread;
    private Thread timeOutNodeThread;
    private volatile boolean toStop = false;

    public void start() {

        // 修改节点翻牌时间
        turnDateThread = new Thread(() -> {
            while (!toStop) {
                try {
                    log.info(">>>>>>>>>>> 对作业进行翻牌操作。");
                    JobAdminConfig.getAdminConfig().getRunWorkHelper().updateWorkTurnDate();
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(">>>>>>>>>>> 作业失败日期处理线程发生异常：", e);
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
            log.info(">>>>>>>>>>> 作业失败日期处理线程已停止。");
        });
        turnDateThread.setDaemon(true);
        turnDateThread.setName("job, admin JobWorkMonitorHelper-turnDate");
        turnDateThread.start();

        // 修改超过固定时间，任务状态仍然为1的节点任务修改为0
        timeOutNodeThread = new Thread(() -> {
            while (!toStop) {
                try {
                    JobAdminConfig.getAdminConfig().getRunNodeHelper().updateTimeOutRunNodeStatus();
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(">>>>>>>>>>> 作业节点超时处理线程发生异常：", e);
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
            log.info(">>>>>>>>>>> 作业节点超时处理线程已停止。");
        });
        timeOutNodeThread.setDaemon(true);
        timeOutNodeThread.setName("job, admin JobWorkMonitorHelper-timeOutNode");
        timeOutNodeThread.start();
    }

    public void toStop() {
        toStop = true;

        if (turnDateThread != null) {
            turnDateThread.interrupt();
            try {
                turnDateThread.join();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
        if (timeOutNodeThread != null) {
            timeOutNodeThread.interrupt();
            try {
                timeOutNodeThread.join();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }


}
