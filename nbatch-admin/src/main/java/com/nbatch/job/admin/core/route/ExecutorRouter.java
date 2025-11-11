package com.nbatch.job.admin.core.route;

import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.TriggerParam;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 *
 * @author Mr.ni
 * @date 2025/11/05
 */
public abstract class ExecutorRouter {

    /**
     * route address
     *
     * @param addressList route address list
     * @return  ReturnT.content=address
     */
    public abstract ReturnT<String> route(TriggerParam triggerParam, List<String> addressList);

}
