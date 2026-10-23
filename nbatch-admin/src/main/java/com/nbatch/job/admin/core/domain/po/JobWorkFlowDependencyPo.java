package com.nbatch.job.admin.core.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 流程依赖定义，前置端点完成后才可执行后置端点。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("nbatch_work_flow_dependency")
public class JobWorkFlowDependencyPo extends Model<JobWorkFlowDependencyPo> {

    @TableId(type = IdType.ASSIGN_ID)
    private String dependencyId;

    /**
     * 作业ID
     */
    private String workId;

    /**
     * 前置端点类型
     */
    private String predecessorType;

    /**
     * 前置端点ID
     */
    private String predecessorId;

    /**
     * 后置端点类型
     */
    private String successorType;

    /**
     * 后置端点ID
     */
    private String successorId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
