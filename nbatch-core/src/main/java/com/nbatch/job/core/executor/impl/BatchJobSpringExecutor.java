package com.nbatch.job.core.executor.impl;

import com.nbatch.job.core.executor.BatchJobExecutor;
import com.nbatch.job.core.glue.GlueFactory;
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
 * job executor (for spring)
 *
 * @author Mr.ni
 */
@Slf4j
public class BatchJobSpringExecutor extends BatchJobExecutor implements ApplicationContextAware, SmartInitializingSingleton, DisposableBean {

    // start
    @Override
    public void afterSingletonsInstantiated() {

        // init JobHandler Repository
        /*initJobHandlerRepository(applicationContext);*/

        // init JobHandler Repository (for method)
        initJobHandlerMethodRepository(applicationContext);

        // refresh GlueFactory
        GlueFactory.refreshInstance(1);

        // super start
        try {
            super.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // destroy
    @Override
    public void destroy() {
        super.destroy();
    }
    

    private void initJobHandlerMethodRepository(ApplicationContext applicationContext) {
        if (applicationContext == null) {
            return;
        }
        // init job handler from method
        String[] beanDefinitionNames = applicationContext.getBeanNamesForType(Object.class, false, true);
        for (String beanDefinitionName : beanDefinitionNames) {

            // get bean
            Object bean;
            Lazy onBean = applicationContext.findAnnotationOnBean(beanDefinitionName, Lazy.class);
            if (onBean!=null){
                log.debug("job annotation scan, skip @Lazy Bean:{}", beanDefinitionName);
                continue;
            }else {
                bean = applicationContext.getBean(beanDefinitionName);
            }

            // filter method
            // referred to ：org.springframework.context.event.EventListenerMethodProcessor.processBean
            Map<Method, BatchJob> annotatedMethods = null;   
            try {
                annotatedMethods = MethodIntrospector.selectMethods(bean.getClass(),
                        (MethodIntrospector.MetadataLookup<BatchJob>) method -> AnnotatedElementUtils.findMergedAnnotation(method, BatchJob.class));
            } catch (Throwable ex) {
                log.error("job method-jobhandler resolve error for bean[{}].", beanDefinitionName, ex);
            }
            if (annotatedMethods==null || annotatedMethods.isEmpty()) {
                continue;
            }

            // generate and regist method job handler
            for (Map.Entry<Method, BatchJob> methodXxlJobEntry : annotatedMethods.entrySet()) {
                Method executeMethod = methodXxlJobEntry.getKey();
                BatchJob batchJob = methodXxlJobEntry.getValue();
                // regist
                registJobHandler(batchJob, bean, executeMethod);
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
