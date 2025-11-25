package com.nbatch.job.handler.dialect;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.ExecuteDbToFileParam;
import com.nbatch.job.core.biz.model.ExecuteFileToDbParam;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.utils.SpecialSqlUtil;

import java.sql.Connection;
import java.util.List;

import static com.nbatch.job.handler.enums.ExceptionCodeEnum.EXECUTE_UPDATE_SQL_FAIL;

/**
 * @description: GBase数据库方言
 * @author: Mr.ni
 * @date: 2025/11/19
 */
public class GBaseDialect implements BaseDialect {

    @Override
    public long fileToDb(Connection connection, ExecuteFileToDbParam param) throws Exception {
        String executeSql = generateFileToDbExecuteSql(param);
        return SpecialSqlUtil.executeUpdate(connection, executeSql);
    }

    @Override
    public boolean dbToFile(Connection connection, ExecuteDbToFileParam param) throws Exception {
        String setExportDirSql = "SET gbase_export_directory = 0";
        SpecialSqlUtil.execute(connection, setExportDirSql);
        String executeSql = generateDbToFileExecuteSql(param);
        return SpecialSqlUtil.execute(connection, executeSql);
    }

    @Override
    public int executeFunction(Connection connection, String tableSql, List<Object> params) throws Exception {
        return SpecialSqlUtil.executeSql(connection, tableSql, params);
    }

    /**
     * 生成gbase文件导入db的sql
     */
    private String generateFileToDbExecuteSql(ExecuteFileToDbParam param) {
        if (param == null) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "gbase文件导入db,参数不能为空");
        }
        if (StrUtil.isBlank(param.getFilePath())) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "gbase文件导入db,文件路径不能为空");
        }
        if (StrUtil.isBlank(param.getImportTableName())) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "gbase文件导入db,表名不能为空");
        }
        if (StrUtil.isBlank(param.getFileCode())) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "gbase文件导入db,文件编码不能为空");
        }
        String exportSql = "LOAD DATA INFILE " + "'" + param.getFilePath() + "'" +
                " INTO TABLE " + param.getImportTableName() +
                " character set " + param.getFileCode();
        if (param.getSep() != null && StrUtil.contains(param.getSep(), "'")) {
            exportSql += " FIELDS TERMINATED BY " + param.getSep();
        } else {
            exportSql += " FIELDS TERMINATED BY '" + param.getSep() + "'";
        }
        if (StrUtil.isNotBlank(param.getImportTableFiled())) {
            exportSql += " TABLE_FIELDS " + "'" + param.getImportTableFiled() + "'";
        }
        return exportSql;

    }

    /**
     * 生成gbase db导入文件的sql
     */
    private String generateDbToFileExecuteSql(ExecuteDbToFileParam param) {
        if (param == null) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "gbasedb导入文件,参数不能为空");
        }
        if (StrUtil.isBlank(param.getFilePath())) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "gbasedb导入文件,文件路径不能为空");
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

        executeSql.append(" INTO OUTFILE '")
                .append(param.getFilePath())
                .append("'").append(" FIELDS TERMINATED BY ' | '")
                .append(" LINES TERMINATED BY '\\n'")
                .append(" null_value ''");

        return executeSql.toString();

    }


}
