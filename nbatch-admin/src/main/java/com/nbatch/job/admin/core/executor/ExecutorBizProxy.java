package com.nbatch.job.admin.core.executor;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.admin.core.scheduler.JobScheduler;
import com.nbatch.job.core.biz.ExecutorBiz;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.RunNodeLogAckParam;
import com.nbatch.job.core.biz.model.RunNodeLogPullParam;
import com.nbatch.job.core.biz.model.RunNodeLogPullResult;
import com.nbatch.job.core.biz.model.RunNodeStatusQueryParam;
import com.nbatch.job.core.biz.model.RunNodeStatusQueryResult;
import com.nbatch.job.core.biz.model.StopRunNodeParam;
import com.nbatch.job.core.constant.HandleCodeConstant;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecutorBizProxy {

    private ExecutorBizProxy() {
    }

    public static ReturnT<String> beat(String address) {
        return call(address, "beat", ExecutorBiz::beat);
    }

    public static ReturnT<String> runWork(String address, ExecuteWorkParam executeWorkParam) {
        return call(address, "runWork", executorBiz -> executorBiz.runWork(executeWorkParam));
    }

    public static ReturnT<String> stopRunNode(String address, StopRunNodeParam stopRunNodeParam) {
        return call(address, "stopRunNode", executorBiz -> executorBiz.stopRunNode(stopRunNodeParam));
    }

    /**
     * 批量查询指定执行器中的运行节点实时状态。
     */
    public static ReturnT<RunNodeStatusQueryResult> queryRunNodeStatus(String address,
                                                                       RunNodeStatusQueryParam queryParam) {
        return call(address, "queryRunNodeStatus", executorBiz -> executorBiz.queryRunNodeStatus(queryParam));
    }

    public static ReturnT<RunNodeLogPullResult> pullRunNodeLog(String address, RunNodeLogPullParam pullParam) {
        return call(address, "pullRunNodeLog", executorBiz -> executorBiz.pullRunNodeLog(pullParam));
    }

    public static ReturnT<String> ackRunNodeLog(String address, RunNodeLogAckParam ackParam) {
        return call(address, "ackRunNodeLog", executorBiz -> executorBiz.ackRunNodeLog(ackParam));
    }

    /**
     * 拉取执行器本地详细日志。
     */
    public static ReturnT<RunNodeLogPullResult> pullRunNodeLogDetail(String address, RunNodeLogPullParam pullParam) {
        return call(address, "pullRunNodeLogDetail", executorBiz -> executorBiz.pullRunNodeLogDetail(pullParam));
    }

    /**
     * 确认执行器本地详细日志。
     */
    public static ReturnT<String> ackRunNodeLogDetail(String address, RunNodeLogAckParam ackParam) {
        return call(address, "ackRunNodeLogDetail", executorBiz -> executorBiz.ackRunNodeLogDetail(ackParam));
    }

    private static <T> ReturnT<T> call(String address, String action, ExecutorCall<T> executorCall) {
        if (StrUtil.isBlank(address)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "执行器地址不能为空。");
        }
        try {
            ExecutorBiz executorBiz = JobScheduler.getExecutorBiz(address);
            if (executorBiz == null) {
                return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "未找到执行器客户端。");
            }
            ReturnT<T> result = executorCall.call(executorBiz);
            return result == null ? new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "执行器 RPC 返回结果为空。") : result;
        } catch (Exception e) {
            log.warn("执行器 RPC 调用失败，地址：{}，操作：{}", address, action, e);
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "执行器 RPC 调用异常：" + e.getMessage());
        }
    }

    private interface ExecutorCall<T> {
        ReturnT<T> call(ExecutorBiz executorBiz) throws Exception;
    }
}
