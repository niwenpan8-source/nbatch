package com.nbatch.job.admin.service;

import com.nbatch.job.admin.core.domain.param.JobWorkNodePageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkNodeParam;
import com.nbatch.job.admin.core.domain.param.JobWorkNodeRelationParam;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodeRelationPo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeRelationVo;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeTypeVo;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeVo;
import com.nbatch.job.admin.core.domain.vo.JobWorkRunNodeVo;

import java.util.List;
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

    /**
     * 得到所有的发布的
     */
    List<JobWorkNodePo> getWorkNode(String workId);

    /**
     * 获取所有启用的作业
     */
    List<JobWorkPo> getAllWorkList();

    /**
     * 获得所有作业节点关系
     */
    List<JobWorkNodeRelationVo> getWorkNodeRelationByWorkId(String workId);

    /**
     * 批量插入作业节点关系
     */
    int updateWorkNodeRelation(JobWorkNodeRelationParam param);

}
