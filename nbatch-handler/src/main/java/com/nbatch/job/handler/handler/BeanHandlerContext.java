package com.nbatch.job.handler.handler;

import com.nbatch.job.core.biz.model.ExecuteNodeParam;

/**
 * @description: BeanHandler 上下文
 * @author: Mr.ni
 * @date: 2025/12/19
 */
public class BeanHandlerContext {

    private static final ThreadLocal<ExecuteNodeParam> BEAN_THREAD_LOCAL = new InheritableThreadLocal<>();

    public static void setBeanThreadLocal(ExecuteNodeParam param) {
        BEAN_THREAD_LOCAL.set(param);
    }

    public static ExecuteNodeParam getBeanThreadLocal() {
        return BEAN_THREAD_LOCAL.get();
    }

    public static void removeBeanThreadLocal() {
        BEAN_THREAD_LOCAL.remove();
    }




}
