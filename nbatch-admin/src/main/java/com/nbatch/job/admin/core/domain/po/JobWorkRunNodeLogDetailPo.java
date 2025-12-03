package com.nbatch.job.admin.core.domain.po;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.EqualsAndHashCode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import java.util.Date;

/**
 * @description: 作业运行节点详细日志
 * @author: Mr.ni
 * @date: 2025-12-03
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("nbatch_job_work_run_node_log_detail")
public class JobWorkRunNodeLogDetailPo extends Model<JobWorkRunNodeLogDetailPo> {

	/**
	 * 节点日志id
	 */
	@TableId
	private String detailLogId;

	/**
	 * 作业id
	 */
	private String workId;

	/**
	 * 运行作业id
	 */
	private String runWorkId;

	/**
	 * 作业节点id
	 */
	private String nodeId;

	/**
	 * 运行作业节点id
	 */
	private String runNodeId;

	/**
	 * 执行信息
	 */
	private String handleMsg;

	/**
	 * 执行-时间
	 */
	private Date executeTime;

	/**
	 * 执行-时间
	 */
	private Date callBackTime;

}
