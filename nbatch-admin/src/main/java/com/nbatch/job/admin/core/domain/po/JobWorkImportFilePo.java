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
 * @description: 作业节点导入节点表
 * @author: Mr.ni
 * @date: 2025-11-13
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("nbatch_job_work_import_file")
public class JobWorkImportFilePo extends Model<JobWorkImportFilePo> {

	/**
	 * 导入文件id
	 */
	@TableId(type = IdType.ASSIGN_ID)
	private String importFileId;

	/**
	 * 作业节点id
	 */
	private String nodeId;

	/**
	 * 导入的文件名
	 */
	private String fileName;

	/**
	 * 导入的表名
	 */
	private String importTableName;

	/**
	 * 导入的列
	 */
	private String importTableFiled;

	/**
	 * 导入条件
	 */
	private String importTableCondition;

	/**
	 * 文件编码
	 */
	private String fileCode;

	/**
	 * 分隔符
	 */
	private String sep;

	/**
	 * 是否全量文件：1全量 0增量
	 */
	private Integer allUpdate;

	/**
	 * 是否压缩：1压缩 0不压缩
	 */
	private Integer isGzip;

	/**
	 * 生成文件名，特殊替换字符
	 */
	private String fileNameParam;

	/**
	 * 创建时间
	 */
	private Date createTime;

	/**
	 * 修改时间
	 */
	private Date updateTime;

}
