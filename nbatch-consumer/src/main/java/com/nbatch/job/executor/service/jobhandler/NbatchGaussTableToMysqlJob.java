package com.nbatch.job.executor.service.jobhandler;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.handler.annotation.BatchJob;
import com.nbatch.job.executor.service.helper.GaussTableToMysqlHelper;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.handler.BeanHandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.nbatch.job.handler.enums.ExceptionCodeEnum.EXECUTE_UPDATE_SQL_FAIL;

/**
 * 通过表名将高斯数据库表迁移到MySQL
 * @author: Mr.ni
 * @date: 2026/1/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NbatchGaussTableToMysqlJob {

    private final GaussTableToMysqlHelper gaussTableToMysqlHelper;

    /**
     * 执行lua脚本
     */
    @BatchJob(value = "gaussTableToMysql")
    public void gaussTableToMysql() {
        ExecuteNodeParam param = BeanHandlerContext.getBeanThreadLocal();
        if (param == null || StrUtil.isEmpty(param.getExecuteContentParam())) {
            log.warn("Lua script path is empty.");
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), "Lua 脚本路径为空");
        }
        try {
            String tableName = param.getExecuteContentParam();
            gaussTableToMysqlHelper.syncTableStructure(tableName);
        } catch (Exception e) {
            param.pushRunNodeLogDetailCallback(e.getMessage());
            throw new HandlerException(EXECUTE_UPDATE_SQL_FAIL.getCode(), e);
        }
    }


}
