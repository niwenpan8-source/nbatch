package com.nbatch.job.admin.core.domain.param;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @description: 任务执行器分组实体类
 * @author: Mr.ni
 * @date: 2025/11/6
 */
@Data
@Accessors(chain = true)
public class JobGroupParam {

    /**
     * 主键ID
     */
    private String id;

    /**
     * 执行器AppName
     */
    private String appName;

    /**
     * 执行器名称
     */
    private String title;

    /**
     * 执行器地址类型：0=自动注册、1=手动录入
     */
    private Integer addressType;

    /**
     * 执行器地址列表，多地址逗号分隔
     */
    private String addressList;

    /**
     * 更新时间
     */
    private Date updateTime;

}
