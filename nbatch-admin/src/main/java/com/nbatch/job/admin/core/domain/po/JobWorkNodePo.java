package com.nbatch.job.admin.core.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.EqualsAndHashCode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import java.util.Date;

/**
 * @description: 作业节点表
 * @author: Mr.ni
 * @date: 2025-11-13
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("nbatch_work_node")
public class JobWorkNodePo extends Model<JobWorkNodePo> {

	/**
	 * 作业节点id
	 */
	@TableId(type = IdType.ASSIGN_ID)
	private String nodeId;

	/**
	 * 作业id
	 */
	private String workId;

	/**
	 * 所属流程组，所有节点均归属一个组。
	 */
	private String groupId;

	/**
	 * 节点名称
	 */
	private String nodeName;

	/**
	 * 节点描述
	 */
	private String nodeDesc;

	/**
	 * 节点类型：script（脚本）、store_procedure（存储过程）、execute_sql（执行 SQL）、file_to_db（文件导入数据库）、db_to_file（数据库导出文件）
	 */
	private String nodeType;

	/**
	 * 数据库类型
	 */
	private String dbType;

	/**
	 * 执行内容
	 */
	private String executeContent;

	/**
	 * 执行内容参数
	 */
	private String executeContentParam;

	/**
	 * 执行器
	 */
	private String executeHandler;

	/**
	 * 脚本类型 => Java,Shell,Python,PHP,Nodejs,PowerShell
	 */
	private String scriptType;

	/**
	 * 修改时间
	 */
	private Date updateTime;

	/**
	 * 失败策略：stop-该运行节点停止, skip-跳过继续, retry-重试
	 */
	private String errorStrategy;

	/**
	 * 重试次数，如果归零则不再能够进行重试
	 */
	private Integer retryTimes;

	/** 当前正式运行状态。 */
	private Integer nodeRunStatus;

	/** 当前运行翻牌日期。 */
	private Date turnDate;

	/**
	 * 当前剩余重试次数，与模板 retryTimes 分离。
	 */
	private Integer currentRetryTimes;

}
