package com.nbatch.job.handler.handler.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.DbType;
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
import java.util.List;

import static com.nbatch.job.handler.constant.JobHandlerConstant.FILE_TYPE_SUFFIX_CSV;
import static com.nbatch.job.handler.constant.JobHandlerConstant.TODAY_TABLE_SUFFIX;
import static com.nbatch.job.handler.enums.ExceptionCodeEnum.FILE_TO_DB_FAIL;
import static com.nbatch.job.handler.enums.NodeTypeEnum.NODE_TYPE_FILE_TO_DB;

/**
 * @description: 文件导入数据库处理
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
        String tempPath = handlerPropertiesConstant.getTempPath();
        JSONObject replaceObj;
        if (StrUtil.isBlank(param.getFileName())) {
            replaceObj = JSONUtil.parseObj(param.getFileNameParam());
        } else {
            replaceObj = new JSONObject();
        }
        replaceObj.putOpt("date", nodeParam.getTurnDate());
        String finishGenerateFileName = NbatchFileUtil.generateFileName(param.getFileName(), replaceObj);
        // 文件导入到数据库压缩文件名称
        String fileImportCompressDbPath = tempPath + File.separator + finishGenerateFileName;
        log.info("文件导入数据库压缩文件名称：{}", fileImportCompressDbPath);
        if (!FileUtil.exist(fileImportCompressDbPath)) {
            throw new HandlerException(FILE_TO_DB_FAIL.getCode(),
                    StrUtil.format("文件不存在:{}", fileImportCompressDbPath));
        }
        // 文件导入到数据库解压文件名称
        String fileImportDbPath = tempPath + File.separator + finishGenerateFileName + FILE_TYPE_SUFFIX_CSV;
        NbatchFileUtil.unGzipFile(fileImportCompressDbPath, fileImportDbPath);
        setFilePath(param, finishGenerateFileName, nodeParam.getDbType());
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
        int totalLines = FileUtil.getTotalLines(new File(fileImportDbPath));
        FileUtil.del(fileImportDbPath);
        NbatchFileUtil.checkImportDataNum(totalLines, importDbCount);
    }

    /**
     * 设置文件路径
     */
    private void setFilePath(ExecuteFileToDbParam param, String finishGenerateFileName, String dbType) {
        String dbExportFilePath = handlerPropertiesConstant.getTempPath()
                + File.separator + finishGenerateFileName + FILE_TYPE_SUFFIX_CSV;
        String remoteDbExportFilePath = handlerPropertiesConstant.getRemoteTempPath()
                + File.separator + finishGenerateFileName + FILE_TYPE_SUFFIX_CSV;
        param.setFilePath(dbExportFilePath);
        param.setRemoteFilePath(remoteDbExportFilePath);
    }


    private String getDeleteAllSql(String tableName) {
        return "truncate table " + tableName;
    }

    private String getDeleteByConditionSql(String tableName) {
        String templateSql = "delete from {}\n" +
                "where fundcode in (select fundcode from\n" +
                "    {}_today)";
        return StrUtil.format(templateSql, tableName, tableName);
    }

    /**
     * 获取导入正式表sql
     */
    private String getInsertFormalTable(String tableName, String tableFiled,
                                        String tempTableName) {
        return "insert into " + tableName + " (" + tableFiled + ")" +
                " select " + tableFiled + " from " + tempTableName;
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
                getDeleteByConditionSql(importTableName));
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
