package com.nbatch.job.admin.core.domain.param;

import com.nbatch.job.admin.core.domain.model.BaseModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @description: 作业节点表
 * @author: Mr.ni
 * @date: 2025-11-13
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobWorkNodeLogPageParam extends BaseModel<JobWorkNodePageParam> {

    private String workId;

    private String nodeId;

    private Date startTime;

    private Date endTime;

}
