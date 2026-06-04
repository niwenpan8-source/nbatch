package com.nbatch.job.admin.core.domain.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.EqualsAndHashCode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @description: 作业运行节点
 * @author: Mr.ni
 * @date: 2025-11-20
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("nbatch_job_work_run_node")
public class JobWorkRunNodePo extends Model<JobWorkRunNodePo> {

	/**
	 * 运行节点id
	 */
	@TableId
	private String runNodeId;

	/**
	 * 执行作业id
	 */
	private String runWorkId;

	/**
	 * 作业id
	 */
	private String workId;

	/**
	 * 作业节点id
	 */
	private String nodeId;

	/**
	 * 节点运行状态：0=未运行、1=运行节点
	 */
	private Integer nodeRunStatus;

	/**
	 * 翻牌日期
	 */
	private Date turnDate;

	/**
	 * 创建时间
	 */
	private Date createTime;

	/**
	 * 失败策略：stop-该运行节点停止, skip-跳过继续, retry-重试
	 */
	private String errorStrategy;

	/**
	 * 重试次数，如果归零则不再能够进行重试
	 */
	private Integer retryTimes;

	/**
	 * 运行节点开始时间
	 */
	private LocalDateTime startTime;

	/**
	 * 运行节点结束时间
	 */
	private LocalDateTime endTime;

}
