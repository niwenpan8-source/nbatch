package com.nbatch.job.handler.handler.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nbatch.job.core.biz.model.ExecuteFileToDbParam;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.handler.constant.JobHandlerPropertiesConstant;
import com.nbatch.job.handler.dialect.BaseDialect;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.handler.JobNodeHandlerAdapter;
import com.nbatch.job.handler.helper.DialectHelper;
import com.nbatch.job.handler.utils.NbatchFileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nbatch.job.core.enums.NodeTypeEnum.NODE_TYPE_FILE_TO_DB;
import static com.nbatch.job.handler.enums.ExceptionCodeEnum.FILE_TO_DB_FAIL;

/**
 * 文件导入数据库处理
 *
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@Slf4j
@RequiredArgsConstructor
public class FileToDbHandler implements JobNodeHandlerAdapter {

    private final DialectHelper dialectHelper;

    private final JobHandlerPropertiesConstant handlerPropertiesConstant;

    @Override
    public boolean isSupport(String jobType) {
        return StrUtil.equals(NODE_TYPE_FILE_TO_DB.getCode(), jobType);
    }

    @Override
    public void execute(ExecuteNodeParam nodeParam) throws Exception {

        log.info("\n\n======================开始执行文件导入数据库节点，表名称【{}】======================", nodeParam.getExecuteFileToDbParam().getFileName());
        long startTime = System.currentTimeMillis();
        ExecuteFileToDbParam param = nodeParam.getExecuteFileToDbParam();
        String fileToDbPath = handlerPropertiesConstant.getPath();
        JSONObject replaceObj;
        if (StrUtil.isBlank(param.getFileName())) {
            replaceObj = JSONUtil.parseObj(param.getFileNameParam());
        } else {
            replaceObj = new JSONObject();
        }
        replaceObj.putOpt("date", nodeParam.getTurnDate());
        String importFileName = NbatchFileUtil.generateFileName(param.getFileName(), replaceObj);
        // 文件导入到数据库文件名称
        String importDbFilePath = fileToDbPath + File.separator + importFileName;
        log.info("文件导入文件名称：{}", importDbFilePath);
        if (!FileUtil.exist(importDbFilePath)) {
            throw new HandlerException(FILE_TO_DB_FAIL.getCode(),
                    StrUtil.format("文件不存在:{}", importDbFilePath));
        }
        // 文件导入到数据库解压文件名称
        setFilePath(param, importFileName);
        // 是否压缩：1压缩 0不压缩
        String tempFilePath = importDbFilePath + ".temp";
        if (param.getIsGzip() == 1) {
            String tempFileName = importFileName + ".temp";
            NbatchFileUtil.unGzipFile(param.getFilePath(), tempFilePath);
            setFilePath(param, tempFileName);
        }
        BaseDialect dialect = dialectHelper.getDialect(nodeParam.getDbType());
        // 导入的逻辑首先将数据导入到中间表，然后进行数据校验
        String importTableName = param.getImportTableName();
        String importTodayTableName = StrUtil.format("{}_{}", importTableName, nodeParam.getNodeId());
        dialect.dropTable(dialectHelper.getConnection(nodeParam.getDbType()), importTodayTableName);
        // 全量导入
        if (param.getAllUpdate() == 1) {
            dialect.copyTableStructure(dialectHelper.getConnection(nodeParam.getDbType()), importTableName, importTodayTableName);
            handleAllTable(importTableName, importTodayTableName, param, dialect, nodeParam.getDbType());
        } else {
            dialect.copyTableNotStructure(dialectHelper.getConnection(nodeParam.getDbType()), importTableName, importTodayTableName);
            handleUpdateTable(importTableName, importTodayTableName, param, dialect, nodeParam.getDbType(), replaceObj);
        }

        if (param.getIsGzip() == 1) {
            FileUtil.del(tempFilePath);
        }

        long endTime = System.currentTimeMillis();
        nodeParam.pushRunNodeLogDetailCallback(StrUtil.format("【文件导入数据库节点执行耗时】：{}ms", endTime - startTime));
        // dialect.dropTable(dialectHelper.getConnection(nodeParam.getDbType()), importTodayTableName);

        log.info("\n\n======================结束执行文件导入数据库节点，表名称【{}】======================", param.getImportTableName());
    }

    /**
     * 设置文件路径
     */
    private void setFilePath(ExecuteFileToDbParam param, String finishGenerateFileName) {
        String dbExportFilePath = handlerPropertiesConstant.getPath()
                + File.separator + finishGenerateFileName;
        String remoteDbExportFilePath = handlerPropertiesConstant.getRemotePath()
                + File.separator + finishGenerateFileName;
        param.setFilePath(dbExportFilePath);
        param.setRemoteFilePath(remoteDbExportFilePath);
    }

    private String getDeleteAllSql(String tableName) {
        return "truncate table " + tableName;
    }


    private String getDeleteByConditionSql(String tableName, String todayTableName, String importTableCondition, JSONObject replaceObj) {
        // 使用正则检测是否包含 #{xxx} 格式的占位符
        Pattern placeholderPattern = Pattern.compile("#\\{(\\w+)\\}");
        Matcher matcher = placeholderPattern.matcher(importTableCondition);

        String templateSql;
        String formatted;
        String whereClause;
        if (matcher.find()) {
            templateSql = "DELETE FROM {} t1\n" +
                    "WHERE {}";
            // 存在占位符：使用 replaceObj 中的值替换所有 #{} 占位符，直接作为条件
            StringBuffer sb = new StringBuffer();
            matcher.reset();
            while (matcher.find()) {
                String key = matcher.group(1);
                Object value = replaceObj.get(key);
                String valueStr = "";
                if (value instanceof Date) {
                    valueStr = DateUtil.format((Date) value, DatePattern.NORM_DATE_FORMAT);
                }
                matcher.appendReplacement(sb, StrUtil.format("'{}'", value != null ? valueStr : ""));
            }
            matcher.appendTail(sb);
            whereClause = sb.toString();
            formatted = StrUtil.format(templateSql, tableName, whereClause);
        } else {
            templateSql = "DELETE FROM {} t1\n" +
                    "WHERE EXISTS (\n" +
                    "    SELECT 1 FROM {} t2\n" +
                    "    WHERE {}\n" +
                    ")";
            // 不存在占位符：原逻辑，将逗号分隔的字段串转换为 t1.FIELD = t2.FIELD 的关联条件
            String[] fields = importTableCondition.split(",");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < fields.length; i++) {
                String field = fields[i].trim();
                if (i > 0) {
                    sb.append(" AND ");
                }
                sb.append("t1.").append(field).append(" = t2.").append(field);
            }
            whereClause = sb.toString();
            formatted = StrUtil.format(templateSql, tableName, todayTableName, whereClause);
        }
        log.info("删除临时表数据sql:{}", formatted);
        return formatted;
    }


    /**
     * 获取导入正式表sql
     */
    private String getInsertFormalTable(String tableName, String tableFiled,
                                        String tempTableName) {

        String formart = "insert into " + tableName + " (" + tableFiled + ")" +
                " select " + tableFiled + " from " + tempTableName;
        log.info("导入正式表sql:{}", formart);
        return formart;
    }


    /**
     * 处理增量数据
     */
    private long handleUpdateTable(String importTableName,
                                   String importTodayTableName,
                                   ExecuteFileToDbParam param,
                                   BaseDialect dialect,
                                   String dbType,
                                   JSONObject replaceObj
    ) throws Exception {
        long importDbCount;
        // 删除临时表数据
        String deleteAllSql = getDeleteAllSql(importTodayTableName);
        dialect.executeUpdate(dialectHelper.getConnection(dbType), deleteAllSql);
        ExecuteFileToDbParam copyParam = BeanUtil.toBean(param, ExecuteFileToDbParam.class);
        copyParam.setImportTableName(importTodayTableName);
        importDbCount = dialect.fileToDb(dialectHelper.getConnection(dbType), copyParam);
        int totalLines = FileUtil.getTotalLines(new File(param.getFilePath()));
        NbatchFileUtil.checkImportDataNum(totalLines, importDbCount);
        String importTableCondition = param.getImportTableCondition();
        if (StrUtil.isEmpty(importTableCondition)) {
            throw new HandlerException(FILE_TO_DB_FAIL.getCode(), "导入文件表条件列不可为空！");
        }
        dialect.executeUpdate(dialectHelper.getConnection(dbType),
                getDeleteByConditionSql(importTableName, importTodayTableName, param.getImportTableCondition(), replaceObj));
        String tableFiled = param.getImportTableFiled();
        if (StrUtil.isEmpty(tableFiled)) {
            log.info("===================导入增量文件导入文件表列不可为空！==================");
            throw new HandlerException(FILE_TO_DB_FAIL.getCode(), "导入文件表列不可为空！");
        }

        String insertFormalTable = getInsertFormalTable(importTableName, tableFiled, importTodayTableName);
        dialect.executeUpdate(dialectHelper.getConnection(dbType),
                insertFormalTable);
        return importDbCount;

    }

    /**
     * 处理全量导入数据
     */
    private long handleAllTable(String importTableName,
                                String importTodayTableName,
                                ExecuteFileToDbParam param,
                                BaseDialect dialect,
                                String dbType
    ) throws Exception {
        long importDbCount;
        // 将所有数据导入到临时表
        ExecuteFileToDbParam copyParam = BeanUtil.toBean(param, ExecuteFileToDbParam.class);
        copyParam.setImportTableName(importTodayTableName);
        importDbCount = dialect.fileToDb(dialectHelper.getConnection(dbType), copyParam);
        int totalLines = FileUtil.getTotalLines(new File(param.getFilePath()));
        NbatchFileUtil.checkImportDataNum(totalLines, importDbCount);
        // 删除原表
        dialect.dropTable(dialectHelper.getConnection(dbType), importTableName);
        // 将临时表修改为原表名
        dialect.renameTable(dialectHelper.getConnection(dbType), importTodayTableName, importTableName);
        return importDbCount;
    }

}