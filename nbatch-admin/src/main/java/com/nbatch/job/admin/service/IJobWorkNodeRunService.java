package com.nbatch.job.admin.service;

import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogPo;
import com.nbatch.job.core.biz.model.ReturnT;

import java.util.List;

/**
 * 作业节点运行控制服务。
 */
public interface IJobWorkNodeRunService {

    /**
     * 回拨重跑当前节点。日期为空时使用当前作业批次翻牌日期。
     */
    ReturnT<String> rerunNode(String nodeId, String turnDate);

    /**
     * 回拨重跑当前节点和下游依赖节点。日期为空时使用当前作业批次翻牌日期。
     */
    ReturnT<String> rerunNodeWithDependents(String nodeId, String turnDate);

    /**
     * 停止运行中的节点。
     */
    ReturnT<String> stopRunNodes(String workId, List<JobWorkNodePo> runNodeList);

    /**
     * 停止并确认运行节点当前批次的执行已经退出。
     */
    ReturnT<String> stopAndConfirmRunNodesExited(String workId, List<JobWorkNodePo> runNodeList);

    /**
     * 获取未关闭的运行日志。
     */
    List<JobWorkRunNodeLogPo> getUnclosedRunNodeLogs(String workId, List<JobWorkNodePo> runNodeList);
}
