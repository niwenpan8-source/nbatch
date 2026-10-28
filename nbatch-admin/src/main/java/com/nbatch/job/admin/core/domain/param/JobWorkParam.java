package com.nbatch.job.admin.core.domain.param;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 作业表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobWorkParam {

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

    private String jobGroup;
    private String scheduleType;
    private String scheduleConf;
    private Integer scheduleStatus;
    private String misfireStrategy;
    private Long triggerLastTime;
    private Long triggerNextTime;
    private String cronExpression;

    private Integer timeout;

    private String notifyEmail;

    private Integer version;

    private Date createTime;

    private Date updateTime;

}
