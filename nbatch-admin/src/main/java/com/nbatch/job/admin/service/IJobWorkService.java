package com.nbatch.job.admin.service;

import com.nbatch.job.admin.core.domain.param.JobWorkPageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkParam;
import com.nbatch.job.admin.core.domain.vo.JobWorkVo;

import java.util.Map;

/**
 * @description: 作业
 * @author: Mr.ni
 * @date: 2025/11/13
 */
public interface IJobWorkService {

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
     * 通过得到id得到对象
     */
    JobWorkVo getModel(String id);

    /**
     * 删除
     */
    int delete(String id);
}
