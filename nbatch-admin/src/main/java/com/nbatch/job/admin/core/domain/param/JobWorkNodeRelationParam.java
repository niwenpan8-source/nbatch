package com.nbatch.job.admin.core.domain.param;

import lombok.Data;

import java.util.List;

import java.util.Date;

/**
 * @description: 作业节点关系表
 * @author: Mr.ni
 * @date: 2025-11-13
 */
@Data
public class JobWorkNodeRelationParam {

    /**
     * 作业id
     */
    private String workId;

    private List<NodeRelation> nodeRelationList;

    @Data
    public static class NodeRelation {

        /**
         * 节点1
         */
        private String nodeId1;

        /**
         * 节点2
         */
        private String nodeId2;
    }

    private String conditionExpression;

    private Integer delayMinutes;

    private String relationType;

    private Date createTime;

    private Date updateTime;

}
