package com.nbatch.job.admin.core.route.strategy;

import com.nbatch.job.admin.core.route.ExecutorRouter;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.TriggerParam;

import java.util.List;
import java.util.Random;

/**
 *
 * @author Mr.ni
 * @date 2025/11/05
 */
public class ExecutorRouteRandom extends ExecutorRouter {

    private static final Random LOCAL_RANDOM = new Random();

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        String address = addressList.get(LOCAL_RANDOM.nextInt(addressList.size()));
        return new ReturnT<>(address);
    }

}
