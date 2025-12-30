package com.nbatch.job.admin.core.thread;

import cn.hutool.json.JSONObject;
import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.admin.core.domain.po.JobRunWorkPo;
import com.nbatch.job.admin.core.enums.WorkStatusEnum;
import com.nbatch.job.admin.core.scheduler.JobScheduler;
import com.nbatch.job.core.biz.ExecutorBiz;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.TriggerParam;
import com.nbatch.job.core.constant.HandleCodeConstant;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * job work run node execute
 *
 * @author Mr.ni
 */
@Slf4j
public class JobWorkRunNodeHelper {

    private static final JobWorkRunNodeHelper INSTANCE = new JobWorkRunNodeHelper();

    public static JobWorkRunNodeHelper getInstance() {
        return INSTANCE;
    }

    public static final ConcurrentHashMap<String, JSONObject> RUN_WORK_ID_CACHE = new ConcurrentHashMap<>();


    // ---------------------- monitor ----------------------

    public static void putRunWorkCache(String runWorkId, JSONObject jsonObject) {
        RUN_WORK_ID_CACHE.put(runWorkId, jsonObject);
    }

    private Thread workThread;
    private volatile boolean toStop = false;

    public void start() {

        // for monitor
        workThread = new Thread(() -> {

            // monitor
            while (!toStop) {
                try {
                    executeRunWork();
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

            executeRunWork();

            log.info(">>>>>>>>>>> job, JobWorkMonitorHelper stop");

        });
        workThread.setDaemon(true);
        workThread.setName("job, admin JobWorkMonitorHelper");
        workThread.start();
    }

    /**
     * 执行任务
     */
    private void executeRunWork() {
        // 得到所有需要执行的work
        // Scan Job

        Connection conn = null;
        Boolean connAutoCommit = null;
        PreparedStatement preparedStatement = null;

        try {

            conn = JobAdminConfig.getAdminConfig().getDataSource().getConnection();
            connAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            preparedStatement = conn.prepareStatement("select * from nbatch_job_lock where lock_name = 'schedule_lock' for update");
            preparedStatement.execute();


            List<JobRunWorkPo> aLlNeedRunWorkList = JobAdminConfig.getAdminConfig().getRunWorkHelper().getALlNeedRunWorkList();
            for (JobRunWorkPo jobRunWorkPo : aLlNeedRunWorkList) {


                ExecuteWorkParam executeWorkParam
                        = JobAdminConfig.getAdminConfig().getRunNodeHelper().getEnableExecuteWork(jobRunWorkPo);
                if (executeWorkParam == null) {
                    continue;
                }
                // 如果断电重连咋办，如何恢复这个缓存？ 这里采用被动的，只有当任务执行到的时候才会进行断电重连
                JSONObject jsonObject = RUN_WORK_ID_CACHE.get(executeWorkParam.getRunWorkId());
                if (jsonObject == null) {
                    continue;
                }
                String address = jsonObject.getStr("address");
                TriggerParam triggerParam = jsonObject.get("triggerParam", TriggerParam.class);
                triggerParam.setExecuteWorkParam(executeWorkParam);

                ExecutorBiz executorBiz = JobScheduler.getExecutorBiz(address);
                if (executorBiz == null) {
                    continue;
                }
                JobAdminConfig.getAdminConfig().getRunNodeHelper()
                        .updateNodeRunStatus(triggerParam.getExecuteWorkParam(), WorkStatusEnum.START.getCode());
                ReturnT<String> runResult = executorBiz.run(triggerParam);

                // 如果请求失败需要将作业节点置为停止,当遇到的异常为者执行超时,应该如何处理，如果不出，他会一直超时
                if (runResult.getCode() >= HandleCodeConstant.HANDLE_CODE_FAIL) {
                    for (ExecuteNodeParam executeNodeParam : executeWorkParam.getExecuteNodeParamList()) {
                        JobAdminConfig.getAdminConfig().getRunNodeHelper()
                                .updateNodeStatusById(executeNodeParam.getRunNodeId(),
                                        WorkStatusEnum.STOP.getCode());
                        JobAdminConfig.getAdminConfig().getRunNodeHelper()
                                .updateCallBackRunNodeLog(executeNodeParam.getNodeLogId()
                                        , HandleCodeConstant.HANDLE_CODE_FAIL
                                        , runResult.getMsg());
                    }
                    if (runResult.getCode() >= HandleCodeConstant.HANDLE_CODE_TIMEOUT) {
                        RUN_WORK_ID_CACHE.remove(jobRunWorkPo.getRunWorkId());
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {

            // commit
            if (conn != null) {
                try {
                    conn.commit();
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
                try {
                    conn.setAutoCommit(Boolean.TRUE.equals(connAutoCommit));
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
                try {
                    conn.close();
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
            // close PreparedStatement
            if (null != preparedStatement) {
                try {
                    preparedStatement.close();
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

    }

    /**
     * 缓存运行中的任务
     *
     * @param runWorkId 运行中的任务id
     */
    public static void removeRunWorkCache(String runWorkId) {
        RUN_WORK_ID_CACHE.remove(runWorkId);
    }

    public void toStop() {
        toStop = true;

        workThread.interrupt();
        try {
            workThread.join();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
        RUN_WORK_ID_CACHE.clear();
    }


}
