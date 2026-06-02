package com.nbatch.job.admin.core.domain.param;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @description: 作业节点关系表
 * @author: Mr.ni
 * @date: 2025-11-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobWorkNodeRelationParam {

    /**
     * 作业节点关系id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String nodeRelationId;

    /**
     * 作业id
     */
    private String workId;

    /**
     * 节点1
     */
    private String nodeId1;

    /**
     * 节点2
     */
    private String nodeId2;

    /**
     * 节点顺序
     */
    private Integer nodeOrder;

    private String conditionExpression;

    private Integer delayMinutes;

    private String relationType;

    private Date createTime;

    private Date updateTime;

}
