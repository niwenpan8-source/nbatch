package com.nbatch.job.handler.handler.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.handler.JobNodeHandlerAdapter;
import com.nbatch.job.handler.helper.DialectHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;

import static com.nbatch.job.core.enums.NodeTypeEnum.NODE_TYPE_STORE_PROCEDURE;
import static com.nbatch.job.handler.enums.ExceptionCodeEnum.EXECUTE_SQL_FAIL;

/**
 * 存储过程
 */
@RequiredArgsConstructor
@Log
public class StoreProcedureReturnStrHandler implements JobNodeHandlerAdapter {

    private final DialectHelper dialectHelper;

    private static final String EXECUTE_SQL_FAIL_CODE = "00000";

    @Override
    public boolean isSupport(String jobType) {
        return StrUtil.equals(NODE_TYPE_STORE_PROCEDURE.getCode(), jobType);
    }

    @Override
    public void execute(ExecuteNodeParam nodeParam) throws Exception {

        log.info("\n\n======================【开始执行存储过程作业节点】======================\n\n");
        log.info("存储过程作业节点名称【" + nodeParam.getExecuteContent()+"】");

        JSONObject paramObj;
        if (nodeParam.getExecuteContentParam() != null) {
            paramObj = new JSONObject(nodeParam.getExecuteContentParam());
        } else {
            paramObj = new JSONObject();
        }
        paramObj.putOpt("date", DateUtil.format(nodeParam.getTurnDate(), DatePattern.NORM_DATE_FORMAT));
        String result = dialectHelper.getDialect(nodeParam.getDbType())
                .executeStoreProcedureReturnStr(dialectHelper.getConnection(nodeParam.getDbType()),
                        nodeParam.getExecuteContent(), paramObj);

        // 将日志推送到详细日志表
        nodeParam.pushRunNodeLogDetailCallback(result);
        log.info(StrUtil.format("result:{}", result));
        if (!StrUtil.startWith(result,EXECUTE_SQL_FAIL_CODE)) {
            throw new HandlerException(EXECUTE_SQL_FAIL.getCode(), StrUtil.format("存储过程执行失败,存储过程名称:{},错误码为：{}",
                    nodeParam.getExecuteContent(), result));
        }

        log.info("\n\n======================结束执行存储过程作业节点======================\n\n{}");

    }
}
