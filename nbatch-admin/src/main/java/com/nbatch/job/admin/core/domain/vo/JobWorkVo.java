package com.nbatch.job.admin.core.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @description: 作业表
 * @author: Mr.ni
 * @date: 2025-11-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobWorkVo {

    /**
     * 作业id
     */
    private String workId;

    /**
     * 作业名
     */
    private String workName;

    /**
     * 作业描述
     */
    private String workDesc;

    /**
     * 作业类型 => 0:翻牌类型，1：顺序类型
     */
    private Integer workType;

    private String workTypeName;

    /**
     * 作业状态：0=停用、1=启用
     */
    private Integer workStatus;

    private String workStatusName;

    private String cronExpression;

    private Integer timeout;

    private String notifyEmail;

    private Integer version;

    /**
     * 初始化翻牌日期
     */
    private Date initTurnDate;

    private String initTurnDateText;

    private Date createTime;

    private Date updateTime;

    /**
     * 最近一次运行作业的翻牌日期
     */
    private String turnDate;

    private String runWorkId;

    private String runWorkCreateTime;

    /**
     * 最近一次运行作业状态
     */
    private Integer runWorkStatus;

    private String runWorkStatusName;

}
