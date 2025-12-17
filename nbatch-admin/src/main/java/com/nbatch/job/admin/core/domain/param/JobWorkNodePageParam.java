package com.nbatch.job.admin.core.domain.param;

import com.nbatch.job.admin.core.domain.model.BaseModel;
import lombok.Data;

/**
 * @description: 作业节点分页参数
 * @author: Mr.ni
 * @date: 2025/11/13
 */
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


}
