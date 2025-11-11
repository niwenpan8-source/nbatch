package com.nbatch.job.admin.core.domain.param;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @description: 任务日志统计报表实体类
 * @author: Mr.ni
 * @date: 2025/11/6
 */
@Data
@Accessors(chain = true)
public class JobLogReportParam {

    /**
     * 主键ID
     */
    private String id;

    /**
     * 调度-时间
     */
    private Date triggerDay;

    /**
     * 运行中-日志数量
     */
    private Integer runningCount;

    /**
     * 执行成功-日志数量
     */
    private Integer sucCount;

    /**
     * 执行失败-日志数量
     */
    private Integer failCount;

    /**
     * 更新时间
     */
    private Date updateTime;

}
