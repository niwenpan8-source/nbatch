package com.nbatch.job.admin.core.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @description: 任务注册表实体类
 * @author: Mr.ni
 * @date: 2025/11/6
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("nbatch_job_registry")
public class JobRegistryPo extends Model<JobRegistryPo> {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
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
