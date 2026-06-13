package com.nbatch.job.admin.core.executor;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.admin.core.scheduler.JobScheduler;
import com.nbatch.job.core.biz.ExecutorBiz;
import com.nbatch.job.core.biz.model.IdleBeatParam;
import com.nbatch.job.core.biz.model.KillParam;
import com.nbatch.job.core.biz.model.LogParam;
import com.nbatch.job.core.biz.model.LogResult;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.RunNodeLogAckParam;
import com.nbatch.job.core.biz.model.RunNodeLogPullParam;
import com.nbatch.job.core.biz.model.RunNodeLogPullResult;
import com.nbatch.job.core.biz.model.StopRunNodeParam;
import com.nbatch.job.core.biz.model.TriggerParam;
import com.nbatch.job.core.constant.HandleCodeConstant;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecutorBizProxy {

    private ExecutorBizProxy() {
    }

    public static ReturnT<String> beat(String address) {
        return call(address, "beat", ExecutorBiz::beat);
    }

    public static ReturnT<String> idleBeat(String address, IdleBeatParam idleBeatParam) {
        return call(address, "idleBeat", executorBiz -> executorBiz.idleBeat(idleBeatParam));
    }

    public static ReturnT<String> run(String address, TriggerParam triggerParam) {
        return call(address, "run", executorBiz -> executorBiz.run(triggerParam));
    }

    public static ReturnT<String> kill(String address, KillParam killParam) {
        return call(address, "kill", executorBiz -> executorBiz.kill(killParam));
    }

    public static ReturnT<String> stopRunNode(String address, StopRunNodeParam stopRunNodeParam) {
        return call(address, "stopRunNode", executorBiz -> executorBiz.stopRunNode(stopRunNodeParam));
    }

    public static ReturnT<LogResult> log(String address, LogParam logParam) {
        return call(address, "log", executorBiz -> executorBiz.log(logParam));
    }

    public static ReturnT<RunNodeLogPullResult> pullRunNodeLog(String address, RunNodeLogPullParam pullParam) {
        return call(address, "pullRunNodeLog", executorBiz -> executorBiz.pullRunNodeLog(pullParam));
    }

    public static ReturnT<String> ackRunNodeLog(String address, RunNodeLogAckParam ackParam) {
        return call(address, "ackRunNodeLog", executorBiz -> executorBiz.ackRunNodeLog(ackParam));
    }

    private static <T> ReturnT<T> call(String address, String action, ExecutorCall<T> executorCall) {
        if (StrUtil.isBlank(address)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "executor address is blank");
        }
        try {
            ExecutorBiz executorBiz = JobScheduler.getExecutorBiz(address);
            if (executorBiz == null) {
                return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "executor client not found");
            }
            ReturnT<T> result = executorCall.call(executorBiz);
            return result == null ? new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "executor rpc result is null") : result;
        } catch (Exception e) {
            log.warn("executor rpc failed, address:{}, action:{}", address, action, e);
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, e.getMessage());
        }
    }

    private interface ExecutorCall<T> {
        ReturnT<T> call(ExecutorBiz executorBiz) throws Exception;
    }
}
