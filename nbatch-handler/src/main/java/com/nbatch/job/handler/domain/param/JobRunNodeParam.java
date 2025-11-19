package com.nbatch.job.handler.domain.param;

import lombok.Data;

/**
 * @description: 运行作业节点参数
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@Data
public class JobRunNodeParam {

    /**
     * 运行作业节点id
     */
    private String runNodeId;

    /**
     * script:脚本,store_procedure:存储过程,execute_sql:执行sql,file_to_db:文件导入到数据库,db_to_file:数据库导出到文件
     */
    private String nodeType;

    /**
     * 如果为：file_to_db
     */
    private JobWorkImportFileParam fileToDbParam;

    /**
     * 如果为：db_to_file
     */
    private JobWorkExportFileParam dbToFileParam;

}
