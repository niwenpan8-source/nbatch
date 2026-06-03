package com.nbatch.job.admin.core.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;

import java.util.Date;

/**
 * @description: 作业节点关系表
 * @author: Mr.ni
 * @date: 2025-11-13
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("nbatch_job_work_node_relation")
public class JobWorkNodeRelationPo extends Model<JobWorkNodeRelationPo> {

	/**
	 * 作业节点关系id
	 */
	@TableId(type = IdType.ASSIGN_ID)
	private String nodeRelationId;

	/**
	 * 作业id
	 */
	private String workId;

	/**
	 * 节点1
	 */
	private String nodeId1;

	/**
	 * 节点2
	 */
	private String nodeId2;

	/**
	 * 节点顺序（用于顺序类型流程）
	 */
	private Integer nodeOrder;

	/**
	 * 条件表达式（用于条件分支）
	 */
	private String conditionExpression;

	/**
	 * 执行延迟时间，单位分钟
	 */
	private Integer delayMinutes;

	/**
	 * 关系类型：sequential-顺序, parallel-并行, conditional-条件
	 */
	private String relationType;

	/**
	 * 创建时间
	 */
	private Date createTime;

	/**
	 * 修改时间
	 */
	private Date updateTime;

}
