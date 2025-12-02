package com.nbatch.job.core.biz.model;

import cn.hutool.core.util.IdUtil;
import lombok.Data;

import java.util.Date;

/**
 * @description: 执行节点参数
 * @author: Mr.ni
 * @date: 2025/11/20
 */
@Data
public class ExecuteNodeParam {

    /**
     * 运行作业id
     */
    private String workId;
    private String runWorkId;

    /**
     * 作业节点id
     */
    private String nodeId;
    private String runNodeId;

    /**
     * 运行节点日志id
     */
    private String nodeLogId = IdUtil.getSnowflakeNextIdStr();

    /**
     * script:脚本,store_procedure:存储过程,execute_sql:执行sql,file_to_db:文件导入到数据库,db_to_file:数据库导出到文件
     */
    private String nodeType;

    /**
     * 数据库类型
     */
    private String dbType;

    /**
     * 翻牌日期
     */
    private Date turnDate;

    /**
     * 执行sql
     */
    private String executeSql;

    /**
     * 执行sql参数
     */
    private String executeSqlParam;

    /**
     * 执行器
     */
    private String executeHandler;

    /**
     * 执行文件导入数据库参数
     */
    private ExecuteFileToDbParam executeFileToDbParam;

    /**
     * 执行数据库导出文件参数
     */
    private ExecuteDbToFileParam executeDbToFileParam;

}
