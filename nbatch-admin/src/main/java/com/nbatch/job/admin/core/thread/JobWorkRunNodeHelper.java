package com.nbatch.job.admin.core.thread;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.executor.ExecutorBizProxy;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 作业运行节点执行线程
 */
@Slf4j
public class JobWorkRunNodeHelper {

    private static final JobWorkRunNodeHelper INSTANCE = new JobWorkRunNodeHelper();

    public static JobWorkRunNodeHelper getInstance() {
        return INSTANCE;
    }

    // ---------------------- monitor ----------------------

    private Thread workThread;
    private volatile boolean toStop = false;

    public void start() {

        // 启动监控线程
        workThread = new Thread(() -> {

            // 执行监控循环
            while (!toStop) {
                try {
                    executeRunWork();
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(">>>>>>>>>>> 作业流程失败处理线程发生异常：", e);
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

            log.info(">>>>>>>>>>> 作业流程运行节点线程已停止。");

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
        // 扫描待执行节点

        Connection conn = null;
        Boolean connAutoCommit = null;
        PreparedStatement preparedStatement = null;
        List<RunWorkDispatchCommand> dispatchCommandList = new ArrayList<>();

        try {

            conn = JobAdminConfig.getAdminConfig().getDataSource().getConnection();
            connAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            preparedStatement = conn.prepareStatement("select * from nbatch_lock where lock_name = 'schedule_lock' for update");
            preparedStatement.execute();

            List<JobWorkPo> allNeedRunWorkList = JobAdminConfig.getAdminConfig().getRunWorkHelper().getAllNeedRunWorkList();
            for (JobWorkPo jobRunWorkPo : allNeedRunWorkList) {


                ExecuteWorkParam executeWorkParam
                        = JobAdminConfig.getAdminConfig().getRunNodeHelper().getEnableExecuteWork(jobRunWorkPo);
                if (executeWorkParam == null) {
                    continue;
                }
                String address = resolveExecutorAddress(executeWorkParam.getWorkId());
                if (StrUtil.isBlank(address)) {
                    log.warn("workId:{} 未找到可用执行器地址，跳过本次继续调度", executeWorkParam.getWorkId());
                    continue;
                }

                executeWorkParam.setExecutorAddress(address);
                // 标记节点为已下发
                JobAdminConfig.getAdminConfig().getRunNodeHelper().markRunNodesDispatched(executeWorkParam);
                dispatchCommandList.add(new RunWorkDispatchCommand(address, executeWorkParam));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {

            // 提交事务
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
            // 关闭预编译语句
            if (null != preparedStatement) {
                try {
                    preparedStatement.close();
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        // 网络调用必须在数据库调度锁释放后执行，单个执行器变慢不会阻塞整轮调度。
        for (RunWorkDispatchCommand command : dispatchCommandList) {
            dispatchRunWork(command);
        }

    }

    /**
     * 在调度锁释放后发送执行命令，并区分确定失败与响应未知。
     */
    private void dispatchRunWork(RunWorkDispatchCommand command) {
        // 调用执行器执行作业
        ReturnT<String> runResult = ExecutorBizProxy.runWork(command.address, command.executeWorkParam);
        // 如果为成功则直接返回
        if (runResult.getCode() < HandleCodeConstant.HANDLE_CODE_FAIL) {
            return;
        }
        String handleMsg = runResult.getMsg();
        // 未知结果，记录并跳过。
        if (isDispatchUnknown(runResult)) {
            JobAdminConfig.getAdminConfig().getRunNodeHelper()
                    .markRunNodesDispatchUnknown(command.executeWorkParam, handleMsg);
            return;
        }
        // 失败则标记为失败
        JobAdminConfig.getAdminConfig().getRunNodeHelper()
                .markRunNodesDispatchFailed(command.executeWorkParam, handleMsg);
    }

    /**
     * 调度锁内生成的不可变命令快照，供锁外执行 RPC。
     */
    private static final class RunWorkDispatchCommand {
        /**
         * 一次锁外下发所需的完整参数。
         */
        private final String address;
        private final ExecuteWorkParam executeWorkParam;

        /**
         * 固化调度锁内计算出的目标执行器和执行参数。
         */
        private RunWorkDispatchCommand(String address, ExecuteWorkParam executeWorkParam) {
            this.address = address;
            this.executeWorkParam = executeWorkParam;
        }
    }

    /**
     * 判断是否为“请求可能已到达执行器，但管理端未拿到响应”的未知结果。
     */
    private boolean isDispatchUnknown(ReturnT<String> runResult) {
        if (runResult == null || StrUtil.isBlank(runResult.getMsg())) {
            return false;
        }
        String msg = runResult.getMsg().toLowerCase();
        return msg.contains("read timed out") || msg.contains("read timeout");
    }

    /**
     * 按作业配置查询执行器，并按注册顺序选择第一个可用地址。
     */
    private String resolveExecutorAddress(String workId) {
        if (StrUtil.isBlank(workId)) {
            return null;
        }
        JobWorkPo work = JobAdminConfig.getAdminConfig().getJobWorkMapper().selectById(workId);
        if (work == null) {
            return null;
        }
        return JobAdminConfig.getAdminConfig().getRegistryAddressHelper().getAddressByOrder(work.getJobGroup());
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
