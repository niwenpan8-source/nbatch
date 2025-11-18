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
@TableName("nbatch_job_work_node")
public class JobWorkNodePo extends Model<JobWorkNodePo> {

	/**
	 * 作业节点id
	 */
	@TableId(type = IdType.ASSIGN_ID)
	private String nodeId;

	/**
	 * 节点名称
	 */
	private String nodeName;

	/**
	 * 节点描述
	 */
	private String nodeDesc;

	/**
	 * 节点状态：0=停用、1=启用
	 */
	private Integer nodeStatus;

	/**
	 * scipt:脚本,store_procedure:存储过程,execute_sql:执行sql,import:导入,export:导出
	 */
	private String nodeType;

}
