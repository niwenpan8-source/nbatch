package com.nbatch.job.handler.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.nbatch.job.core.util.SpringUtil;
import com.nbatch.job.handler.constant.JobHandlerPropertiesConstant;
import com.nbatch.job.handler.handler.JobNodeHandlerAdapter;
import com.nbatch.job.handler.handler.JobHandlerHolder;
import com.nbatch.job.handler.handler.impl.BeanHandler;
import com.nbatch.job.handler.handler.impl.DbToFileHandler;
import com.nbatch.job.handler.handler.impl.ExecuteSqlHandler;
import com.nbatch.job.handler.handler.impl.FileToDbHandler;
import com.nbatch.job.handler.handler.impl.ScriptHandler;
import com.nbatch.job.handler.handler.impl.StoreProcedureHandler;
import com.nbatch.job.handler.helper.DialectHelper;
import com.nbatch.job.handler.utils.BatchThreadPoolUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import static com.nbatch.job.handler.enums.NodeTypeEnum.NODE_TYPE_BEAN;
import static com.nbatch.job.handler.enums.NodeTypeEnum.NODE_TYPE_DB_TO_FILE;
import static com.nbatch.job.handler.enums.NodeTypeEnum.NODE_TYPE_EXECUTE_SQL;
import static com.nbatch.job.handler.enums.NodeTypeEnum.NODE_TYPE_FILE_TO_DB;
import static com.nbatch.job.handler.enums.NodeTypeEnum.NODE_TYPE_SCRIPT;
import static com.nbatch.job.handler.enums.NodeTypeEnum.NODE_TYPE_STORE_PROCEDURE;

/**
 * @description: 配置类
 * @author: Mr.ni
 * @date: 2025/11/25
 */
@Slf4j
@Configuration
public class NbatchHandlerConfig {

    @Resource
    private DynamicRoutingDataSource dataSource;

    /**
     * spring工具类
     */
    @Bean
    public SpringUtil springUtil() {
        return new SpringUtil();
    }

    /**
     * 数据库操作工具类
     */
    @Bean("dialectHelper")
    public DialectHelper dialectHelper() {
        return new DialectHelper(dataSource);
    }

    @Bean("handlerPropertiesConstant")
    public JobHandlerPropertiesConstant handlerPropertiesConstant() {
        return new JobHandlerPropertiesConstant();
    }

    /**
     * 任务处理适配器
     */
    @Bean("jobHandlerAdapterMap")
    public Map<String, JobNodeHandlerAdapter> jobHandlerAdapterMap(DialectHelper dialectHelper,
                                                                   JobHandlerPropertiesConstant handlerPropertiesConstant) {
        Map<String, JobNodeHandlerAdapter> jobHandlerAdapterMap = new HashMap<>();
        FileToDbHandler fileToDbHandler = new FileToDbHandler(dialectHelper, handlerPropertiesConstant);
        DbToFileHandler dbToFileHandler = new DbToFileHandler(dialectHelper, handlerPropertiesConstant);
        ExecuteSqlHandler executeSqlHandler = new ExecuteSqlHandler(dialectHelper);
        StoreProcedureHandler storeProcedureHandler = new StoreProcedureHandler(dialectHelper);
        ScriptHandler scriptHandler = new ScriptHandler();
        BeanHandler beanHandler = new BeanHandler();
        jobHandlerAdapterMap.put(NODE_TYPE_FILE_TO_DB.getCode(), fileToDbHandler);
        jobHandlerAdapterMap.put(NODE_TYPE_DB_TO_FILE.getCode(), dbToFileHandler);
        jobHandlerAdapterMap.put(NODE_TYPE_EXECUTE_SQL.getCode(), executeSqlHandler);
        jobHandlerAdapterMap.put(NODE_TYPE_STORE_PROCEDURE.getCode(), storeProcedureHandler);
        jobHandlerAdapterMap.put(NODE_TYPE_SCRIPT.getCode(), scriptHandler);
        jobHandlerAdapterMap.put(NODE_TYPE_BEAN.getCode(), beanHandler);
        return jobHandlerAdapterMap;
    }

    /**
     * 任务处理持有者
     */
    @Bean(name = "jobHandlerHolder")
    public JobHandlerHolder jobHandlerHolder(Map<String, JobNodeHandlerAdapter> jobHandlerAdapterMap) {
        return new JobHandlerHolder(jobHandlerAdapterMap);
    }



    /**
     * 销毁线程池
     */
    @PreDestroy
    public void destroy() {
        System.out.println("destroy thread pool");
        BatchThreadPoolUtil.shutdownAllThreadPool();
    }
}
