package com.nbatch.job.admin.core.domain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @description: 作业节点类型表
 * @author: Mr.ni
 * @date: 2025/11/18
 */
@Data
@Accessors(chain = true)
public class JobWorkNodeTypeVo {

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 节点类型名称
     */
    private String nodeTypeName;

    /**
     * 作业节点
     */
    private List<JobWorkNodeVo> jobWorkNodeList;
}
