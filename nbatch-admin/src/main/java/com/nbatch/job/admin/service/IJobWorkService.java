package com.nbatch.job.admin.service;

import com.nbatch.job.admin.core.domain.param.JobWorkPageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkParam;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.vo.JobWorkVo;
import com.nbatch.job.core.biz.model.ReturnT;

import java.util.List;
import java.util.Map;
import java.util.Date;

/**
 * 作业
 */
public interface IJobWorkService {

    /**
     * 首页统计信息
     */
    Map<String, Object> dashboardInfo();

    /**
     * 首页统计信息
     */
    ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate);

    /**
     * 分页列表
     */
    Map<String, Object> pageList(JobWorkPageParam param);

    /**
     * 插入
     */
    int insert(JobWorkParam param);

    /**
     * 修改
     */
    int update(JobWorkParam param);

    /**
     * 启动调度
     */
    ReturnT<String> startSchedule(String workId);

    /**
     * 停止调度
     */
    ReturnT<String> stopSchedule(String workId);

    /**
     * 通过得到id得到对象
     */
    JobWorkVo getModel(String id);

    /**
     * 删除
     */
    int delete(String id);

}
