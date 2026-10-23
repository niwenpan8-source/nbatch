package com.nbatch.job.core.biz.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Map;

/**
 * 执行器中的运行节点状态快照。
 */
@Data
@Accessors(chain = true)
public class RunNodeStatusQueryResult implements Serializable {

    private static final long serialVersionUID = 42L;

    /**
     * nodeLogId 对应的实时执行状态。
     */
    private Map<String, String> nodeStateMap;
}
