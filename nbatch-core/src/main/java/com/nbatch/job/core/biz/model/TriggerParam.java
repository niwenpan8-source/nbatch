package com.nbatch.job.core.biz.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * 触发参数
 * @author Mr.ni
 * @date 2025/11/05
 */
@Data
@ToString
@NoArgsConstructor
public class TriggerParam implements Serializable{
    private static final long serialVersionUID = 42L;

    private String jobId;

    private String executorHandler;
    private String executorParams;
    private String executorBlockStrategy;
    private int executorTimeout;

    private String logId;
    private long logDateTime;
    /**
     * Trigger idempotency key. Defaults to logId when blank.
     */
    private String triggerKey;

    private String glueType;
    private String glueSource;
    private long glueUpdatetime;

    private int broadcastIndex;
    private int broadcastTotal;


    /**
     * 作业id
     */
    private String workId;

    /**
     * 执行作业参数
     */
    private ExecuteWorkParam executeWorkParam;


}
