package com.nbatch.job.admin.service;

import com.nbatch.job.admin.core.domain.param.JobWorkNodePageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkNodeParam;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeVo;

import java.util.Map;

/**
 * @description: 作业节点
 * @author: Mr.ni
 * @date: 2025/11/13
 */
public interface IJobWorkNodeService {

    /**
     * 分页列表
     */
    Map<String, Object> pageList(JobWorkNodePageParam param);

    /**
     * 插入
     */
    int insert(JobWorkNodeParam param);

    /**
     * 修改
     */
    int update(JobWorkNodeParam param);

    /**
     * 通过得到id得到对象
     */
    JobWorkNodeVo getModel(String id);

    /**
     * 删除
     */
    int delete(String id);
}
