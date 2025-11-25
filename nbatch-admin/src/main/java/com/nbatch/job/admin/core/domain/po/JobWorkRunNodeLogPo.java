package com.nbatch.job.admin.core.domain.po;

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
 * @description: 作业运行节点日志
 * @author: Mr.ni
 * @date: 2025-11-25
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("nbatch_job_work_run_node_log")
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
	 * 执行状态
	 */
	private Integer handleCode;

	/**
	 * 执行信息
	 */
	private String handleMsg;

	/**
	 * 创建时间
	 */
	private Date createTime;

	/**
	 * 回调时间
	 */
	private Date callBackTime;

}
