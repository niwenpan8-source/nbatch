package com.nbatch.job.handler.dialect;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.nbatch.job.core.biz.model.ExecuteDbToFileParam;
import com.nbatch.job.core.biz.model.ExecuteFileToDbParam;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.utils.AsciiUtil;
import com.nbatch.job.handler.utils.SpecialSqlUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;

import static com.nbatch.job.handler.enums.ExceptionCodeEnum.EXECUTE_UPDATE_SQL_FAIL;

/**
 * @description: GBase数据库方言
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@Slf4j
public class GBaseDialect implements BaseDialect {

    @Override
    public long fileToDb(Connection connection, ExecuteFileToDbParam param) throws Exception {
        String executeSql = generateFileToDbExecuteSql(param);
        initImportFileFields(param);
        log.info("执行 file to db sql：{}", executeSql);
        return SpecialSqlUtil.executeUpdate(connection, executeSql);
    }

    @Override
    public boolean dbToFile(Connection connection, ExecuteDbToFileParam param) throws Exception {
        FileUtil.del(param.getFilePath());
        String setExportDirSql = "SET gbase_export_directory = 0";
        log.info("执行 set export sql：{}", setExportDirSql);
        SpecialSqlUtil.executeNotCloseConnect(connection, setExportDirSql);
        String executeSql = generateDbToFileExecuteSql(param);
        log.info("执行 db to file sql：{}", executeSql);
        SpecialSqlUtil.execute(connection, executeSql);
        return true;
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

    /**
     * 获取导入文件字段
     */
    private void initImportFileFields(ExecuteFileToDbParam param) {
        if (StrUtil.isEmpty(param.getImportTableFiled())) {
            return;
        }
        String separator = param.getSep();
        if (StrUtil.contains(separator, "X'")) {
            // 3. 处理原始字符串中的十六进制部分
            String hexPart = separator.replaceAll("X'|'", "");
            separator = AsciiUtil.hexToAscii(hexPart);
        }
        if (StrUtil.isEmpty(separator)) {
            separator = " | ";
        }
        int actualColumnSize = readCsvFirstSize(param.getRemoteFilePath(), separator);
        List<String> templateColumns = StrUtil.split(param.getImportTableFiled(), StrPool.COMMA);
        if (actualColumnSize > templateColumns.size()) {
            StringBuilder importTableFiled = new StringBuilder(param.getImportTableFiled());
            for (int i = 0; i < actualColumnSize - templateColumns.size(); i++) {
                importTableFiled.append(", " + "@c").append(i + 1);
            }
            param.setImportTableFiled(importTableFiled.toString());
        }
    }

    /**
     * 测试写入csv
     */
    public int readCsvFirstSize(String filePath, String separator) {
        try (
                FileInputStream fileInputStream = new FileInputStream(filePath);
                InputStreamReader isr = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(isr)
        ) {
            String firstLine = reader.readLine();
            //CSV格式文件为逗号分隔符文件，这里根据逗号切分
            List<String> itemList = StrUtil.split(firstLine, separator);
            return itemList.size();
        } catch (Exception e) {
            log.error("读取csv文件异常");
        }
        return 0;
    }

    /**
     * 生成gbase文件导入db的sql
     */
    private String generateFileToDbExecuteSql(ExecuteFileToDbParam param) {
        if (param == null) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "gbase文件导入db,参数不能为空");
        }
        if (StrUtil.isBlank(param.getRemoteFilePath())) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "gbase文件导入db,文件路径不能为空");
        }
        if (StrUtil.isBlank(param.getImportTableName())) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "gbase文件导入db,表名不能为空");
        }
        if (StrUtil.isBlank(param.getFileCode())) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "gbase文件导入db,文件编码不能为空");
        }
        String exportSql = "LOAD DATA INFILE " + "'" + param.getRemoteFilePath() + "'" +
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
        if (StrUtil.isBlank(param.getRemoteFilePath())) {
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
                .append(param.getRemoteFilePath())
                .append("'").append(" FIELDS TERMINATED BY ' | '").append("'")
                .append(StrUtil.isEmpty(param.getSep()) ? "|" : param.getSep())
                .append("'")
                .append(" LINES TERMINATED BY '\\n'")
                .append(" null_value ''");

        return executeSql.toString();

    }


}
