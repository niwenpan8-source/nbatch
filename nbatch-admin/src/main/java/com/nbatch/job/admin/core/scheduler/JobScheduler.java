package com.nbatch.job.admin.core.scheduler;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.admin.core.thread.JobCompleteHelper;
import com.nbatch.job.admin.core.thread.JobFailMonitorHelper;
import com.nbatch.job.admin.core.thread.JobLogReportHelper;
import com.nbatch.job.admin.core.thread.JobRegistryHelper;
import com.nbatch.job.admin.core.thread.JobRunNodeLogDetailHelper;
import com.nbatch.job.admin.core.thread.JobScheduleHelper;
import com.nbatch.job.admin.core.thread.JobTriggerPoolHelper;
import com.nbatch.job.admin.core.thread.JobWorkMonitorHelper;
import com.nbatch.job.admin.core.thread.JobWorkRunNodeHelper;
import com.nbatch.job.admin.core.util.I18nUtil;
import com.nbatch.job.core.biz.ExecutorBiz;
import com.nbatch.job.core.biz.client.ExecutorBizClient;
import com.nbatch.job.core.enums.ExecutorBlockStrategyEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 任务调度初始化
 * @author Mr.ni
 */
@Slf4j
public class JobScheduler {

    public void init() throws Exception {
        // init i18n
        initI18n();

        // admin trigger pool start
        JobTriggerPoolHelper.toStart();

        // admin registry monitor run
        JobRegistryHelper.getInstance().start();

        // admin fail-monitor run
        JobFailMonitorHelper.getInstance().start();

        // admin lose-monitor run ( depend on JobTriggerPoolHelper )
        JobCompleteHelper.getInstance().start();

        // admin log report start
        JobLogReportHelper.getInstance().start();

        // start-schedule  ( depend on JobTriggerPoolHelper )
        JobScheduleHelper.getInstance().start();

        // start-work-monitor
        JobWorkMonitorHelper.getInstance().start();

        // start-run node log detail-monitor
        JobRunNodeLogDetailHelper.getInstance().start();

        // start-work-run node execute
        JobWorkRunNodeHelper.getInstance().start();

        log.info(">>>>>>>>> init job admin success.");
    }

    
    public void destroy() {

        // stop-schedule
        JobScheduleHelper.getInstance().toStop();

        // admin log report stop
        JobLogReportHelper.getInstance().toStop();

        // admin lose-monitor stop
        JobCompleteHelper.getInstance().toStop();

        // admin fail-monitor stop
        JobFailMonitorHelper.getInstance().toStop();

        // admin registry stop
        JobRegistryHelper.getInstance().toStop();

        // admin trigger pool stop
        JobTriggerPoolHelper.toStop();

        // admin work monitor stop
        JobWorkMonitorHelper.getInstance().toStop();

        // admin run node log detail stop
        JobRunNodeLogDetailHelper.getInstance().toStop();

        // admin work run node stop
        JobWorkRunNodeHelper.getInstance().toStop();

    }

    // ---------------------- I18n ----------------------

    private void initI18n(){
        for (ExecutorBlockStrategyEnum item : ExecutorBlockStrategyEnum.values()) {
            item.setTitle(I18nUtil.getString("jobconf_block_".concat(item.name())));
        }
    }

    // ---------------------- executor-client ----------------------
    private static final ConcurrentMap<String, ExecutorBiz> EXECUTOR_BIZ_REPOSITORY = new ConcurrentHashMap<>();

    public static ExecutorBiz getExecutorBiz(String address) {
        // valid
        if (StrUtil.isBlank(address)) {
            return null;
        }

        // load-cache
        address = address.trim();
        ExecutorBiz executorBiz = EXECUTOR_BIZ_REPOSITORY.get(address);
        if (executorBiz != null) {
            return executorBiz;
        }

        // set-cache
        executorBiz = new ExecutorBizClient(address,
                JobAdminConfig.getAdminConfig().getAccessToken(),
                JobAdminConfig.getAdminConfig().getTimeout());

        EXECUTOR_BIZ_REPOSITORY.put(address, executorBiz);
        return executorBiz;
    }

}
