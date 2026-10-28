package com.nbatch.job.admin.core.domain.param;

import com.nbatch.job.admin.core.domain.model.BaseModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 作业节点表
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobWorkNodeLogPageParam extends BaseModel<JobWorkNodePageParam> {

    private String workId;

    private String nodeId;

    private String groupId;

    private String nodeLogId;


    private String startTime;

    private String endTime;

}
