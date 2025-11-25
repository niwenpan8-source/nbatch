package com.nbatch.job.core.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Map;

/**
 * @description:
 * @author: Mr.ni
 * @date: 2025/11/21
 */
public class SpringUtil implements ApplicationContextAware {

    /**
     * 上下文对象实例
     */
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringUtil.applicationContext = applicationContext;
    }

    /**
     * 获取对象
     *
     * @param name bean名称
     * @return Object 一个以所给名字注册的bean的实例
     */
    public static Object getBean(String name) throws BeansException {
        return applicationContext.getBean(name);
    }

    /**
     * 获取对象
     *
     * @param clazz class
     * @return Object 一个以所给名字注册的bean的实例
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) throws BeansException {
        return applicationContext.getBeansOfType(clazz);
    }
}
