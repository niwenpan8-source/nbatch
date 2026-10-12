package com.nbatch.job.executor.core.config;

import com.nbatch.job.core.executor.impl.BatchJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 作业执行器配置
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

    @Value("${nbatch.job.executor.datapath:}")
    private String dataPath;

    @Bean
    public BatchJobSpringExecutor jobExecutor() {
        log.info(">>>>>>>>>>> 作业配置初始化。");
        BatchJobSpringExecutor jobSpringExecutor = new BatchJobSpringExecutor();
        jobSpringExecutor.setAdminAddresses(adminAddresses);
        jobSpringExecutor.setAppName(appName);
        jobSpringExecutor.setAddress(address);
        jobSpringExecutor.setIp(ip);
        jobSpringExecutor.setPort(port);
        jobSpringExecutor.setAccessToken(accessToken);
        jobSpringExecutor.setTimeout(timeout);
        jobSpringExecutor.setDataPath(dataPath);

        return jobSpringExecutor;
    }


}
