package com.nbatch.job.executor.service.helper;

import com.baomidou.mybatisplus.annotation.DbType;
import com.nbatch.job.core.util.SpringUtil;
import com.nbatch.job.handler.helper.DialectHelper;
import com.nbatch.job.handler.utils.SpecialSqlUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * @description: Gauss数据库执行sql帮助类
 * @author: Mr.ni
 * @date: 2025/12/19
 */
public class ExecuteGaussSqlHelper {

    /**
     * 执行更新sql
     */
    public static int executeUpdateSql(String sql) throws SQLException {
        DialectHelper dialectHelper = SpringUtil.getBean(DialectHelper.class);
        return SpecialSqlUtil.executeUpdate(dialectHelper.getConnection(DbType.OPENGAUSS.getDb()), sql);
    }

    /**
     * 执行查询sql
     */
    public static List<Map<String, Object>> executeQuerySql(String sql) throws SQLException {
        DialectHelper dialectHelper = SpringUtil.getBean(DialectHelper.class);
        return SpecialSqlUtil.queryList(dialectHelper.getConnection(DbType.OPENGAUSS.getDb()), sql, 1000000000L);
    }

}
