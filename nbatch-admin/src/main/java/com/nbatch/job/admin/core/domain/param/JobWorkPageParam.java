package com.nbatch.job.admin.core.domain.param;

import com.nbatch.job.admin.core.domain.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @description: 作业节点分页参数
 * @author: Mr.ni
 * @date: 2025/11/13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JobWorkPageParam extends BaseModel<JobWorkPageParam> {

    private Integer workStatus;

}
