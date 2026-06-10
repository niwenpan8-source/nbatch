package com.nbatch.job.handler.dialect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.nbatch.job.core.biz.model.ExecuteDbToFileParam;
import com.nbatch.job.core.biz.model.ExecuteFileToDbParam;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.utils.SpecialSqlUtil;
import lombok.extern.slf4j.Slf4j;
import org.opengauss.copy.CopyManager;
import org.opengauss.core.BaseConnection;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static com.nbatch.job.handler.enums.ExceptionCodeEnum.EXECUTE_UPDATE_SQL_FAIL;

/**
 * @description: Gauss数据库方言
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@Slf4j
public class OpenGaussDialect implements BaseDialect {

    @Override
    public long fileToDb(Connection connection, ExecuteFileToDbParam param) throws Exception {
        // 这里使用连接代理关闭，重置线程池属性
        if (connection == null || connection.isClosed()) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "连接已关闭");
        }
        boolean originalAutoCommit = connection.getAutoCommit();
        try (
                FileInputStream fileInputStream = new FileInputStream(param.getFilePath());
                BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream, StandardCharsets.UTF_8));
                ByteArrayOutputStream batchBuffer = new ByteArrayOutputStream()
        ) {
            // 开启自动提交
            connection.setAutoCommit(false);
            BaseConnection baseConnection = connection.unwrap(BaseConnection.class);
            String line;
            int currentBatchLineCount = 0;
            long totalCopied = 0L;

            // 每10万提交一次
            final int batchSize = 100_000;
            String importSql = generateFileToDbExecuteSql(param);
            while ((line = reader.readLine()) != null) {
                batchBuffer.write(line.getBytes(StandardCharsets.UTF_8));
                // CSV 行结束符
                batchBuffer.write('\n');
                currentBatchLineCount++;

                // 达到批次大小，执行 copyIn
                if (currentBatchLineCount >= batchSize) {
                    totalCopied += flushBatch(baseConnection, importSql, batchBuffer);
                    batchBuffer.reset(); // 清空缓冲区
                    currentBatchLineCount = 0;

                    // 提交当前批次事务
                    connection.commit();
                    log.info("已提交批次，累计导入 {} 行", totalCopied);
                }
            }

            // 处理剩余的数据
            if (currentBatchLineCount > 0) {
                totalCopied += flushBatch(baseConnection, importSql, batchBuffer);
                connection.commit();
                log.info("已提交最后批次，累计导入 {} 行", totalCopied);
            }

            log.info("高斯数据库导入sql：{}", importSql);
            return totalCopied;
        } catch (Exception e) {
            log.error("高斯数据库导入数据异常", e);
            try {
                connection.rollback(); // 回滚事务
            } catch (SQLException rollbackEx) {
                log.error("回滚事务失败", rollbackEx);
            }
            throw e;
        } finally {
            // 这里使用连接代理关闭，重置线程池属性
            try {
                connection.setAutoCommit(originalAutoCommit);
                connection.close();
            } catch (SQLException e) {
                log.error("恢复自动提交状态失败", e);
            }
        }
    }

    /**
     * 执行单个批次的 copyIn
     */
    private long flushBatch(BaseConnection baseConnection, String copySql, ByteArrayOutputStream buffer) throws Exception {
        try (InputStream batchStream = new ByteArrayInputStream(buffer.toByteArray())) {
            CopyManager copyManager = new CopyManager(baseConnection);
            return copyManager.copyIn(copySql, batchStream);
        } catch (IOException e) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), e);
        }
    }

    @Override
    public boolean dbToFile(Connection connection, ExecuteDbToFileParam param) throws Exception {
        try (
                FileOutputStream fileOutputStream = new FileOutputStream(param.getFilePath())
        ) {
            // 使用 unwrap 获取底层 BaseConnection
            BaseConnection baseConnection = connection.unwrap(BaseConnection.class);

            log.info("高斯数据库的 NetworkTimeout：{}", baseConnection.getNetworkTimeout());
            CopyManager copyManager = new CopyManager(baseConnection);
            String exportSql = generateDbToFileExecuteSql(param);
            log.info("高斯数据库导出sql：{}", exportSql);
            return copyManager.copyOut(exportSql, fileOutputStream) > 0;
        } catch (Exception e) {
            log.error("高斯数据库导出数据异常");
            throw e;
        } finally {
            // 这里使用连接代理关闭，重置线程池属性
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
    }

    @Override
    public void executeStoreProcedure(Connection connection, String sql, JSONObject paramObj) throws Exception {
        log.info("执行 function paramObj sql：{}, paramObj:{}", sql, paramObj);
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

    /**
     * 生成gbase文件导入db的sql
     */
    private String generateFileToDbExecuteSql(ExecuteFileToDbParam param) {
        if (param == null) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "gbase文件导入db,参数不能为空");
        }
        if (StrUtil.isBlank(param.getImportTableName())) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "gbase文件导入db,表名不能为空");
        }
        if (StrUtil.isBlank(param.getFileCode())) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "gbase文件导入db,文件编码不能为空");
        }
        if (StrUtil.isBlank(param.getImportTableFiled())) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "gbase文件导入db,表列属性不能为空");
        }
        String importSql = "COPY {tableName} ({tableField}) FROM STDIN ignore_extra_data " +
                "fill_missing_fields 'multi' delimiter '{sep}' encoding" +
                " '{fileCode}' csv;";

        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("tableName", param.getImportTableName());
        paramMap.put("fileCode", param.getFileCode());
        paramMap.put("tableField", param.getImportTableFiled());
        if (StrUtil.isNotEmpty(param.getSep())) {
            paramMap.put("sep", param.getSep());
        } else {
            paramMap.put("sep", "|");
        }
        importSql = StrUtil.format(importSql, paramMap);

        return importSql;
    }

    /**
     * 生成gbase db导入文件的sql
     */
    private String generateDbToFileExecuteSql(ExecuteDbToFileParam param) {
        if (param == null) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "gbasedb导入文件,参数不能为空");
        }
        if (StrUtil.isBlank(param.getExportTableName())) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "gbasedb导入文件,表名不能为空");
        }
        if (StrUtil.isBlank(param.getFileCode())) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "gbasedb导入文件,文件编码不能为空");
        }
        StringBuilder executeSql = new StringBuilder();
        executeSql.append("select ");
        if (StrUtil.isNotBlank(param.getExportTableFiled())) {
            // 使用字符串替换函数去除字段中的换行符和分隔符 |
            executeSql.append(param.getExportTableFiled());
//            String[] fields = param.getExportTableFiled().split(",");
//            for (int i = 0; i < fields.length; i++) {
//                String field = fields[i].trim();
//                // 使用REPLACE函数去除换行符和分隔符 |
//                executeSql.append("REPLACE(REPLACE(REPLACE(").append(field).append(", E'\\n', ''), E'\\r', ''), '|', '') AS ").append(field);
//                if (i < fields.length - 1) {
//                    executeSql.append(", ");
//                }
//            }
        } else {
            executeSql.append("*");
        }
        executeSql.append(" from ").append(param.getExportTableName());
        if (StrUtil.isNotBlank(param.getExportTableCondition())) {
            executeSql.append(" where ").append(param.getExportTableCondition());
        }

        return "COPY (" + executeSql + ") TO STDOUT delimiter '" + (StrUtil.isEmpty(param.getSep()) ? "|" : param.getSep())
                + "' encoding '"
                + param.getFileCode() + "' csv;";

    }


}
