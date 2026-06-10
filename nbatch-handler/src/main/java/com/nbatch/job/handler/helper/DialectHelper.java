package com.nbatch.job.handler.helper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.mybatisplus.annotation.DbType;
import com.nbatch.job.core.context.BatchJobHelper;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.handler.dialect.*;
import com.nbatch.job.handler.enums.NodeTypeEnum;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.thread.BatchThreadPoolExecutor;
import com.nbatch.job.handler.utils.BatchThreadPoolUtil;
import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

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
     * 该方法要求传入一个 Executor 对象用于处理超时任务。在实际应用中，
     * 需要注意这个 Executor 的生命周期和线程数管理，避免因为频繁创建/销毁线程或在高并发下耗尽线程池资源而引发新的性能问题。
     */
    private static final String THREAD_POOL_KEY = "handleTimeOutTask";

    private static final int THREAD_POOL_NUM = 1;


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
            Connection connection = gbaseDataSource.getConnection();
            BatchThreadPoolExecutor batchThreadPoolExecutor = BatchThreadPoolUtil.getBatchThreadPoolExecutor(THREAD_POOL_KEY);
            if (batchThreadPoolExecutor == null) {
                batchThreadPoolExecutor = BatchThreadPoolUtil.newThreadPoolExecutorDiscard(THREAD_POOL_KEY, THREAD_POOL_NUM,
                        THREAD_POOL_NUM, 30, TimeUnit.MINUTES, 1000);
            }
            connection.setNetworkTimeout(batchThreadPoolExecutor, 200000);
            return connection;
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
        if (StrUtil.equals(dbType.toLowerCase(), DbType.GBASE.getDb())) {
            return new GBaseDialect();
        } else if (StrUtil.equals(dbType.toLowerCase(), DbType.GAUSS_DB.getDb().toLowerCase())) {
            return new GaussDialect();
        } else if (StrUtil.equals(dbType.toLowerCase(), DbType.OPENGAUSS.getDb().toLowerCase())) {
            return new OpenGaussDialect();
        } else if (StrUtil.equals(dbType.toLowerCase(), DbType.MYSQL.getDb())) {
            return new MysqlDialect();
        } else if (StrUtil.equals(dbType.toLowerCase(), DbType.CLICK_HOUSE.getDb())) {
            return new ClickhouseDialect();
        }
        return null;
    }

}
