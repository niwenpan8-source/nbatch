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

    /**
     * 事件偏移量
     */
    private Long offset;

    /**
     * 事件时间戳
     */
    private Long timestamp;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 作业ID
     */
    private String workId;

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 节点日志ID
     */
    private String nodeLogId;

    /**
     * 翻牌时间
     */
    private Date turnDate;

    /**
     * 作业类型
     */
    private Integer workType;

    /**
     * 处理码
     */
    private Integer handleCode;

    /**
     * 处理信息
     */
    private String handleMsg;

}
