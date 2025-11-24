package com.nbatch.job.admin.core.domain.param;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
     * 节点顺序
     */
    private Integer nodeSequence;

}
