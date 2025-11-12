package com.nbatch.job.admin.core.route.strategy;

import com.nbatch.job.admin.core.scheduler.JobScheduler;
import com.nbatch.job.admin.core.route.ExecutorRouter;
import com.nbatch.job.admin.core.util.I18nUtil;
import com.nbatch.job.core.biz.ExecutorBiz;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.TriggerParam;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 *
 * @author Mr.ni
 * @date 2025/11/05
 */
@Slf4j
public class ExecutorRouteFailover extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {

        StringBuilder beatResultBuilder = new StringBuilder();
        for (String address : addressList) {
            // beat
            ReturnT<String> beatResult;
            try {
                ExecutorBiz executorBiz = JobScheduler.getExecutorBiz(address);
                assert executorBiz != null;
                beatResult = executorBiz.beat();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                beatResult = new ReturnT<>(ReturnT.FAIL_CODE, "" + e);
            }
            beatResultBuilder.append((beatResultBuilder.length() > 0) ? "<br><br>" : "")
                    .append(I18nUtil.getString("jobconf_beat"))
                    .append("：")
                    .append("<br>address：").append(address)
                    .append("<br>code：").append(beatResult.getCode())
                    .append("<br>msg：").append(beatResult.getMsg());

            // beat success
            if (beatResult.getCode() == ReturnT.SUCCESS_CODE) {

                beatResult.setMsg(beatResultBuilder.toString());
                beatResult.setContent(address);
                return beatResult;
            }
        }
        return new ReturnT<>(ReturnT.FAIL_CODE, beatResultBuilder.toString());

    }
}
