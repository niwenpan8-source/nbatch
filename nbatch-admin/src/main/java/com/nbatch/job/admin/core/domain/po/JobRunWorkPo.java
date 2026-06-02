package com.nbatch.job.admin.core.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.EqualsAndHashCode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @description: 运行作业表
 * @author: Mr.ni
 * @date: 2025-12-01
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("nbatch_job_run_work")
public class JobRunWorkPo extends Model<JobRunWorkPo> {

	/**
	 * 运行作业id
	 */
	@TableId(type = IdType.ASSIGN_ID)
	private String runWorkId;

	/**
	 * 作业id
	 */
	private String workId;

	/**
	 * 运行状态：0=待执行、1=进行中、2=执行完毕
	 */
	private Integer runWorkStatus;

	/**
	 * 翻牌日期
	 */
	private Date turnDate;

	/**
	 * 创建时间
	 */
	private Date createTime;

	/**
	 * 结束时间
	 */
	private Date endTime;

	/**
	 * 执行失败时的错误信息
	 */
	private String errorMsg;

	/**
	 * 触发类型：manual-手动, cron-定时, api-接口
	 */
	private String triggerType;

	/**
	 * 节点总数
	 */
	private Integer totalNodeCount;

	/**
	 * 已完成节点数
	 */
	private Integer completedNodeCount;

	/**
	 * 失败节点数
	 */
	private Integer failedNodeCount;

	/**
	 * 流程上下文变量（JSON格式），用于节点间传参
	 */
	private String contextJson;

	/**
	 * 作业类型：0=普通作业、1=定时作业
	 */
	@TableField(exist = false)
	private Integer workType;

}
