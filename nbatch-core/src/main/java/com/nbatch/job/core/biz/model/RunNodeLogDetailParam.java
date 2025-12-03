package com.nbatch.job.core.biz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @description: 作业运行节点详细日志
 * @author: Mr.ni
 * @date: 2025-12-03
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class RunNodeLogDetailParam {

    /**
     * 作业id
     */
    private String workId;

    /**
     * 运行作业id
     */
    private String runWorkId;

    /**
     * 作业节点id
     */
    private String nodeId;

    /**
     * 运行作业节点id
     */
    private String runNodeId;

    /**
     * 执行信息
     */
    private String handleMsg;

    /**
     * 执行-时间
     */
    private Date executeTime;

    /**
     * 执行-时间
     */
    private Date callBackTime;

}
