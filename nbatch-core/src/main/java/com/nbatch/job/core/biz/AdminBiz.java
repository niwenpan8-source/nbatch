package com.nbatch.job.core.biz;

import com.nbatch.job.core.biz.model.RegistryParam;
import com.nbatch.job.core.biz.model.ReturnT;

/**
 * 服务端处理接口
 */
public interface AdminBiz {


    // ---------------------- registry ----------------------

    /**
     * 注册执行器
     *
     * @param registryParam registryParam
     */
    ReturnT<String> registry(RegistryParam registryParam);

    /**
     * 注销执行器
     *
     * @param registryParam registryParam
     */
    ReturnT<String> registryRemove(RegistryParam registryParam);


    // ---------------------- biz (custome) ----------------------
    // 作业组、作业等管理接口

}
