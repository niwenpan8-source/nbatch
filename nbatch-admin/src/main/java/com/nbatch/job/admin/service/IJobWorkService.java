package com.nbatch.job.admin.service;

import com.nbatch.job.admin.core.domain.param.JobWorkPageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkParam;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.vo.JobWorkVo;
import com.nbatch.job.core.biz.model.ReturnT;

import java.util.List;
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
     * 获取作业列表
     */
    List<JobWorkPo> getWorkList();

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

    /**
     * 恢复运行作业重跑
     */
    ReturnT<String> recoverRunWork(String runWorkId);

    /**
     * 重跑最新运行作业
     */
    ReturnT<String> rerunLatestRunWork(String workId);
}
