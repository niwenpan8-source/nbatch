package com.nbatch.job.handler.dialect;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.clickhouse.client.ClickHouseRequest;
import com.clickhouse.client.ClickHouseResponse;
import com.clickhouse.data.ClickHouseFormat;
import com.clickhouse.jdbc.internal.ClickHouseConnectionImpl;
import com.nbatch.job.core.biz.model.ExecuteDbToFileParam;
import com.nbatch.job.core.biz.model.ExecuteFileToDbParam;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.utils.InvisibleCharUtil;
import com.nbatch.job.handler.utils.SpecialSqlUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.nbatch.job.handler.enums.ExceptionCodeEnum.EXECUTE_UPDATE_SQL_FAIL;

/**
 * @description: Clickhouse数据库方言
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@Slf4j
public class ClickhouseDialect implements BaseDialect {

    @Override
    public long fileToDb(Connection connection, ExecuteFileToDbParam param) throws Exception {
        try {
            ClickHouseConnectionImpl ckConn = connection.unwrap(ClickHouseConnectionImpl.class);
            String database = ckConn.getCurrentDatabase();

            if (database == null || database.isEmpty()) {
                throw new IllegalArgumentException("无法确定目标数据库，请在 param 中指定 database");
            }

            String table = param.getImportTableName();
            String filePath = param.getFilePath();
            String delimiter = Optional.ofNullable(param.getSep()).orElse(",");

            String insertSql = StrUtil.format("INSERT INTO {}.{}({})", database, table, param.getImportTableFiled());
            log.info("执行 CSV 导入: {}", insertSql);

            // 如果需要使用异步方式
            try (FileInputStream fis = new FileInputStream(filePath)) {
                ClickHouseRequest<?> request = ckConn.unwrap(ClickHouseRequest.class)
                        .write().data(fis)
                        .query(insertSql)
                        .format(ClickHouseFormat.CSV)
                        // 在 CSV 数据中被视为分隔符的字符。如果通过字符串设置，则该字符串的长度必须为 1。
                        .set("format_csv_delimiter", delimiter)
                        // 对于 Values 格式：如果流式解析器无法解析某个字段，则运行 SQL 解析器，并尝试将其作为 SQL 表达式进行解释。
                        .set("input_format_values_interpret_expressions", 0)
                        // 启用或禁用在插入数据时跳过多余字段。
                        // 写入数据时，如果输入数据包含目标表中不存在的列，ClickHouse 会抛出异常。如果启用了跳过功能，ClickHouse 不会插入多余数据，也不会抛出异常。
                        .set("input_format_skip_unknown_fields", 1)
                        // 设置从文本格式（CSV、TSV 等）读取时可接受的最大错误数。
                        //默认值为 0。
                        //应始终与 input_format_allow_errors_ratio 搭配使用。
                        //如果在读取行时发生错误，但错误计数器仍小于 input_format_allow_errors_num，ClickHouse 会忽略该行并继续处理下一行。
                        //如果同时超过 input_format_allow_errors_num 和 input_format_allow_errors_ratio，ClickHouse 会抛出异常。
                        .set("input_format_allow_errors_num", 10)
                        // 设置从文本格式（CSV、TSV 等）读取时允许出现错误的最大百分比。 该百分比通过 0 到 1 之间的浮点数指定。
                        // 默认值为 0。
                        // 始终与 input_format_allow_errors_num 一起使用。
                        // 如果在读取行时发生错误，但错误比例仍小于 input_format_allow_errors_ratio，ClickHouse 会忽略该行并继续处理下一行。
                        // 如果同时超过 input_format_allow_errors_num 和 input_format_allow_errors_ratio，ClickHouse 会抛出异常。
                        .set("input_format_allow_errors_ratio", 0.1);

                CompletableFuture<ClickHouseResponse> future = request.execute();
                future.get(30, TimeUnit.MINUTES);

                String queryCount = "SELECT COUNT(*) FROM " + param.getImportTableName();
                log.info("执行查询表数据条数sql：{}", queryCount);
                long readRows = 0;
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery(queryCount)) {
                    if (rs.next()) {
                        readRows = rs.getLong(1);
                    }
                }
                log.info("CSV 导入成功，共读取 {} 行", readRows);
                return readRows;
            } catch (Exception e) {
                log.error("CSV 导入失败:", e);
                throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(),
                        "CSV 导入失败: " + ExceptionUtil.getRootCauseMessage(e));
            }
        } catch (Exception e) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), e);
        } finally {
            // 这里使用连接代理关闭，重置线程池属性
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
    }

    @Override
    public boolean dbToFile(Connection connection, ExecuteDbToFileParam param) throws Exception {
        try {
            ClickHouseConnectionImpl ckConn = connection.unwrap(ClickHouseConnectionImpl.class);
            String database = ckConn.getCurrentDatabase();

            if (database == null || database.isEmpty()) {
                throw new IllegalArgumentException("无法确定目标数据库，请在 param 中指定 database");
            }

            String table = param.getExportTableName();
            String filePath = param.getFilePath();
            String sep = Optional.ofNullable(param.getSep()).orElse(",");
            char delimiter = InvisibleCharUtil.parseChar(sep);

            String querySql = StrUtil.format("SELECT {} FROM {}.{}", param.getExportTableFiled()
                    , database, table);
            log.info("执行 table -> csv file: {}", querySql);
            // 如果需要使用异步方式
            try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
                ClickHouseRequest<?> request = ckConn.unwrap(ClickHouseRequest.class)
                        .query(querySql)
                        .format(ClickHouseFormat.CSVWithNames)
                        .set("format_csv_delimiter", delimiter);
                byte[] buffer = new byte[8192];
                int bytesRead;
                InputStream inputStream = request.execute().get().getInputStream();
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                }
                return true;
            } catch (Exception e) {
                log.error("CSV 导入失败:", e);
                throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(),
                        "CSV 导入失败: " + ExceptionUtil.getRootCauseMessage(e));
            }
        } catch (Exception e) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), e);
        } finally {
            // 这里使用连接代理关闭，重置线程池属性
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
    }


    /**
     * 删除表根据表名
     */
    @Override
    public int dropTable(Connection connection, String tableName) throws Exception {
        String dropTableSql = "DROP TABLE IF EXISTS " + tableName;
        log.info("执行 drop table sql：{}", dropTableSql);
        return SpecialSqlUtil.executeUpdate(connection, dropTableSql);
    }

    /**
     * 根据表名根据原表创建临时表，临时表名格式为原表名_today
     */
    @Override
    public int copyTableStructure(Connection connection, String tableName, String targetTableName) throws Exception {
        String createTableSql = StrUtil.format("CREATE TABLE {} LIKE {}", targetTableName, tableName);
        log.info("执行 create table sql：{}", createTableSql);
        return SpecialSqlUtil.executeUpdate(connection, createTableSql);
    }

    /**
     * 根据表名根据原表创建临时表，临时表名格式为原表名_today
     */
    @Override
    public int copyTableNotStructure(Connection connection, String tableName, String targetTableName) throws Exception {
        String createTableSql = StrUtil.format("CREATE TABLE {} LIKE {}", targetTableName, tableName);
        log.info("执行 create table not structure sql：{}", createTableSql);
        return SpecialSqlUtil.executeUpdate(connection, createTableSql);
    }

    /**
     * 将临时表修改为原表名
     */
    @Override
    public int renameTable(Connection connection, String currentTableName, String targetTableName) throws Exception {
        String renameTableSql = StrUtil.format("ALTER TABLE {} RENAME TO {}", currentTableName, targetTableName);
        log.info("执行 rename table sql：{}", renameTableSql);
        return SpecialSqlUtil.executeUpdate(connection, renameTableSql);
    }

    @Override
    public void executeStoreProcedure(Connection connection, String sql, JSONObject paramObj) throws Exception {
        log.info("执行 function paramObj sql：{}", sql);
        SpecialSqlUtil.executeStoreProcedure(connection, sql, paramObj);
    }

    @Override
    public String executeStoreProcedureReturnStr(Connection connection, String sql, JSONObject paramObj) throws Exception {
        log.info("执行 function paramObj sql：{}", sql);
        return SpecialSqlUtil.executeStoreProcedureReturnStr(connection, sql, paramObj);
    }

    @Override
    public int executeUpdate(Connection connection, String sql) throws Exception {
        log.info("执行 update sql：{}", sql);
        return SpecialSqlUtil.executeUpdate(connection, sql);
    }


}
