package com.nbatch.job.admin.service;


import com.nbatch.job.admin.core.domain.param.JobInfoParam;
import com.nbatch.job.admin.core.domain.po.JobUserPo;
import com.nbatch.job.core.biz.model.ReturnT;

import java.util.Date;
import java.util.Map;

/**
 * core job action for xxl-job
 * 
 * @author Mr.ni 2016-5-28 15:30:33
 */
public interface XxlJobService {

	/**
	 * page list
	 *
	 * @param start 开始
	 * @param length 长度
	 * @param jobGroup 任务组
	 * @param jobDesc 任务描述
	 * @param executorHandler 执行器
	 * @param author 作者
	 */
	Map<String, Object> pageList(int start, int length, String jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author);

	/**
	 * add job
	 *
	 * @param jobInfo 任务信息
	 */
	ReturnT<String> add(JobInfoParam jobInfo, JobUserPo loginUser);

	/**
	 * update job
	 *
	 * @param jobInfo 任务信息
	 */
	ReturnT<String> update(JobInfoParam jobInfo, JobUserPo loginUser);

	/**
	 * remove job
	 *
	 * @param id 任务ID
	 */
	ReturnT<String> remove(int id);

	/**
	 * start job
	 *
	 * @param id 任务ID
	 */
	ReturnT<String> start(int id);

	/**
	 * stop job
	 *
	 * @param id 任务ID
	 */
	ReturnT<String> stop(int id);

	/**
	 * trigger
	 *
	 * @param loginUser 登录用户
	 * @param jobId 任务ID
	 * @param executorParam 执行参数
	 * @param addressList 调度器地址列表
	 */
	ReturnT<String> trigger(JobUserPo loginUser, String jobId, String executorParam, String addressList);

	/**
	 * dashboard info
	 */
	Map<String,Object> dashboardInfo();

	/**
	 * chart info
	 *
	 * @param startDate 开始日期
	 * @param endDate 结束日期
	 */
	ReturnT<Map<String,Object>> chartInfo(Date startDate, Date endDate);

}
