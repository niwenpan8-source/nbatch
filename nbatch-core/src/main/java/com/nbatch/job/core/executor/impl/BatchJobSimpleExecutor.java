package com.nbatch.job.core.executor.impl;

import cn.hutool.core.collection.CollUtil;
import com.nbatch.job.core.executor.BatchJobExecutor;
import com.nbatch.job.core.handler.annotation.BatchJob;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


/**
 * job executor (for frameless)
 *
 * @author Mr.ni 2020-11-05
 */
@Slf4j
public class BatchJobSimpleExecutor extends BatchJobExecutor {


    private List<Object> xxlJobBeanList = new ArrayList<>();
    public List<Object> getXxlJobBeanList() {
        return xxlJobBeanList;
    }
    public void setXxlJobBeanList(List<Object> xxlJobBeanList) {
        this.xxlJobBeanList = xxlJobBeanList;
    }


    @Override
    public void start() {

        // init JobHandler Repository (for method)
        initJobHandlerMethodRepository(xxlJobBeanList);

        // super start
        try {
            super.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
    }


    private void initJobHandlerMethodRepository(List<Object> jobBeanList) {
        if (CollUtil.isEmpty(jobBeanList)) {
            return;
        }

        // init job handler from method
        for (Object bean: jobBeanList) {
            // method
            Method[] methods = bean.getClass().getDeclaredMethods();
            if (methods.length == 0) {
                continue;
            }
            for (Method executeMethod : methods) {
                BatchJob xxlJob = executeMethod.getAnnotation(BatchJob.class);
                // registry
                registerJobHandler(xxlJob, bean, executeMethod);
            }

        }

    }

}
