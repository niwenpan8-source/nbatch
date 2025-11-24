package com.nbatch.job.core.biz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @description: 作业节点表
 * @author: Mr.ni
 * @date: 2025-11-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteWorkParam {

    /**
     * 任务id
     */
    private String jobId;

    /**
     * 作业id
     */
    private String workId;

    /**
     * 执行节点参数列表
     */
    List<ExecuteNodeParam> executeNodeParamList;

}
