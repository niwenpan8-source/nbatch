package com.nbatch.job.executor.core.config;

import com.nbatch.job.core.executor.impl.BatchJobSpringExecutor;
import com.nbatch.job.core.util.SpringUtil;
import com.nbatch.job.handler.handler.JobHandlerHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * job config
 *
 * @author Mr.ni 2017-04-28
 */
@Slf4j
@Configuration
public class JobConsumerConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.admin.accessToken}")
    private String accessToken;

    @Value("${xxl.job.admin.timeout}")
    private int timeout;

    @Value("${xxl.job.executor.appName}")
    private String appName;

    @Value("${xxl.job.executor.address}")
    private String address;

    @Value("${xxl.job.executor.ip}")
    private String ip;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    @Value("${xxl.job.executor.logretentiondays}")
    private int logRetentionDays;


    @Bean
    public BatchJobSpringExecutor xxlJobExecutor() {
        log.info(">>>>>>>>>>> job config init.");
        BatchJobSpringExecutor xxlJobSpringExecutor = new BatchJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppName(appName);
        xxlJobSpringExecutor.setAddress(address);
        xxlJobSpringExecutor.setIp(ip);
        xxlJobSpringExecutor.setPort(port);
        xxlJobSpringExecutor.setAccessToken(accessToken);
        xxlJobSpringExecutor.setTimeout(timeout);
        xxlJobSpringExecutor.setLogPath(logPath);
        xxlJobSpringExecutor.setLogRetentionDays(logRetentionDays);

        return xxlJobSpringExecutor;
    }

    @Bean
    public SpringUtil springUtil() {
        return new SpringUtil();
    }

    @Bean(name = "jobHandlerHolder")
    public JobHandlerHolder jobHandlerHolder() {
        return new JobHandlerHolder();
    }


}