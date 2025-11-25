package com.nbatch.job.executor.core.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.nbatch.job.core.executor.impl.BatchJobSpringExecutor;
import com.nbatch.job.core.util.SpringUtil;
import com.nbatch.job.handler.handler.JobHandlerAdapter;
import com.nbatch.job.handler.handler.JobHandlerHolder;
import com.nbatch.job.handler.handler.impl.DbToFileHandler;
import com.nbatch.job.handler.handler.impl.FileToDbHandler;
import com.nbatch.job.handler.helper.DialectHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import static com.nbatch.job.handler.enums.NodeTypeEnum.NODE_TYPE_DB_TO_FILE;
import static com.nbatch.job.handler.enums.NodeTypeEnum.NODE_TYPE_FILE_TO_DB;

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

    @Value("${nbatch.job.executor.logretentiondays}")
    private int logRetentionDays;

    @Resource
    private DynamicRoutingDataSource dataSource;


    @Bean
    public BatchJobSpringExecutor xxlJobExecutor() {
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
        jobSpringExecutor.setLogRetentionDays(logRetentionDays);

        return jobSpringExecutor;
    }

    @Bean
    public SpringUtil springUtil() {
        return new SpringUtil();
    }

    @Bean("dialectHelper")
    public DialectHelper dialectHelper() {
        return new DialectHelper(dataSource);
    }
    
    @Bean("jobHandlerAdapterMap")
    public Map<String, JobHandlerAdapter> jobHandlerAdapterMap(DialectHelper dialectHelper) {
        Map<String, JobHandlerAdapter> jobHandlerAdapterMap = new HashMap<>();
        jobHandlerAdapterMap.put(NODE_TYPE_FILE_TO_DB.getCode(), new FileToDbHandler(dialectHelper));
        jobHandlerAdapterMap.put(NODE_TYPE_DB_TO_FILE.getCode(), new DbToFileHandler(dialectHelper));
        return jobHandlerAdapterMap;
    }

    @Bean(name = "jobHandlerHolder")
    public JobHandlerHolder jobHandlerHolder(Map<String, JobHandlerAdapter> jobHandlerAdapterMap) {
        return new JobHandlerHolder(jobHandlerAdapterMap);
    }
    


}