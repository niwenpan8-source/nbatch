package com.nbatch.job.core.biz.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 运行节点本地事件拉取结果。
 */
@Data
@Accessors(chain = true)
public class RunNodeLogPullResult implements Serializable {

    private static final long serialVersionUID = 42L;

    private Long ackOffset;
    private Long nextOffset;
    private List<RunNodeLogEventParam> eventList;
}
