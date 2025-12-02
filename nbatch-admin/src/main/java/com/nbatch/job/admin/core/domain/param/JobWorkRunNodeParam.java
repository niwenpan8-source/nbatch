package com.nbatch.job.admin.core.domain.param;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @description: 作业运行节点
 * @author: Mr.ni
 * @date: 2025-11-20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobWorkRunNodeParam {

    /**
     * 运行节点id
     */
    private String runNodeId;

    /**
     * 作业id
     */
    private String workId;

    /**
     * 作业节点id
     */
    private String nodeId;

    /**
     * 节点运行状态：0=未运行、1=运行节点
     */
    private Integer nodeRunStatus;

    /**
     * 翻牌日期
     */
    private Date turnDate;

}
