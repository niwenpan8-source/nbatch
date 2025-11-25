package com.nbatch.job.handler.dialect;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.ExecuteDbToFileParam;
import com.nbatch.job.core.biz.model.ExecuteFileToDbParam;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.utils.SpecialSqlUtil;
import lombok.extern.slf4j.Slf4j;
import org.opengauss.copy.CopyManager;
import org.opengauss.core.BaseConnection;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.util.List;

import static com.nbatch.job.handler.enums.ExceptionCodeEnum.EXECUTE_UPDATE_SQL_FAIL;

/**
 * @description: GBase数据库方言
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@Slf4j
public class GaussDialect implements BaseDialect {

    @Override
    public long fileToDb(Connection connection, ExecuteFileToDbParam param) throws Exception {
        try (
                FileInputStream fileInputStream = new FileInputStream(param.getFileName())
        ) {
            BaseConnection baseConnection = connection.unwrap(BaseConnection.class);
            CopyManager copyManager = new CopyManager(baseConnection);
            String importSql = generateFileToDbExecuteSql(param);

            return copyManager.copyIn(importSql, fileInputStream);
        } catch (Exception e) {
            log.error("高斯数据库导入数据异常");
            throw e;
        } finally {
            // 这里使用连接代理关闭，重置线程池属性
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        }
    }

    @Override
    public boolean dbToFile(Connection connection, ExecuteDbToFileParam param) throws Exception {
        try (
                FileOutputStream fileOutputStream = new FileOutputStream(param.getFilePath())
        ) {
            // 使用 unwrap 获取底层 BaseConnection
            BaseConnection baseConnection = connection.unwrap(BaseConnection.class);
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
    public int executeFunction(Connection connection, String tableSql, List<Object> params) throws Exception{
        return SpecialSqlUtil.executeSql(connection, tableSql, params);
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
        String importSql = "COPY " + param.getImportTableName() + " FROM STDIN ignore_extra_data fill_missing_fields 'multi' delimiter ' | ' encoding '"
                + param.getFileCode() +
                "' csv;";
        if (StrUtil.isNotEmpty(param.getImportTableFiled())) {
            importSql = "COPY " + param.getImportTableName() + "(" + param.getImportTableFiled() + ") FROM STDIN ignore_extra_data fill_missing_fields 'multi' delimiter ' | ' encoding 'utf-8' csv;";
        }
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
            executeSql.append(param.getExportTableFiled());
        } else {
            executeSql.append("*");
        }
        executeSql.append(" from ").append(param.getExportTableName());
        if (StrUtil.isNotBlank(param.getExportTableCondition())) {
            executeSql.append(" where ").append(param.getExportTableCondition());
        }

        return "COPY (" + executeSql + ") TO STDOUT delimiter ' | ' encoding '"
                + param.getFileCode() + "' csv;";

    }


}
