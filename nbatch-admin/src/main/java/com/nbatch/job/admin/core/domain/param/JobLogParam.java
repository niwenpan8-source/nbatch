package com.nbatch.job.admin.core.domain.param;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @description: 任务日志实体类
 * @author: Mr.ni
 * @date: 2025/11/6
 */
@Data
@Accessors(chain = true)
public class JobLogParam {

    /**
     * 主键ID
     */
    private String id;

    /**
     * 执行器主键ID
     */
    private String jobGroup;

    /**
     * 任务，主键ID
     */
    private String jobId;

    /**
     * 执行器地址，本次执行的地址
     */
    private String executorAddress;

    /**
     * 执行器任务handler
     */
    private String executorHandler;

    /**
     * 执行器任务参数
     */
    private String executorParam;

    /**
     * 执行器任务分片参数，格式如 1/2
     */
    private String executorShardingParam;

    /**
     * 失败重试次数
     */
    private Integer executorFailRetryCount;

    /**
     * 调度-时间
     */
    private Date triggerTime;

    /**
     * 调度-结果
     */
    private Integer triggerCode;

    /**
     * 调度-日志
     */
    private String triggerMsg;

    /**
     * 执行-时间
     */
    private Date handleTime;

    /**
     * 执行-状态
     */
    private Integer handleCode;

    /**
     * 执行-日志
     */
    private String handleMsg;

    /**
     * 告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败
     */
    private Integer alarmStatus;

}
