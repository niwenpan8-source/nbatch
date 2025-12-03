package com.nbatch.job.admin.core.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * @description: 作业表
 * @author: Mr.ni
 * @date: 2025-11-13
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName("nbatch_job_work")
public class JobWorkPo extends Model<JobWorkPo> {

	/**
	 * 作业id
	 */
	@TableId(type = IdType.ASSIGN_ID)
	private String workId;

	/**
	 * 作业名
	 */
	private String workName;

	/**
	 * 作业描述
	 */
	private String workDesc;

	/**
	 * 作业类型 => 0:翻牌类型，1：顺序类型
	 */
	private Integer workType;

	/**
	 * 作业状态：0=停用、1=启用
	 */
	private Integer workStatus;

}
