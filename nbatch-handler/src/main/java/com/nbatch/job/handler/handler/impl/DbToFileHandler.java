package com.nbatch.job.handler.handler.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.nbatch.job.core.biz.model.ExecuteDbToFileParam;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.handler.constant.JobHandlerPropertiesConstant;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.handler.JobNodeHandlerAdapter;
import com.nbatch.job.handler.helper.DialectHelper;
import com.nbatch.job.handler.utils.NbatchFileUtil;
import lombok.RequiredArgsConstructor;

import java.io.File;

import static com.nbatch.job.core.enums.NodeTypeEnum.NODE_TYPE_DB_TO_FILE;
import static com.nbatch.job.handler.constant.JobHandlerConstant.FILE_TYPE_SUFFIX_EXECUTE;
import static com.nbatch.job.handler.enums.ExceptionCodeEnum.EXECUTE_UPDATE_SQL_FAIL;

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
        return StrUtil.equals(NODE_TYPE_DB_TO_FILE.getCode(), jobType);
    }

    @Override
    public void execute(ExecuteNodeParam nodeParam) throws Exception {
        ExecuteDbToFileParam param = nodeParam.getExecuteDbToFileParam();
        // 数据库导入到文件操作文件夹
        String dbToFilePath = handlerPropertiesConstant.getTempPath();
        JSONObject replaceObj;
        if (StrUtil.isBlank(param.getFileName())) {
            replaceObj = JSONUtil.parseObj(param.getFileNameParam());
        } else {
            replaceObj = new JSONObject();
        }
        replaceObj.putOpt("date", nodeParam.getTurnDate());
        String finishGenerateFileName = NbatchFileUtil.generateFileName(param.getFileName(), replaceObj);

        // 生成最终文件的中间文件
        String executeFinishGenerateFileName = finishGenerateFileName + FILE_TYPE_SUFFIX_EXECUTE;

        // 创建文件
        setFilePath(param, executeFinishGenerateFileName);
        // gauss 数据库没有文件的话他会报错
        // FileUtil.touch(param.getFilePath());
        // 导出相关文件
        boolean flag = dialectHelper.getDialect(nodeParam.getDbType())
                .dbToFile(dialectHelper.getConnection(nodeParam.getDbType()), param);

        File file = new File(dbToFilePath, executeFinishGenerateFileName);
        // 将压缩文件进行重命名
        FileUtil.rename(file, dbToFilePath + File.separator + finishGenerateFileName, true);
        // 是否压缩：1压缩 0不压缩
        if (param.getIsGzip() == 1) {
            NbatchFileUtil.gzipFile(dbToFilePath + File.separator + finishGenerateFileName);
        }
        if (!flag) {
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "数据库导出文件失败");
        }
    }

    /**
     * 对于正在导出的文件
     */
    private void setFilePath(ExecuteDbToFileParam param, String executeFinishGenerateFilePath) {
        String dbExportFilePath = handlerPropertiesConstant.getTempPath()
                + File.separator + executeFinishGenerateFilePath;
        String remoteDbExportFilePath = handlerPropertiesConstant.getRemoteTempPath()
                + File.separator + executeFinishGenerateFilePath;
        param.setFilePath(dbExportFilePath);
        param.setRemoteFilePath(remoteDbExportFilePath);
    }

}
