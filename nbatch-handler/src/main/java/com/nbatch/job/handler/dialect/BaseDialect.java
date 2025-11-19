package com.nbatch.job.handler.dialect;

import com.nbatch.job.handler.domain.param.JobWorkExportFileParam;
import com.nbatch.job.handler.domain.param.JobWorkImportFileParam;

import java.sql.Connection;
import java.util.List;

/**
 * @description: 方言接口
 * @author: Mr.ni
 * @date: 2025/11/19
 */
public interface BaseDialect {

    /**
     * 将文件从文件导入到数据库
     */
    long fileToDb(Connection connection, JobWorkImportFileParam param);

    /**
     * 将文件从数据库导入到文件
     */
    long dbToFile(Connection connection, JobWorkExportFileParam param);

    /**
     * 执行存储过程
     */
    int executeSql(Connection connection, String tableSql, List<Object> params);


}
