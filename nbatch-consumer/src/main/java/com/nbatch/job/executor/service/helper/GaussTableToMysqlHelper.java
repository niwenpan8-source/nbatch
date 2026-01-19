package com.nbatch.job.executor.service.helper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.nbatch.job.handler.helper.DialectHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * GaussDB 表数据复制到 MySQL
 * @author: Mr.ni
 * @date: 2026/1/14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GaussTableToMysqlHelper {

    private final DialectHelper dialectHelper;

    /**
     * 同步单张表结构
     */
    public void syncTableStructure(String tableName) throws Exception {
        Connection gaussConnection = dialectHelper.getConnection(DbType.OPENGAUSS.getDb());
        try {

            // 1. 从 GaussDB 获取表结构
            List<ColumnMeta> columns = getTableColumns(gaussConnection, tableName);
            if (columns.isEmpty()) {
                log.info("❌ 表 {} 不存在或无字段", tableName);
                return;
            }

            // 2. 在 ClickHouse 创建目标表
            createClickHouseTable(tableName, columns);

        } catch (Exception e) {
            // Table already exists
            if (e instanceof SQLException) {
                SQLException sqlException = (SQLException) e;
                if (sqlException.getErrorCode() == 1050) {
                    log.info("表已存在，跳过创建。");
                }
            } else {
                throw e;
            }

        } finally {
            // 这里使用连接代理关闭，重置线程池属性
            if (gaussConnection != null && !gaussConnection.isClosed()) {
                gaussConnection.close();
            }
        }

    }

    /**
     * 获取 GaussDB 表字段元数据
     */
    private List<ColumnMeta> getTableColumns(Connection gaussConnection, String tableName) throws SQLException {
        List<ColumnMeta> columns = new ArrayList<>();
        DatabaseMetaData metaData = gaussConnection.getMetaData();
        try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                String type = rs.getString("TYPE_NAME");
                int size = rs.getInt("COLUMN_SIZE");
                int decimalDigits = rs.getInt("DECIMAL_DIGITS");
                String nullable = rs.getString("IS_NULLABLE");

                columns.add(new ColumnMeta(name, type, size, decimalDigits, "NO".equalsIgnoreCase(nullable)));
            }
        }
        return columns;
    }


    /**
     * 在 ClickHouse 创建表
     */
    private void createClickHouseTable(String tableName, List<ColumnMeta> columns) throws Exception {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        String clickHouseTableName = StrUtil.format("synctable_customer_portrait_{}", tableName);
        String clickHouseTempTableName = StrUtil.format("synctable_customer_portrait_{}_today", tableName);
        sql.append("{0}").append(" (\n");

        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sql.append(",\n");
            }
            ColumnMeta col = columns.get(i);
            String chType = convertGaussTypeToClickHouse(col.gaussType, col.size, col.decimalDigits, col.notNull);
            sql.append("  `").append(col.name).append("` ").append(chType);
        }

        // 使用 MergeTree 引擎，按第一列排序（可优化）
        sql.append("\n) ENGINE = MergeTree() ORDER BY (")
                .append(columns.get(0).name)
                .append(");");
        String clickHouseCreateTableSql = StrUtil.indexedFormat(new String(sql), clickHouseTableName);
        String clickHouseCreateTempTableSql = StrUtil.indexedFormat(new String(sql), clickHouseTempTableName);
        // 创建表
        dialectHelper.getDialect(DbType.CLICK_HOUSE.getDb())
                .executeUpdate(dialectHelper.getConnection(DbType.CLICK_HOUSE.getDb())
                        , StrUtil.toString(clickHouseCreateTableSql));
        // 创建临时表
        dialectHelper.getDialect(DbType.CLICK_HOUSE.getDb())
                .executeUpdate(dialectHelper.getConnection(DbType.CLICK_HOUSE.getDb())
                        , StrUtil.toString(clickHouseCreateTempTableSql));
    }


    /**
     * GaussDB 类型 → ClickHouse 类型映射
     * @param gaussType GaussDB 类型
     * @param size 列大小
     * @param decimalDigits 小数位数
     * @param notNull 是否为非空
     */
    private static String convertGaussTypeToClickHouse(String gaussType, int size, int decimalDigits, boolean notNull) {
        gaussType = gaussType.toUpperCase();
        String baseType;

        switch (gaussType) {
            case "INT4": case "INTEGER":
                baseType = "Int32";
                break;
            case "INT8": case "BIGINT":
                baseType = "Int64";
                break;
            case "INT2": case "SMALLINT":
                baseType = "Int16";
                break;
            case "NUMERIC": case "DECIMAL":
                baseType = "Decimal(" + (size > 0 ? size : 10) + ", " + decimalDigits + ")";
                break;
            case "FLOAT4": case "REAL":
                baseType = "Float32";
                break;
            case "FLOAT8": case "DOUBLE PRECISION":
                baseType = "Float64";
                break;
            case "CHAR": case "VARCHAR": case "TEXT":
                // ClickHouse 不区分长度
                baseType = "String";
                break;
            case "BYTEA": case "BLOB":
                // 或用 FixedString，但 String 更通用
                baseType = "String";
                break;
            case "TIMESTAMP": case "TIMESTAMP WITHOUT TIME ZONE":
                // 默认 UTC
                baseType = "DateTime";
                break;
            case "TIMESTAMP WITH TIME ZONE":
                // 毫秒精度
                baseType = "DateTime64(3, 'UTC')";
                break;
            case "DATE":
                baseType = "Date";
                break;
            case "TIME":
                // ClickHouse 无原生 TIME 类型
                baseType = "String";
                break;
            case "BOOLEAN": case "BOOL":
                // 0/1 表示 false/true
                baseType = "UInt8";
                break;
            default:
                System.err.println("⚠️ 未知类型: " + gaussType + " → 映射为 String");
                baseType = "String";
        }

        // ClickHouse 支持 Nullable，但通常不建议（影响性能）
        return notNull ? baseType : "Nullable(" + baseType + ")";
    }

    // 字段元数据内部类
    private static class ColumnMeta {
        String name, gaussType;
        int size, decimalDigits;
        boolean notNull;

        ColumnMeta(String name, String gaussType, int size, int decimalDigits, boolean notNull) {
            this.name = name;
            this.gaussType = gaussType;
            this.size = size;
            this.decimalDigits = decimalDigits;
            this.notNull = notNull;
        }
    }




}
