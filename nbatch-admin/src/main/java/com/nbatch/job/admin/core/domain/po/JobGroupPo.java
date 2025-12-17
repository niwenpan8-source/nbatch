package com.nbatch.job.admin.core.domain.po;

import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @description: 任务执行器分组实体类
 * @author: Mr.ni
 * @date: 2025/11/6
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("nbatch_job_group")
public class JobGroupPo extends Model<JobGroupPo> {

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
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
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String addressList;

    /**
     * 更新时间
     * 忽略空值判断
     */
    private Date updateTime;

    /**
     * 执行器地址列表(系统注册)
     */
    @TableField(exist = false)
    private List<String> registryList;

    public List<String> getRegistryList() {
        if (StrUtil.isNotBlank(addressList)) {
            registryList = new ArrayList<>(Arrays.asList(addressList.split(StrPool.COMMA)));
        }
        return registryList;
    }

}
