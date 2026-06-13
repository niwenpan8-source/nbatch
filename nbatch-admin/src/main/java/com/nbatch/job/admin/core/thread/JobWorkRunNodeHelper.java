package com.nbatch.job.admin.core.thread;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.admin.core.domain.RunWorkExecuteContext;
import com.nbatch.job.admin.core.domain.po.JobGroupPo;
import com.nbatch.job.admin.core.domain.po.JobInfoPo;
import com.nbatch.job.admin.core.domain.po.JobLogPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunPo;
import com.nbatch.job.admin.core.enums.ExecutorRouteStrategyEnum;
import com.nbatch.job.admin.core.executor.ExecutorBizProxy;
import com.nbatch.job.admin.core.helper.RunNodeHelper.NodeStatusContext;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.TriggerParam;
import com.nbatch.job.core.constant.HandleCodeConstant;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
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

    public static final ConcurrentHashMap<String, RunWorkExecuteContext> RUN_WORK_ID_CACHE = new ConcurrentHashMap<>();


    // ---------------------- monitor ----------------------

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

            List<JobWorkRunPo> allNeedRunWorkList = JobAdminConfig.getAdminConfig().getRunWorkHelper().getAllNeedRunWorkList();
            for (JobWorkRunPo jobRunWorkPo : allNeedRunWorkList) {


                ExecuteWorkParam executeWorkParam
                        = JobAdminConfig.getAdminConfig().getRunNodeHelper().getEnableExecuteWork(jobRunWorkPo);
                if (executeWorkParam == null) {
                    continue;
                }
                RunWorkExecuteContext context = getRunWorkExecuteContext(jobRunWorkPo, executeWorkParam);
                if (context == null || context.getTriggerParam() == null) {
                    log.warn("runWorkId:{} 缺少运行上下文，跳过本次继续调度", executeWorkParam.getRunWorkId());
                    continue;
                }

                TriggerParam triggerParam = context.getTriggerParam();
                triggerParam.setExecuteWorkParam(executeWorkParam);

                String address = resolveExecutorAddress(context, triggerParam);
                if (StrUtil.isBlank(address)) {
                    log.warn("runWorkId:{} 未找到可用执行器地址，跳过本次继续调度", executeWorkParam.getRunWorkId());
                    continue;
                }

                context.setAddress(address).setUpdateTime(System.currentTimeMillis());
                RUN_WORK_ID_CACHE.put(executeWorkParam.getRunWorkId(), context);
                executeWorkParam.setExecutorAddress(address);

                ReturnT<String> runResult = ExecutorBizProxy.run(address, triggerParam);

                // 如果存在网络问题，则将运行作业标记为异常
                if (runResult == null || runResult.getCode() >= HandleCodeConstant.HANDLE_CODE_FAIL) {
                    String handleMsg = runResult == null ? "运行节点下发失败" : runResult.getMsg();
                    JobAdminConfig.getAdminConfig().getRunNodeHelper()
                            .markRunNodesDispatchFailed(executeWorkParam, handleMsg);
                    if (runResult == null || runResult.getCode() >= HandleCodeConstant.HANDLE_CODE_TIMEOUT) {
                        RUN_WORK_ID_CACHE.remove(jobRunWorkPo.getRunWorkId());
                    }
                } else {
                    JobAdminConfig.getAdminConfig().getRunNodeHelper()
                            .handleNodeStatus(NodeStatusContext.dispatched(executeWorkParam));
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

    private RunWorkExecuteContext getRunWorkExecuteContext(JobWorkRunPo jobRunWorkPo, ExecuteWorkParam executeWorkParam) {
        String runWorkId = executeWorkParam.getRunWorkId();
        RunWorkExecuteContext context = RUN_WORK_ID_CACHE.get(runWorkId);
        if (context != null) {
            return context;
        }

        context = rebuildRunWorkExecuteContext(jobRunWorkPo);
        if (context != null) {
            RUN_WORK_ID_CACHE.put(runWorkId, context);
            log.info("runWorkId:{} 运行上下文缓存丢失，已从数据库重建", runWorkId);
        }
        return context;
    }

    /**
     * 重建运行作业上下文
     *
     * @param jobRunWorkPo 运行作业
     */
    private RunWorkExecuteContext rebuildRunWorkExecuteContext(JobWorkRunPo jobRunWorkPo) {
        if (jobRunWorkPo == null || StrUtil.isBlank(jobRunWorkPo.getWorkId())) {
            return null;
        }

        JobInfoPo jobInfo = JobAdminConfig.getAdminConfig().getJobInfoMapper().selectOne(Wrappers.lambdaQuery(JobInfoPo.class)
                .eq(JobInfoPo::getWorkId, jobRunWorkPo.getWorkId())
                .orderByDesc(JobInfoPo::getUpdateTime)
                .last("limit 1"));
        if (jobInfo == null) {
            log.warn("workId:{} 未找到绑定任务，无法重建运行上下文", jobRunWorkPo.getWorkId());
            return null;
        }

        JobLogPo jobLog = JobAdminConfig.getAdminConfig().getJobLogMapper().selectOne(Wrappers.lambdaQuery(JobLogPo.class)
                .eq(JobLogPo::getJobId, jobInfo.getId())
                .orderByDesc(JobLogPo::getTriggerTime)
                .last("limit 1"));

        TriggerParam triggerParam = buildTriggerParam(jobInfo, jobLog);
        String address = jobLog == null ? null : jobLog.getExecutorAddress();
        return new RunWorkExecuteContext()
                .setRunWorkId(jobRunWorkPo.getRunWorkId())
                .setAddress(address)
                .setTriggerParam(triggerParam)
                .setUpdateTime(System.currentTimeMillis());
    }

    /**
     * 构建触发参数
     */
    private TriggerParam buildTriggerParam(JobInfoPo jobInfo, JobLogPo jobLog) {
        TriggerParam triggerParam = new TriggerParam();
        triggerParam.setJobId(jobInfo.getId());
        triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
        triggerParam.setExecutorParams(jobInfo.getExecutorParam());
        triggerParam.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        triggerParam.setExecutorTimeout(jobInfo.getExecutorTimeout() == null ? 0 : jobInfo.getExecutorTimeout());
        triggerParam.setGlueType(jobInfo.getGlueType());
        triggerParam.setGlueSource(jobInfo.getGlueSource());
        triggerParam.setGlueUpdatetime(jobInfo.getGlueUpdatetime() == null ? 0 : jobInfo.getGlueUpdatetime().getTime());
        triggerParam.setWorkId(jobInfo.getWorkId());

        if (jobLog != null) {
            triggerParam.setLogId(jobLog.getId());
            Date triggerTime = jobLog.getTriggerTime();
            triggerParam.setLogDateTime(triggerTime == null ? System.currentTimeMillis() : triggerTime.getTime());
            int[] sharding = parseSharding(jobLog.getExecutorShardingParam());
            triggerParam.setBroadcastIndex(sharding[0]);
            triggerParam.setBroadcastTotal(sharding[1]);
        } else {
            triggerParam.setLogDateTime(System.currentTimeMillis());
            triggerParam.setBroadcastIndex(0);
            triggerParam.setBroadcastTotal(1);
        }
        return triggerParam;
    }

    /**
     * 解析分片参数
     */
    private String resolveExecutorAddress(RunWorkExecuteContext context, TriggerParam triggerParam) {
        if (StrUtil.isNotBlank(context.getAddress()) && isExecutorAlive(context.getAddress())) {
            return context.getAddress();
        }

        JobInfoPo jobInfo = JobAdminConfig.getAdminConfig().getJobInfoMapper().selectById(triggerParam.getJobId());
        if (jobInfo == null) {
            return context.getAddress();
        }
        JobGroupPo group = JobAdminConfig.getAdminConfig().getJobGroupMapper().selectById(jobInfo.getJobGroup());
        if (group == null || CollUtil.isEmpty(group.getRegistryList())) {
            return context.getAddress();
        }

        ExecutorRouteStrategyEnum executorRouteStrategyEnum = ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), ExecutorRouteStrategyEnum.FIRST);
        if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == executorRouteStrategyEnum) {
            int addressIndex = Math.min(triggerParam.getBroadcastIndex(), group.getRegistryList().size() - 1);
            String routedAddress = group.getRegistryList().get(Math.max(addressIndex, 0));
            return isExecutorAlive(routedAddress) ? routedAddress : findAliveExecutorAddress(group.getRegistryList());
        }

        ReturnT<String> routeResult = executorRouteStrategyEnum.getRouter().route(triggerParam, group.getRegistryList());
        if (routeResult != null && routeResult.getCode() == HandleCodeConstant.HANDLE_CODE_SUCCESS) {
            String routedAddress = routeResult.getContent();
            return isExecutorAlive(routedAddress) ? routedAddress : findAliveExecutorAddress(group.getRegistryList());
        }
        return findAliveExecutorAddress(group.getRegistryList());
    }

    /**
     * 寻找存活的节点
     */
    private String findAliveExecutorAddress(List<String> addressList) {
        if (CollUtil.isEmpty(addressList)) {
            return null;
        }
        for (String address : addressList) {
            if (isExecutorAlive(address)) {
                return address;
            }
        }
        return null;
    }

    /**
     * 判断节点是否存活
     */
    private boolean isExecutorAlive(String address) {
        ReturnT<String> beatResult = ExecutorBizProxy.beat(address);
        return beatResult != null && beatResult.getCode() == HandleCodeConstant.HANDLE_CODE_SUCCESS;
    }

    /**
     * 解析分片参数
     */
    private int[] parseSharding(String shardingParam) {
        if (StrUtil.isBlank(shardingParam)) {
            return new int[]{0, 1};
        }
        String[] shardingArr = shardingParam.split("/");
        if (shardingArr.length != 2) {
            return new int[]{0, 1};
        }
        try {
            return new int[]{Integer.parseInt(shardingArr[0]), Integer.parseInt(shardingArr[1])};
        } catch (NumberFormatException e) {
            return new int[]{0, 1};
        }
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
