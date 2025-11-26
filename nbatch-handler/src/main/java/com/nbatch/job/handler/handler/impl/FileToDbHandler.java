package com.nbatch.job.handler.handler.impl;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.ExecuteFileToDbParam;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.handler.constant.JobHandlerPropertiesConstant;
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
public class FileToDbHandler implements JobHandlerAdapter {

    private final DialectHelper dialectHelper;

    private final JobHandlerPropertiesConstant handlerPropertiesConstant;

    @Override
    public boolean isSupport(String jobType) {
        return StrUtil.equals(NODE_TYPE_FILE_TO_DB.getCode(), jobType);
    }

    @Override
    public void execute(ExecuteNodeParam nodeParam) throws Exception {
        ExecuteFileToDbParam param = nodeParam.getExecuteFileToDbParam();
        dialectHelper.getDialect(param.getDbType())
                .fileToDb(dialectHelper.getConnection(param.getDbType()), param);
    }

}
