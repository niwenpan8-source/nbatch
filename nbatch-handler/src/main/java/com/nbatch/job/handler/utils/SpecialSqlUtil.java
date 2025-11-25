package com.nbatch.job.handler.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.handler.exception.HandlerException;
import lombok.extern.slf4j.Slf4j;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.nbatch.job.handler.enums.ExceptionCodeEnum.EXECUTE_UPDATE_SQL_FAIL;

/**
 * @description: 特殊sql运行处理
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@Slf4j
public class SpecialSqlUtil {

    /**
     * 执行更新sql
     */
    public static boolean execute(Connection conn, String boundSql) throws SQLException {
        boolean executeFlag;
        try (PreparedStatement preparedStatement = conn.prepareStatement(boundSql)) {
            executeFlag = preparedStatement.execute();
        } catch (SQLException e) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), e);
        } finally {
            // 这里使用连接代理关闭，重置线程池属性
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        }
        return executeFlag;
    }


    /**
     * 执行更新sql
     */
    public static int executeUpdate(Connection conn, String boundSql) throws SQLException {
        int executeNum;
        try (PreparedStatement preparedStatement = conn.prepareStatement(boundSql)) {
            executeNum = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), e);
        } finally {
            // 这里使用连接代理关闭，重置线程池属性
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        }
        return executeNum;
    }

    /**
     * 执行查询sql
     */
    public static List<Map<String, Object>> queryList(Connection conn, String sqlCmd, long rowsLimt) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        AtomicLong total = new AtomicLong(0L);
        ResultSet resultSet = null;
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = conn.prepareStatement(sqlCmd);
            resultSet = preparedStatement.executeQuery();

            while(resultSet.next()) {
                int columnCount = resultSet.getMetaData().getColumnCount();
                Map<String, Object> lineResult = new HashMap<>();

                for(int i = 1; i <= columnCount; ++i) {
                    String fieldAlias = resultSet.getMetaData().getColumnLabel(i);
                    if (StrUtil.isNotEmpty(fieldAlias)) {
                        lineResult.put(fieldAlias, resultSet.getObject(i));
                    } else {
                        lineResult.put(resultSet.getMetaData().getColumnName(i), resultSet.getObject(i));
                    }
                }

                result.add(lineResult);
                if (total.addAndGet(1L) >= rowsLimt) {
                    throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "查询记录数超过上限");
                }
            }
        } catch (Exception e) {
            log.error("查询异常：{}", e.getMessage());
        } finally {
            if (null != resultSet) {
                try {
                    resultSet.close();
                } catch (Exception e) {
                    log.error("关闭resultSet异常-{}", e.getMessage());
                }
            }

            if (null != preparedStatement) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {
                    log.error("关闭preparedStatement异常-{}", e.getMessage());
                }
            }
            // 这里使用连接代理关闭，重置线程池属性
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }

        }
        return result;
    }

    /**
     * 运行需要回调的语句
     */
    public static int executeSql(Connection conn, String tableSql, List<Object> params) throws SQLException {
        try (
                CallableStatement call = conn.prepareCall(tableSql);
        ) {
            if (CollUtil.isNotEmpty(params)) {
                for (int i = 0; i < params.size(); i++) {
                    Object param = params.get(i);
                    call.setObject(i + 1, param);
                }
            }
            call.registerOutParameter(params.size() + 1, Types.INTEGER);
            try (ResultSet ignored = call.executeQuery()) {
                // 输出OUT的值
                return call.getInt(params.size() + 1);
            } catch (SQLException e) {
                log.error("得到执行结果发生异常");
                throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), e);
            }
        } catch (Exception e) {
            log.error("执行SQL发生异常");
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), e);
        } finally {
            // 这里使用连接代理关闭，重置线程池属性
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        }
    }

}
