package com.nbatch.job.admin.core.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 作业运行节点详细日志
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("nbatch_work_run_node_log_detail")
public class JobWorkRunNodeLogDetailPo extends Model<JobWorkRunNodeLogDetailPo> {

    /**
     * 节点日志id
     */
    @TableId
    private String detailLogId;

    /**
     * 所属单次节点执行日志。
     */
    private String nodeLogId;

    /**
     * 作业标识
     */
    private String workId;

    /**
     * 步骤标识
     */
    private String nodeId;

    /**
     * 节点名称，仅用于日志展示，不持久化。
     */
    @TableField(exist = false)
    private String nodeName;

    /**
     * 翻牌日期
     */
    private Date turnDate;

    /**
     * 执行信息
     */
    private String handleMsg;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    /**
     * 回调时间
     */
    private LocalDateTime executeTime;

    /**
     * 执行-时间
     */
    private LocalDateTime callBackTime;

}
