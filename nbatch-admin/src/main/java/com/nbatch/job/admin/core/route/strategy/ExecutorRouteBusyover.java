package com.nbatch.job.admin.core.route.strategy;

import com.nbatch.job.admin.core.scheduler.XxlJobScheduler;
import com.nbatch.job.admin.core.route.ExecutorRouter;
import com.nbatch.job.admin.core.util.I18nUtil;
import com.nbatch.job.core.biz.ExecutorBiz;
import com.nbatch.job.core.biz.model.IdleBeatParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.TriggerParam;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author Mr.ni
 */
@Slf4j
public class ExecutorRouteBusyover extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        StringBuilder idleBeatResultBuilder = new StringBuilder();
        for (String address : addressList) {
            // beat
            ReturnT<String> idleBeatResult;
            try {
                ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(address);
                assert executorBiz != null;
                idleBeatResult = executorBiz.idleBeat(new IdleBeatParam(triggerParam.getJobId()));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                idleBeatResult = new ReturnT<>(ReturnT.FAIL_CODE, "" + e);
            }
            idleBeatResultBuilder.append((idleBeatResultBuilder.length() > 0) ? "<br><br>" : "")
                    .append(I18nUtil.getString("jobconf_idleBeat"))
                    .append("：")
                    .append("<br>address：").append(address)
                    .append("<br>code：").append(idleBeatResult.getCode())
                    .append("<br>msg：").append(idleBeatResult.getMsg());

            // beat success
            if (idleBeatResult.getCode() == ReturnT.SUCCESS_CODE) {
                idleBeatResult.setMsg(idleBeatResultBuilder.toString());
                idleBeatResult.setContent(address);
                return idleBeatResult;
            }
        }

        return new ReturnT<>(ReturnT.FAIL_CODE, idleBeatResultBuilder.toString());
    }

}
