package com.nbatch.job.admin.core.scheduler;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.admin.core.thread.JobRegistryHelper;
import com.nbatch.job.admin.core.thread.JobRunNodeLogPullHelper;
import com.nbatch.job.admin.core.thread.JobRunNodeLogDetailPullHelper;
import com.nbatch.job.admin.core.thread.JobScheduleHelper;
import com.nbatch.job.admin.core.thread.JobTriggerPoolHelper;
import com.nbatch.job.admin.core.thread.JobWorkMonitorHelper;
import com.nbatch.job.admin.core.thread.JobWorkRunNodeHelper;
import com.nbatch.job.core.biz.ExecutorBiz;
import com.nbatch.job.core.biz.client.ExecutorBizClient;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 任务调度初始化
 */
@Slf4j
public class JobScheduler {

    public void init() throws Exception {
        // 启动管理端触发线程池
        JobTriggerPoolHelper.toStart();

        // 启动管理端注册监控
        JobRegistryHelper.getInstance().start();

        // 启动作业调度
        JobScheduleHelper.getInstance().start();

        // 启动作业监控
        JobWorkMonitorHelper.getInstance().start();

        // 启动运行节点详细日志拉取
        JobRunNodeLogDetailPullHelper.getInstance().start();

        // 启动运行节点事件日志拉取
        JobRunNodeLogPullHelper.getInstance().start();

        // 启动运行节点执行
        JobWorkRunNodeHelper.getInstance().start();

        log.info(">>>>>>>>> 作业管理端初始化成功。");
    }

    
    public void destroy() {

        // 停止作业调度
        JobScheduleHelper.getInstance().toStop();

        // 停止管理端注册监控
        JobRegistryHelper.getInstance().toStop();

        // 停止管理端触发线程池
        JobTriggerPoolHelper.toStop();

        // 停止作业监控
        JobWorkMonitorHelper.getInstance().toStop();

        // 停止运行节点详细日志拉取
        JobRunNodeLogDetailPullHelper.getInstance().toStop();

        // 停止运行节点事件日志拉取
        JobRunNodeLogPullHelper.getInstance().toStop();

        // 停止运行节点执行
        JobWorkRunNodeHelper.getInstance().toStop();

    }

    // ---------------------- executor-client ----------------------
    private static final ConcurrentMap<String, ExecutorBiz> EXECUTOR_BIZ_REPOSITORY = new ConcurrentHashMap<>();

    public static ExecutorBiz getExecutorBiz(String address) {
        // 校验执行器地址
        if (StrUtil.isBlank(address)) {
            return null;
        }

        // 加载缓存
        address = address.trim();
        ExecutorBiz executorBiz = EXECUTOR_BIZ_REPOSITORY.get(address);
        if (executorBiz != null) {
            return executorBiz;
        }

        // 设置缓存
        executorBiz = new ExecutorBizClient(address,
                JobAdminConfig.getAdminConfig().getAccessToken(),
                JobAdminConfig.getAdminConfig().getTimeout());

        EXECUTOR_BIZ_REPOSITORY.put(address, executorBiz);
        return executorBiz;
    }

}
