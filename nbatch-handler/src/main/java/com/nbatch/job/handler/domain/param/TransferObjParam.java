package com.nbatch.job.handler.domain.param;

import lombok.Data;

/**
 * @description: 传输对象
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@Data
public class TransferObjParam {

    /**
     * 作业id
     */
    private String workId;

    /**
     * 节点id
     */
    private String nodeId;

    /**
     * 任务执行状态
     * 1: 成功
     * 0: 失败
     */
    private int taskExecuteStatus;


}
