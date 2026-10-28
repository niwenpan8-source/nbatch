package com.nbatch.job.admin.core.domain.param;

import com.nbatch.job.admin.core.domain.model.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 作业节点分页参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JobWorkPageParam extends BaseModel<JobWorkPageParam> {

    private String workName;

}
