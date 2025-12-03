package com.nbatch.job.admin.core.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @description: 作业节点表
 * @author: Mr.ni
 * @date: 2025-11-13
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class JobWorkNodeVo {

    /**
     * 作业节点id
     */
    private String nodeId;
    private String runNodeId;

    /**
     * 作业id
     */
    private String workId;
    private String runWorkId;

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
     * 节点运行状态：0=未运行、1=运行节点
     */
    private Integer nodeRunStatus;

    /**
     * 节点运行时间
     */
    private Date turnDate;

    /**
     * script:脚本,store_procedure:存储过程,execute_sql:执行sql,file_to_db:文件导入到数据库,db_to_file:数据库导出到文件
     */
    private String nodeType;
    private String nodeTypeName;

    /**
     * 数据库类型
     */
    private String dbType;

    /**
     * 执行内容
     */
    private String executeContent;

    /**
     * 执行内容参数
     */
    private String executeContentParam;

    /**
     * 执行器
     */
    private String executeHandler;

    /**
     * 脚本类型 => Java,Shell,Python,PHP,Nodejs,PowerShell
     */
    private String scriptType;

    /**
     * 修改时间
     */
    private Date updateTime;


}
