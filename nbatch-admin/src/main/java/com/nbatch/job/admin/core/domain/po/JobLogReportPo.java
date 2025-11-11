package com.nbatch.job.admin.core.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @description: 任务日志统计报表实体类
 * @author: Mr.ni
 * @date: 2025/11/6
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("nbatch_job_log_report")
public class JobLogReportPo extends Model<JobLogReportPo> {

    /**
     * 主键ID
     */
    @TableId
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
