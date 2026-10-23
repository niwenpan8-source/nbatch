package com.nbatch.job.admin.core.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 作业流程实时事件调度配置。
 */
@Configuration
public class JobWorkFlowConfig {

    @Bean(name = "jobWorkFlowEventTaskScheduler")
    public ThreadPoolTaskScheduler jobWorkFlowEventTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("nbatch-flow-event-");
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }
}
