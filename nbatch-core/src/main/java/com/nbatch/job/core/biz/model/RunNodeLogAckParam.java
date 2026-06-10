package com.nbatch.job.core.biz.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 运行节点本地事件确认参数。
 */
@Data
public class RunNodeLogAckParam implements Serializable {

    private static final long serialVersionUID = 42L;

    private Long offset;
}
