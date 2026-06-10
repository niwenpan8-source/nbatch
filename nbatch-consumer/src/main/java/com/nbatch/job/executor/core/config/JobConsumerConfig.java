package com.nbatch.job.executor.core.config;

import com.nbatch.job.core.executor.impl.BatchJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * job config
 *
 * @author Mr.ni
 */
@Slf4j
@Configuration
public class JobConsumerConfig {

    @Value("${nbatch.job.admin.addresses}")
    private String adminAddresses;

    @Value("${nbatch.job.admin.accessToken}")
    private String accessToken;

    @Value("${nbatch.job.admin.timeout}")
    private int timeout;

    @Value("${nbatch.job.executor.appName}")
    private String appName;

    @Value("${nbatch.job.executor.address}")
    private String address;

    @Value("${nbatch.job.executor.ip}")
    private String ip;

    @Value("${nbatch.job.executor.port}")
    private int port;

    @Value("${nbatch.job.executor.logpath}")
    private String logPath;

    @Value("${nbatch.job.executor.datapath:}")
    private String dataPath;

    @Value("${nbatch.job.executor.logretentiondays}")
    private int logRetentionDays;


    @Bean
    public BatchJobSpringExecutor jobExecutor() {
        log.info(">>>>>>>>>>> job config init.");
        BatchJobSpringExecutor jobSpringExecutor = new BatchJobSpringExecutor();
        jobSpringExecutor.setAdminAddresses(adminAddresses);
        jobSpringExecutor.setAppName(appName);
        jobSpringExecutor.setAddress(address);
        jobSpringExecutor.setIp(ip);
        jobSpringExecutor.setPort(port);
        jobSpringExecutor.setAccessToken(accessToken);
        jobSpringExecutor.setTimeout(timeout);
        jobSpringExecutor.setLogPath(logPath);
        jobSpringExecutor.setDataPath(dataPath);
        jobSpringExecutor.setLogRetentionDays(logRetentionDays);

        return jobSpringExecutor;
    }


}
