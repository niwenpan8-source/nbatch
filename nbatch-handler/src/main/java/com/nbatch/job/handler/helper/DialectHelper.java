package com.nbatch.job.handler.helper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.mybatisplus.annotation.DbType;
import com.nbatch.job.handler.dialect.BaseDialect;
import com.nbatch.job.handler.dialect.GBaseDialect;
import com.nbatch.job.handler.dialect.GaussDialect;
import com.nbatch.job.handler.exception.HandlerException;
import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static com.nbatch.job.handler.enums.ExceptionCodeEnum.EXECUTE_UPDATE_SQL_FAIL;

/**
 * @description: 数据库方言配置
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@RequiredArgsConstructor
public class DialectHelper {

    private final DynamicRoutingDataSource dataSource;

    /**
     * 获取数据库连接
     * @param dbType 数据库类型枚举
     * @return 数据库连接
     */
    public Connection getConnection(String dbType) {
        DataSource gbaseDataSource = dataSource.getDataSource(dbType);

        if (gbaseDataSource == null) {
            return null;
        }
        try {
            return gbaseDataSource.getConnection();
        } catch (SQLException e) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "获取数据库连接失败");
        }
    }

    /**
     * 获取数据库方言
     * @param dbType 数据库类型枚举
     * @return 数据库方言
     */
    public BaseDialect getDialect(String dbType) {
        if (StrUtil.equals(dbType, DbType.GBASE.getDb())) {
            return new GBaseDialect();
        } else if (StrUtil.equals(dbType, DbType.OPENGAUSS.getDb())) {
            return new GaussDialect();
        }
        return null;
    }


}
