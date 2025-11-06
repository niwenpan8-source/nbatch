package com.nbatch.job.admin.core.model;

import lombok.Data;

import java.util.Date;

/**
 * log for glue, used to track job code process
 * @author Mr.ni 2016-5-19 17:57:46
 */
@Data
public class XxlJobLogGlue {
	
	private int id;

	/**
	 * 任务主键ID
	 */
	private int jobId;

	/**
	 * GLUE类型	#com.nbatch.job.core.glue.GlueTypeEnum
	 */
	private String glueType;
	private String glueSource;
	private String glueRemark;
	private Date addTime;
	private Date updateTime;

}
