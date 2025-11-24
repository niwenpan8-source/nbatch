package com.nbatch.job.handler.handler.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.nbatch.job.handler.domain.param.JobWorkImportFileParam;
import com.nbatch.job.handler.enums.DbTypeEnum;
import com.nbatch.job.handler.handler.JobHandlerAdapter;
import com.nbatch.job.handler.helper.DialectHelper;
import com.nbatch.job.handler.thread.BatchRunnable;
import com.nbatch.job.handler.thread.BatchThreadPoolExecutor;
import com.nbatch.job.handler.thread.BatchThreadPoolUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static com.nbatch.job.handler.enums.NodeTypeEnum.NODE_TYPE_FILE_TO_DB;

/**
 * @description: 文件导入数据库处理
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@Component
@RequiredArgsConstructor
public class FileToDbHandler implements JobHandlerAdapter {

    private final DialectHelper dialectHelper;

    private final BatchThreadPoolExecutor batchThreadPoolExecutor
            = BatchThreadPoolUtil.newThreadPoolExecutorDiscard(NODE_TYPE_FILE_TO_DB.getCode(),
            1, 1, 30, TimeUnit.MINUTES, 1000);

    @Override
    public boolean isSupport(String jobType) {
        return StrUtil.equals(NODE_TYPE_FILE_TO_DB.getCode(), jobType);
    }

    @Override
    public void execute(JSONObject executeParam) {
        batchThreadPoolExecutor.executeBatch(new BatchRunnable(new JSONObject()
                .putOpt("workId", executeParam.getStr("workId")).putOpt("nodeId", executeParam.getStr("nodeId"))) {
            @Override
            public void run() {
                JobWorkImportFileParam param = BeanUtil.toBean(executeParam, JobWorkImportFileParam.class);
                dialectHelper.getDialect(DbTypeEnum.GBASE)
                        .fileToDb(dialectHelper.getConnection(DbTypeEnum.GBASE), param);
            }
        });

    }

}
