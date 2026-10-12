package com.nbatch.job.admin.core.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.EqualsAndHashCode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
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
@TableName("nbatch_work")
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

	private String jobGroup;
	private String scheduleType;
	@TableField(updateStrategy = FieldStrategy.ALWAYS)
	private String scheduleConf;

	/**
	 * 调度状态：0=停止、1=运行中
	 */
	private Integer scheduleStatus;
	private String misfireStrategy;
	@TableField(updateStrategy = FieldStrategy.ALWAYS)
	private Long triggerLastTime;
	@TableField(updateStrategy = FieldStrategy.ALWAYS)
	private Long triggerNextTime;
	private Integer timeout;
	private String notifyEmail;
	private Date createTime;
	private Date updateTime;

	/**
	 * 流程版本号
	 */
	private Integer version;

	/** 当前流程运行状态。 */
	private Integer runWorkStatus;

	/** 当前业务翻牌日期。 */
	private Date turnDate;

	/** 当前一轮流程的创建时间。 */
	private Date runCreateTime;

}
