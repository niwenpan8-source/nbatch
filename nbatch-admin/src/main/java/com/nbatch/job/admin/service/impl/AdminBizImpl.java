package com.nbatch.job.admin.service.impl;

import com.nbatch.job.admin.core.thread.JobRegistryHelper;
import com.nbatch.job.core.biz.AdminBiz;
import com.nbatch.job.core.biz.model.RegistryParam;
import com.nbatch.job.core.biz.model.ReturnT;
import org.springframework.stereotype.Service;


/**
 * 管理端接口实现
 *
 * @author Mr.ni
 */
@Service
public class AdminBizImpl implements AdminBiz {


    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registry(registryParam);
    }

    @Override
    public ReturnT<String> registryRemove(RegistryParam registryParam) {
        return JobRegistryHelper.getInstance().registryRemove(registryParam);
    }

}
