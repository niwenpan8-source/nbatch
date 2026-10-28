package com.nbatch.job.admin.core.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 作业运行节点日志
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("nbatch_work_run_node_log")
public class JobWorkRunNodeLogPo extends Model<JobWorkRunNodeLogPo> {

    /**
     * 节点日志id
     */
    @TableId
    private String nodeLogId;

    /**
     * 作业id
     */
    private String workId;

    /**
     * 作业节点id
     */
    private String nodeId;

    /**
     * 翻牌日期
     */
    private Date turnDate;

    /**
     * 执行状态
     */
    private Integer handleCode;

    /**
     * 执行信息
     */
    private String handleMsg;

    /**
     * 执行器地址
     */
    private String executorAddress;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 实际开始执行时间
     */
    private LocalDateTime executeStartTime;

    /**
     * 实际结束执行时间
     */
    private LocalDateTime executeEndTime;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;

    /**
     * 回调时间
     */
    private LocalDateTime callBackTime;

}
