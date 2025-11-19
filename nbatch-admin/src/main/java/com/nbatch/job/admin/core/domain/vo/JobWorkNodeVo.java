package com.nbatch.job.admin.core.domain.vo;

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
    private String nodeId;

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
     * script:脚本,store_procedure:存储过程,execute_sql:执行sql,file_to_db:文件导入到数据库,db_to_file:数据库导出到文件
     */
    private String nodeType;
    private String nodeTypeName;

    /**
     * 翻牌日期
     */
    private Date turnDate;

}
