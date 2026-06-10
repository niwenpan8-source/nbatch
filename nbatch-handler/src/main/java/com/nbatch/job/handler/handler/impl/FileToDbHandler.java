package com.nbatch.job.handler.handler.impl;

import cn.hutool.core.bean.BeanUtil;
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

import static com.nbatch.job.core.enums.NodeTypeEnum.NODE_TYPE_FILE_TO_DB;
import static com.nbatch.job.handler.constant.JobHandlerConstant.TODAY_TABLE_SUFFIX;
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
        ExecuteFileToDbParam param = nodeParam.getExecuteFileToDbParam();
        String fileToDbPath = handlerPropertiesConstant.getTempPath();
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
        BaseDialect dialect = dialectHelper.getDialect(nodeParam.getDbType());
        // 导入的逻辑首先将数据导入到中间表，然后进行数据校验
        String importTableName = param.getImportTableName();
        String importTodayTableName = importTableName + TODAY_TABLE_SUFFIX;
        long importDbCount;
        // 全量导入
        if (param.getAllUpdate() == 1) {
            String deleteAllSql = getDeleteAllSql(importTableName);
            dialect.executeUpdate(dialectHelper.getConnection(nodeParam.getDbType()), deleteAllSql);
            importDbCount = dialect.fileToDb(dialectHelper.getConnection(nodeParam.getDbType()), param);
        } else {
            importDbCount = handleUpdateTable(importTableName, importTodayTableName, param, dialect, nodeParam.getDbType());
        }
        int totalLines = FileUtil.getTotalLines(new File(importDbFilePath));
        NbatchFileUtil.checkImportDataNum(totalLines, importDbCount);
    }

    /**
     * 设置文件路径
     */
    private void setFilePath(ExecuteFileToDbParam param, String finishGenerateFileName) {
        String dbExportFilePath = handlerPropertiesConstant.getTempPath()
                + File.separator + finishGenerateFileName;
        String remoteDbExportFilePath = handlerPropertiesConstant.getRemoteTempPath()
                + File.separator + finishGenerateFileName;
        param.setFilePath(dbExportFilePath);
        param.setRemoteFilePath(remoteDbExportFilePath);
    }

    private String getDeleteAllSql(String tableName) {
        return "truncate table " + tableName;
    }


    private String getDeleteByConditionSql(String tableName, String importTableCondition) {
        // 将逗号分隔的字段串转换为数组，用于构建 EXISTS 中的关联条件
        String[] fields = importTableCondition.split(",");

        StringBuilder whereClause = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            String field = fields[i].trim();
            if (i > 0) {
                whereClause.append(" AND ");
            }
            // 构建 t1.FIELD = t2.FIELD 的格式
            whereClause.append("t1.").append(field).append(" = t2.").append(field);
        }

        String templateSql = "DELETE FROM {} t1\n" +
                "WHERE EXISTS (\n" +
                "    SELECT 1 FROM {}_today t2\n" +
                "    WHERE {}\n" +
                ")";

        String formatted = StrUtil.format(templateSql, tableName, tableName, whereClause.toString());
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
                                   String dbType
    ) throws Exception {
        long importDbCount;
        // 删除临时表数据
        String deleteAllSql = getDeleteAllSql(importTodayTableName);
        dialect.executeUpdate(dialectHelper.getConnection(dbType), deleteAllSql);
        ExecuteFileToDbParam copyParam = BeanUtil.toBean(param, ExecuteFileToDbParam.class);
        copyParam.setImportTableName(importTodayTableName);
        importDbCount = dialect.fileToDb(dialectHelper.getConnection(dbType), copyParam);
        String importTableCondition = param.getImportTableCondition();
        if (StrUtil.isEmpty(importTableCondition)) {
            throw new HandlerException(FILE_TO_DB_FAIL.getCode(), "导入文件表条件列不可为空！");
        }
        dialect.executeUpdate(dialectHelper.getConnection(dbType),
                getDeleteByConditionSql(importTableName, param.getImportTableCondition()));
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


}
