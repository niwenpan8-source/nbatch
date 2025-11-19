package com.nbatch.job.handler.helper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.nbatch.job.handler.dialect.BaseDialect;
import com.nbatch.job.handler.dialect.GBaseDialect;
import com.nbatch.job.handler.enums.DbTypeEnum;
import com.nbatch.job.handler.exception.HandlerException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static com.nbatch.job.handler.enums.ExceptionCodeEnum.EXECUTE_UPDATE_SQL_FAIL;

/**
 * @description: 数据库方言配置
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@Component
public class DialectHelper {

    @Resource
    private DynamicRoutingDataSource dataSource;


    /**
     * 获取数据库连接
     * @param dbType 数据库类型枚举
     * @return 数据库连接
     */
    public Connection getConnection(DbTypeEnum dbType) {
        DataSource gbaseDataSource = dataSource.getDataSource(dbType.getDb());
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
    public BaseDialect getDialect(DbTypeEnum dbType) {
        if (StrUtil.equals(dbType.getDb(), DbTypeEnum.GBASE.getDb())) {
            return new GBaseDialect();
        }
        return null;
    }


}
