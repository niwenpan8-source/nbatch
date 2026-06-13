package com.nbatch.job.admin.core.route.strategy;

import com.nbatch.job.admin.core.executor.ExecutorBizProxy;
import com.nbatch.job.admin.core.route.ExecutorRouter;
import com.nbatch.job.admin.core.util.I18nUtil;
import com.nbatch.job.core.biz.model.IdleBeatParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.TriggerParam;
import com.nbatch.job.core.constant.HandleCodeConstant;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 执行路由策略-忙碌转移
 */
@Slf4j
public class ExecutorRouteBusyover extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        StringBuilder idleBeatResultBuilder = new StringBuilder();
        for (String address : addressList) {
            // beat
            ReturnT<String> idleBeatResult = ExecutorBizProxy.idleBeat(address, new IdleBeatParam(triggerParam.getJobId()));
            idleBeatResultBuilder.append((idleBeatResultBuilder.length() > 0) ? "<br><br>" : "")
                    .append(I18nUtil.getString("jobconf_idleBeat"))
                    .append("：")
                    .append("<br>address：").append(address)
                    .append("<br>code：").append(idleBeatResult.getCode())
                    .append("<br>msg：").append(idleBeatResult.getMsg());

            // beat success
            if (idleBeatResult.getCode() == HandleCodeConstant.HANDLE_CODE_SUCCESS) {
                idleBeatResult.setMsg(idleBeatResultBuilder.toString());
                idleBeatResult.setContent(address);
                return idleBeatResult;
            }
        }

        return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, idleBeatResultBuilder.toString());
    }

}
