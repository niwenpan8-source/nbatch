package com.nbatch.job.admin.core.domain.param;

import lombok.Data;

import java.util.List;

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

}
