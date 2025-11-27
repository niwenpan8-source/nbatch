package com.nbatch.job.handler.dialect;

import com.nbatch.job.core.biz.model.ExecuteDbToFileParam;
import com.nbatch.job.core.biz.model.ExecuteFileToDbParam;

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
    long fileToDb(Connection connection, ExecuteFileToDbParam param) throws Exception;

    /**
     * 将文件从数据库导入到文件
     */
    boolean dbToFile(Connection connection, ExecuteDbToFileParam param) throws Exception;

    /**
     * 执行存储过程
     */
    int executeFunction(Connection connection, String tableSql, List<Object> params) throws Exception;

    /**
     * 执行修改
     */
    int executeUpdate(Connection connection, String sql) throws Exception;
}
