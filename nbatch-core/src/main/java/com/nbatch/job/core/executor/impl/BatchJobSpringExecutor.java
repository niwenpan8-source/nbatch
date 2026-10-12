package com.nbatch.job.core.executor.impl;

import com.nbatch.job.core.executor.BatchJobExecutor;
import com.nbatch.job.core.handler.annotation.BatchJob;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Map;


/**
 * Spring 作业执行器
 *
 * @author Mr.ni
 */
@Slf4j
public class BatchJobSpringExecutor extends BatchJobExecutor implements ApplicationContextAware, SmartInitializingSingleton, DisposableBean {

    // 启动
    @Override
    public void afterSingletonsInstantiated() {

        // 初始化作业处理器仓库
        /*initJobHandlerRepository(applicationContext);*/

        // 初始化方法作业处理器仓库
        initJobHandlerMethodRepository(applicationContext);

        // 启动基础执行器
        try {
            super.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 销毁
    @Override
    public void destroy() {
        super.destroy();
    }
    

    private void initJobHandlerMethodRepository(ApplicationContext applicationContext) {
        if (applicationContext == null) {
            return;
        }
            // 从方法初始化作业处理器
        String[] beanDefinitionNames = applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanDefinitionName : beanDefinitionNames) {

            // 获取 Bean
            Object bean;
            Lazy onBean = applicationContext.findAnnotationOnBean(beanDefinitionName, Lazy.class);
            if (onBean!=null){
                log.debug("扫描作业注解时跳过 @Lazy Bean：{}", beanDefinitionName);
                continue;
            }else {
                bean = applicationContext.getBean(beanDefinitionName);
            }

            // 筛选方法
            // 参考 org.springframework.context.event.EventListenerMethodProcessor.processBean
            Map<Method, BatchJob> annotatedMethods = null;   
            try {
                annotatedMethods = MethodIntrospector.selectMethods(bean.getClass(),
                        (MethodIntrospector.MetadataLookup<BatchJob>) method -> AnnotatedElementUtils.findMergedAnnotation(method, BatchJob.class));
            } catch (Throwable ex) {
                log.error("解析 Bean[{}] 的作业方法处理器时发生错误。", beanDefinitionName, ex);
            }
            if (annotatedMethods==null || annotatedMethods.isEmpty()) {
                continue;
            }

            // 创建并注册方法作业处理器
            for (Map.Entry<Method, BatchJob> methodXxlJobEntry : annotatedMethods.entrySet()) {
                Method executeMethod = methodXxlJobEntry.getKey();
                BatchJob batchJob = methodXxlJobEntry.getValue();
                // 注册处理器
                registerJobHandler(batchJob, bean, executeMethod);
            }

        }
    }

    // ---------------------- applicationContext ----------------------
    @Getter
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        BatchJobSpringExecutor.applicationContext = applicationContext;
    }

}
