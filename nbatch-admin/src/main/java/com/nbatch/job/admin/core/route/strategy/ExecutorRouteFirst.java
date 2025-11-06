package com.nbatch.job.admin.core.route.strategy;

import com.nbatch.job.admin.core.route.ExecutorRouter;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.TriggerParam;

import java.util.List;

/**
 * @author Mr.ni
 */
public class ExecutorRouteFirst extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList){
        return new ReturnT<>(addressList.get(0));
    }

}
