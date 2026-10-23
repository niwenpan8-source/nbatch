package com.nbatch.job.admin.core.domain.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

/**
 * 作业流程组定义。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("nbatch_work_flow_group")
public class JobWorkFlowGroupPo extends Model<JobWorkFlowGroupPo> {

    @TableId(type = IdType.ASSIGN_ID)
    private String groupId;

    private String workId;

    private String groupName;

    private String groupDesc;

    private String groupType;

    private Integer sortOrder;

    private Integer version;

    private Date createTime;

    private Date updateTime;
}
