package com.nbatch.job.admin.core.domain.param;

import com.nbatch.job.admin.core.domain.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description: 作业节点分页参数
 * @author: Mr.ni
 * @date: 2025/11/13
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class JobWorkNodePageParam extends BaseModel<JobWorkNodePageParam> {

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 作业id
     */
    private String workId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 最新运行节点状态
     */
    private Integer nodeRunStatus;


}
