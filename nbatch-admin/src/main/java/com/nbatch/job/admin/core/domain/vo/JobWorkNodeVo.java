package com.nbatch.job.admin.core.domain.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @description: 作业节点表
 * @author: Mr.ni
 * @date: 2025-11-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobWorkNodeVo {

    /**
     * 作业节点id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private String nodeId;

    /**
     * 作业父节点id
     */
    private String parentNodeId;

    /**
     * 作业id
     */
    private String workId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点描述
     */
    private String nodeDesc;

    /**
     * 节点状态：0=停用、1=启用
     */
    private Integer nodeStatus;

    /**
     * scipt:脚本,store_procedure:存储过程,execute_sql:执行sql,import:导入,export:导出
     */
    private String nodeType;

    /**
     * 节点顺序
     */
    private Integer nodeOrder;

    /**
     * 翻牌时间
     */
    private Date turnTime;

}
