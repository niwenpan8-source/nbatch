package com.nbatch.job.admin.core.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @description: 作业运行节点
 * @author: Mr.ni
 * @date: 2025-11-20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobWorkRunNodeVo {

    /**
     * 运行节点id
     */
    private String nodeId;
    private String runNodeId;

    /**
     * 执行作业id
     */
    private String runWorkId;

    /**
     * script:脚本,store_procedure:存储过程,execute_sql:执行sql,file_to_db:文件导入到数据库,db_to_file:数据库导出到文件
     */
    private String nodeType;

    /**
     * 节点运行状态：0=未运行、1=运行节点
     */
    private Integer nodeRunStatus;

    /**
     * 节点运行状态：0=未运行、1=运行节点
     */
    private String dbType;

    /**
     * 翻牌日期
     */
    private Date turnDate;

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
