package com.nbatch.job.core.biz.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 运行节点本地事件拉取参数。
 */
@Data
public class RunNodeLogPullParam implements Serializable {

    private static final long serialVersionUID = 42L;

    private Long offset;
    private Integer maxSize;
}
