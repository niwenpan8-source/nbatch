package com.nbatch.job.core.handler.annotation;

import java.lang.annotation.*;

/**
 * 作业处理方法注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface BatchJob {

    /**
     * 作业处理器名称
     */
    String value();

    /**
     * 初始化处理器，在作业线程初始化时调用
     */
    String init() default "";

    /**
     * 销毁处理器，在作业线程销毁时调用
     */
    String destroy() default "";

}
