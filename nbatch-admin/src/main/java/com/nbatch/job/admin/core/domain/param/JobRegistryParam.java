package com.nbatch.job.admin.core.domain.param;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 任务注册表实体类
 */
@Data
@Accessors(chain = true)
public class JobRegistryParam {

    /**
     * 主键ID
     */
    private String id;

    /**
     * 注册组
     */
    private String registryGroup;

    /**
     * 注册键
     */
    private String registryKey;

    /**
     * 注册值
     */
    private String registryValue;

    /**
     * 更新时间
     */
    private Date updateTime;

}
