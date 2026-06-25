package com.nbatch.job.handler.dialect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.nbatch.job.core.biz.model.ExecuteDbToFileParam;
import com.nbatch.job.core.biz.model.ExecuteFileToDbParam;
import com.nbatch.job.handler.utils.SpecialSqlUtil;

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
    void executeStoreProcedure(Connection connection, String tableSql, JSONObject paramObj) throws Exception;

    /**
     * 执行存储过程返回str
     */
    String executeStoreProcedureReturnStr(Connection connection, String sql, JSONObject paramObj) throws Exception ;

    /**
     * 执行修改
     */
    int executeUpdate(Connection connection, String sql) throws Exception;

    /**
     * 删除表根据表名
     */
    int dropTable(Connection connection, String tableName) throws Exception;

    /**
     * 根据表名根据原表创建临时表，临时表名格式为原表名_today
     */
    int copyTableStructure(Connection connection, String tableName, String targetTableName)  throws Exception;

    /**
     * 将临时表修改为原表名
     */
    int renameTable(Connection connection, String currentTableName, String targetTableName) throws Exception;

}
