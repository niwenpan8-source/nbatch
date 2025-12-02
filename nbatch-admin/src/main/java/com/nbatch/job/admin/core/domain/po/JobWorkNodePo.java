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
	 * 作业id
	 */
	private String workId;

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
	 * scipt:脚本,store_procedure:存储过程,execute_sql:执行sql,file_to_db:文件导入到数据库,db_to_file:数据库导出到文件
	 */
	private String nodeType;

	/**
	 * 数据库类型
	 */
	private String dbType;

	/**
	 * 执行sql
	 */
	private String executeSql;

	/**
	 * 执行sql参数
	 */
	private String executeSqlParam;

	/**
	 * 执行器
	 */
	private String executeHandler;

}
