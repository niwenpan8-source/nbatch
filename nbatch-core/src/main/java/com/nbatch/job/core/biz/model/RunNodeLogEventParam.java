package com.nbatch.job.core.biz.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 运行节点本地事件日志。
 */
@Data
@Accessors(chain = true)
public class RunNodeLogEventParam implements Serializable {

    private static final long serialVersionUID = 42L;

    private Long offset;
    private Long timestamp;
    private String eventType;
    private String workId;
    private String runWorkId;
    private String nodeId;
    private String runNodeId;
    private String nodeLogId;
    private Date turnDate;
    private Integer workType;
    private Integer handleCode;
    private String handleMsg;
}
