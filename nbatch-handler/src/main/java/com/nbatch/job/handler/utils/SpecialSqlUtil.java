package com.nbatch.job.handler.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nbatch.job.handler.constant.JobHandlerConstant.SQL_FIELD_REPLACE_CHAR;
import static com.nbatch.job.handler.constant.JobHandlerConstant.SQL_FIELD_REPLACE_REGEX;
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
        log.info("执行sql：{}", boundSql);
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
    public static boolean executeNotCloseConnect(Connection conn, String boundSql) throws SQLException {
        boolean executeFlag;
        try (PreparedStatement preparedStatement = conn.prepareStatement(boundSql)) {
            executeFlag = preparedStatement.execute();
        } catch (SQLException e) {
            conn.close();
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), e);
        }
        return executeFlag;
    }

    /**
     * 执行更新sql
     */
    public static int executeUpdate(Connection conn, String boundSql) throws SQLException {
        int executeNum;
        log.info("执行更新sql：{}", boundSql);
        try (PreparedStatement preparedStatement = conn.prepareStatement(boundSql)) {
            // 设置查询超时时间，避免长时间挂起
            executeNum = preparedStatement.executeUpdate();
        } catch (SQLException e) {
            log.error("执行更新SQL失败: {}", e.getMessage());
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
    public static List<Map<String, Object>> queryList(Connection conn, String sqlCmd, long rowsLimit) throws SQLException {
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
                if (total.addAndGet(1L) >= rowsLimit) {
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
     * 执行函数
     */
    public static String executeStoreProcedureReturnStr(Connection conn, String sql, JSONObject paramMap) throws SQLException {
        List<Object> params = new ArrayList<>();
        sql = replacePlaceholders(sql, paramMap, params);
        sql = "call " + sql;
        try (
                CallableStatement call = conn.prepareCall(sql);
        ) {
            call.registerOutParameter(params.size() + 1, Types.VARCHAR);
            if (CollUtil.isNotEmpty(params)) {
                for (int i = 0; i < params.size(); i++) {
                    Object param = params.get(i);
                    call.setObject(i + 1, param);
                }
            }
            // 输出OUT的值
            call.execute();
            return call.getString(params.size() + 1);
        } catch (Exception e) {
            log.error("执行函数SQL发生异常");
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), e);
        } finally {
            // 这里使用连接代理关闭，重置线程池属性
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        }
    }

    /**
     * 执行存储过程
     */
    public static void executeStoreProcedure(Connection conn, String sql, JSONObject paramMap) throws SQLException {
        List<Object> params = new ArrayList<>();
        sql = replacePlaceholders(sql, paramMap, params);
        try (
                CallableStatement call = conn.prepareCall(sql);
        ) {
            if (CollUtil.isNotEmpty(params)) {
                for (int i = 0; i < params.size(); i++) {
                    Object param = params.get(i);
                    call.setObject(i + 1, param);
                }
            }
            // 输出OUT的值
            call.execute();
        } catch (Exception e) {
            log.error("执行存储过程SQL发生异常");
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), e);
        } finally {
            // 这里使用连接代理关闭，重置线程池属性
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        }
    }

    public static String replacePlaceholders(String template, JSONObject values, List<Object> params) {
        Pattern pattern = Pattern.compile(SQL_FIELD_REPLACE_REGEX);
        Matcher matcher = pattern.matcher(template);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = values.getStr(key, "{" + key + "}");
            matcher.appendReplacement(result, SQL_FIELD_REPLACE_CHAR);
            params.add(value);
        }
        matcher.appendTail(result);
        return result.toString();
    }



}
