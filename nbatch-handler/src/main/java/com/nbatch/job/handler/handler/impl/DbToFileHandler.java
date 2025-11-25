package com.nbatch.job.handler.handler.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.ExecuteDbToFileParam;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.handler.enums.DbTypeEnum;
import com.nbatch.job.handler.handler.JobHandlerAdapter;
import com.nbatch.job.handler.helper.DialectHelper;
import lombok.RequiredArgsConstructor;

import static com.nbatch.job.handler.enums.NodeTypeEnum.NODE_TYPE_FILE_TO_DB;

/**
 * @description: 文件导入数据库处理
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@RequiredArgsConstructor
public class DbToFileHandler implements JobHandlerAdapter {

    private final DialectHelper dialectHelper;

    @Override
    public boolean isSupport(String jobType) {
        return StrUtil.equals(NODE_TYPE_FILE_TO_DB.getCode(), jobType);
    }

    @Override
    public void execute(ExecuteNodeParam nodeParam) throws Exception {
        // todo 临时文件路径
        String tempFilePath = "C:/disk/project/work/2025/nbatch/file/123.csv";
        FileUtil.touch(tempFilePath);
        ExecuteDbToFileParam executeDbToFileParam = nodeParam.getExecuteDbToFileParam();
        executeDbToFileParam.setFilePath(tempFilePath);
        dialectHelper.getDialect(executeDbToFileParam.getDbType())
                .dbToFile(dialectHelper.getConnection(executeDbToFileParam.getDbType()), executeDbToFileParam);
    }

}
