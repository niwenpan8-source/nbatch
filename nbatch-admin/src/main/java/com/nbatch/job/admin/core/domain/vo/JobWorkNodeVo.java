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
     * scipt:脚本,store_procedure:存储过程,execute_sql:执行sql,file_to_db:文件导入到数据库,db_to_file:数据库导出到文件
     */
    private String nodeType;

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

    private Date createTime;

    private Integer timeout;

    private Integer retryCount;

    private Integer retryInterval;

    private String errorStrategy;

    private String notifyEmail;

}
