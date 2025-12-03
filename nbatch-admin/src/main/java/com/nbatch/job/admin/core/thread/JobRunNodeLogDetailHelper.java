package com.nbatch.job.admin.core.thread;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogDetailPo;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.RunNodeLogDetailParam;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * job lose-monitor instance
 *
 * @author Mr.ni
 */
@Slf4j
public class JobRunNodeLogDetailHelper {

    private static final JobRunNodeLogDetailHelper INSTANCE = new JobRunNodeLogDetailHelper();

    public static JobRunNodeLogDetailHelper getInstance() {
        return INSTANCE;
    }

    // ---------------------- call back run node log detail ----------------------

    private ThreadPoolExecutor callbackRunNodeLogDetailThreadPool = null;

    public void start() {

        // for callback
        callbackRunNodeLogDetailThreadPool = new ThreadPoolExecutor(
                2,
                20,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(3000),
                r -> new Thread(r, "job, admin JobRunNodeLogDetailHelper-callbackRunNodeLogDetailThreadPool-" + r.hashCode()),
                (r, executor) -> {
                    r.run();
                    log.warn(">>>>>>>>>>> job, callback too fast, match threadpool rejected handler(run now).");
                });

    }

    public void toStop() {
        // stop callbackRunNodeLogDetailThreadPool
        callbackRunNodeLogDetailThreadPool.shutdownNow();
    }


    // ---------------------- helper ----------------------

    public ReturnT<String> callbackRunNodeLogDetail(List<RunNodeLogDetailParam> callbackParamList) {

        callbackRunNodeLogDetailThreadPool.execute(() -> {
            for (RunNodeLogDetailParam runNodeLogDetailParam : callbackParamList) {
                ReturnT<String> callbackResult = callbackRunNodeLogDetail(runNodeLogDetailParam);
                log.debug(">>>>>>>>> JobApiController.callback {}, handleCallbackParam={}, callbackResult={}",
                        (callbackResult.getCode() == ReturnT.SUCCESS_CODE ? "success" : "fail"), runNodeLogDetailParam, callbackResult);
            }
        });

        return ReturnT.SUCCESS;
    }

    private ReturnT<String> callbackRunNodeLogDetail(RunNodeLogDetailParam runNodeLogDetailParam) {
        JobWorkRunNodeLogDetailPo logDetailPo = BeanUtil.toBean(runNodeLogDetailParam, JobWorkRunNodeLogDetailPo.class);
        logDetailPo.setCallBackTime(DateUtil.date());
        JobAdminConfig.getAdminConfig().getLogDetailMapper().insert(logDetailPo);
        return ReturnT.SUCCESS;
    }


}
