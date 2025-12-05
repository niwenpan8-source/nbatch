package com.nbatch.job.core.biz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

/**
 * @description: 作业节点表
 * @author: Mr.ni
 * @date: 2025-11-13
 */
@Data
@Accessors(chain = true)
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
    private String runWorkId;

    /**
     * 作业类型
     */
    private Integer workType;

    /**
     * 日志
     */
    private String jobLogId;

    /**
     * 作业翻牌时间
     */
    private Date turnDate;

    /**
     * 执行节点参数列表
     */
    List<ExecuteNodeParam> executeNodeParamList;

}
