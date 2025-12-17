package com.nbatch.job.handler.handler.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import com.nbatch.job.core.biz.model.ExecuteDbToFileParam;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.handler.constant.JobHandlerPropertiesConstant;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.handler.JobNodeHandlerAdapter;
import com.nbatch.job.handler.helper.DialectHelper;
import com.nbatch.job.handler.utils.NbatchFileUtil;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static com.nbatch.job.handler.constant.JobHandlerConstant.FILE_TYPE_SUFFIX_CSV;
import static com.nbatch.job.handler.constant.JobHandlerConstant.FILE_TYPE_SUFFIX_EXECUTE;
import static com.nbatch.job.handler.enums.ExceptionCodeEnum.EXECUTE_UPDATE_SQL_FAIL;
import static com.nbatch.job.handler.enums.NodeTypeEnum.NODE_TYPE_FILE_TO_DB;

/**
 * @description: 文件导入数据库处理
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@RequiredArgsConstructor
public class DbToFileHandler implements JobNodeHandlerAdapter {

    private final DialectHelper dialectHelper;

    private final JobHandlerPropertiesConstant handlerPropertiesConstant;

    @Override
    public boolean isSupport(String jobType) {
        return StrUtil.equals(NODE_TYPE_FILE_TO_DB.getCode(), jobType);
    }

    @Override
    public void execute(ExecuteNodeParam nodeParam) throws Exception {
        ExecuteDbToFileParam param = nodeParam.getExecuteDbToFileParam();
        String tempPath = handlerPropertiesConstant.getTempPath();

        JSONObject replaceObj = JSONUtil.parseObj(param.getFileNameParam());
        replaceObj.putOpt("date", nodeParam.getTurnDate());
        String finishGenerateFileName = NbatchFileUtil.generateFileName(param.getFileName(), replaceObj);
        // 数据库导出文件名称
        String dbExportFilePath = tempPath + File.separator + finishGenerateFileName + FILE_TYPE_SUFFIX_CSV;
        // 最终生成文件名称
        String finishGenerateFilePath = tempPath + File.separator + finishGenerateFileName;
        // 生成最终文件的中间文件
        String executeFinishGenerateFilePath = finishGenerateFilePath + FILE_TYPE_SUFFIX_EXECUTE;
        if (FileUtil.exist(dbExportFilePath)) {
            FileUtil.del(dbExportFilePath);
        }
        // 创建文件
        //FileUtil.touch(dbExportFilePath);
        setFilePath(param, finishGenerateFileName, nodeParam.getDbType());
        // 导出相关文件
        boolean flag = dialectHelper.getDialect(nodeParam.getDbType())
                .dbToFile(dialectHelper.getConnection(nodeParam.getDbType()), param);

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


    private void setFilePath(ExecuteDbToFileParam param, String finishGenerateFileName, String dbType) {
        String tempPath = handlerPropertiesConstant.getTempPath();
        if (StrUtil.equals(dbType, DbType.GBASE.getDb())) {
            tempPath = handlerPropertiesConstant.getRemoteTempPath();
        }
        String dbExportFilePath = tempPath + File.separator + finishGenerateFileName + FILE_TYPE_SUFFIX_CSV;
        param.setFilePath(dbExportFilePath);
    }

}
