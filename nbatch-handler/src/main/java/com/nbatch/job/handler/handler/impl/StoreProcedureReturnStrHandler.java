package com.nbatch.job.handler.handler.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.handler.handler.JobNodeHandlerAdapter;
import com.nbatch.job.handler.helper.DialectHelper;
import lombok.RequiredArgsConstructor;

import static com.nbatch.job.core.enums.NodeTypeEnum.NODE_TYPE_STORE_PROCEDURE;

/**
 * @description: 存储过程
 * @author: Mr.ni
 * @date: 2025/11/27
 */
@RequiredArgsConstructor
public class StoreProcedureReturnStrHandler implements JobNodeHandlerAdapter {

    private final DialectHelper dialectHelper;

    @Override
    public boolean isSupport(String jobType) {
        return StrUtil.equals(NODE_TYPE_STORE_PROCEDURE.getCode(), jobType);
    }

    @Override
    public void execute(ExecuteNodeParam nodeParam) throws Exception {
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
    }
}
