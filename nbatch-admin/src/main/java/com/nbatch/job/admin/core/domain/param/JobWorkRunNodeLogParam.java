package com.nbatch.job.admin.core.domain.param;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description: NbatchJobWorkRunNodeLog
 * @author: Mr.ni
 * @date: 2025-11-25
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobWorkRunNodeLogParam {

    /**
     * 节点日志id
     */
    private String nodeLogId;

    /**
     * 作业id
     */
    private String workId;

    /**
     * 作业节点id
     */
    private String nodeId;

    /**
     * 执行状态
     */
    private Integer handleCode;

    /**
     * 执行信息
     */
    private String handleMsg;

}
