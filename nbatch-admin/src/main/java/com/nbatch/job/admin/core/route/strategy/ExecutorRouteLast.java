package com.nbatch.job.admin.core.route.strategy;

import com.nbatch.job.admin.core.route.ExecutorRouter;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.TriggerParam;

import java.util.List;

/**
 *
 * @author Mr.ni
 * @date 2025/11/05
 */
public class ExecutorRouteLast extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        return new ReturnT<>(addressList.get(addressList.size() - 1));
    }

}
