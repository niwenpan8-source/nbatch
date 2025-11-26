package com.nbatch.job.handler.handler.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.ExecuteDbToFileParam;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.handler.constant.JobHandlerPropertiesConstant;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.handler.JobHandlerAdapter;
import com.nbatch.job.handler.helper.DialectHelper;
import com.nbatch.job.handler.utils.NbatchFileUtil;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.Date;

import static com.nbatch.job.handler.constant.JobHandlerConstant.FILE_NAME_REPLACE_CHAR_DATE;
import static com.nbatch.job.handler.constant.JobHandlerConstant.FILE_NAME_REPLACE_NODE_ID;
import static com.nbatch.job.handler.constant.JobHandlerConstant.FILE_TYPE_SUFFIX_CSV;
import static com.nbatch.job.handler.constant.JobHandlerConstant.FILE_TYPE_SUFFIX_EXECUTE;
import static com.nbatch.job.handler.enums.ExceptionCodeEnum.DB_TO_FILE_FAIL;
import static com.nbatch.job.handler.enums.ExceptionCodeEnum.EXECUTE_UPDATE_SQL_FAIL;
import static com.nbatch.job.handler.enums.NodeTypeEnum.NODE_TYPE_FILE_TO_DB;

/**
 * @description: 文件导入数据库处理
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@RequiredArgsConstructor
public class DbToFileHandler implements JobHandlerAdapter {

    private final DialectHelper dialectHelper;

    private final JobHandlerPropertiesConstant handlerPropertiesConstant;

    @Override
    public boolean isSupport(String jobType) {
        return StrUtil.equals(NODE_TYPE_FILE_TO_DB.getCode(), jobType);
    }

    @Override
    public void execute(ExecuteNodeParam nodeParam) throws Exception {
        String tempPath = handlerPropertiesConstant.getTempPath();
        ExecuteDbToFileParam executeDbToFileParam = nodeParam.getExecuteDbToFileParam();
        String finishGenerateFileName = generateFileName(executeDbToFileParam.getFileName(), nodeParam.getNodeId(), nodeParam.getTurnDate());
        // 数据库导出文件名称
        String dbExportFilePath = tempPath + File.separator + finishGenerateFileName + FILE_TYPE_SUFFIX_CSV;
        // 最终生成文件名称
        String finishGenerateFilePath = tempPath + File.separator + finishGenerateFileName;
        // 创建文件
        FileUtil.touch(dbExportFilePath);
        executeDbToFileParam.setFilePath(dbExportFilePath);
        // 导出相关文件
        boolean flag = dialectHelper.getDialect(executeDbToFileParam.getDbType())
                .dbToFile(dialectHelper.getConnection(executeDbToFileParam.getDbType()), executeDbToFileParam);

        String executeFinishGenerateFilePath = finishGenerateFilePath + FILE_TYPE_SUFFIX_EXECUTE;
        // 将数据库导出文件进行压缩
        NbatchFileUtil.gzipFile(dbExportFilePath, executeFinishGenerateFilePath);
        File file = new File(executeFinishGenerateFilePath);
        // 将压缩文件进行重命名
        FileUtil.rename(file, finishGenerateFilePath, true);
        FileUtil.del(dbExportFilePath);
        if (!flag) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "数据库导出文件失败");
        }
    }

    /**
     * 生成文件名称
     */
    private String generateFileName(String templateName, String nodeId, Date date) {
        if (StrUtil.isEmpty(templateName)) {
            throw new HandlerException(DB_TO_FILE_FAIL.getCode(), "导出文件配置，文件名称不可为空");
        }
        String fileName = templateName;
        if (date != null) {
            String dateStr = DateUtil.format(date, DatePattern.NORM_DATE_FORMATTER);
            fileName = StrUtil.replace(fileName, FILE_NAME_REPLACE_CHAR_DATE, dateStr);
        }
        if (StrUtil.isNotBlank(nodeId)) {
            fileName = StrUtil.replace(fileName, FILE_NAME_REPLACE_NODE_ID, nodeId);
        }
        return fileName;
    }

}
