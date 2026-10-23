package com.nbatch.job.core.biz.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 批量查询执行器中的运行节点状态。
 */
@Data
@Accessors(chain = true)
public class RunNodeStatusQueryParam implements Serializable {

    private static final long serialVersionUID = 42L;

    /** 需要查询的执行尝试标识。 */
    private List<String> nodeLogIdList;
}
