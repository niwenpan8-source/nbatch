package com.nbatch.job.admin.core.domain.param;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @description: 任务注册表实体类
 * @author: Mr.ni
 * @date: 2025/11/6
 */
@Data
@Accessors(chain = true)
public class JobRegistryParam {

    /**
     * 主键ID
     */
    @TableId
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
