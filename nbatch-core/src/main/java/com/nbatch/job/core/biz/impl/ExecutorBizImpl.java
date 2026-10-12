package com.nbatch.job.core.biz.impl;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
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
import com.nbatch.job.core.handler.IJobHandlerHolder;
import com.nbatch.job.core.handler.RunNodeStopHandler;
import com.nbatch.job.core.files.RunNodeLogEventLog;
import com.nbatch.job.core.files.RunNodeLogDetailLog;
import com.nbatch.job.core.util.SpringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExecutorBizImpl implements ExecutorBiz {

    private static final Logger log = LoggerFactory.getLogger(ExecutorBizImpl.class);

    @Override
    public ReturnT<String> beat() {
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> runWork(ExecuteWorkParam param) {
        if (param == null || param.getExecuteNodeParamList() == null || param.getExecuteNodeParamList().isEmpty()) {
            return ReturnT.error("执行作业流程参数或节点列表不能为空。");
        }
        if (param.getWorkId() == null) {
            return ReturnT.error("workId 不能为空。");
        }
        try {
            SpringUtil.getBean(IJobHandlerHolder.class).handle(param);
            return ReturnT.success("work nodes accepted");
        } catch (Exception e) {
            log.error("执行作业流程失败，workId：{}", param.getWorkId(), e);
            return ReturnT.error("执行作业流程失败：" + ExceptionUtil.getRootCauseMessage(e));
        }
    }

    @Override
    public ReturnT<String> stopRunNode(StopRunNodeParam param) {
        try {
            return SpringUtil.getBean(RunNodeStopHandler.class).stopRunNode(param);
        } catch (Exception e) {
            log.warn("未找到运行节点停止处理器或停止失败。", e);
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "停止运行节点失败：" + ExceptionUtil.getRootCauseMessage(e));
        }
    }

    @Override
    public ReturnT<RunNodeStatusQueryResult> queryRunNodeStatus(RunNodeStatusQueryParam param) {
        try {
            return SpringUtil.getBean(RunNodeStopHandler.class).queryRunNodeStatus(param);
        } catch (Exception e) {
            log.warn("未找到运行节点状态查询处理器或查询失败。", e);
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "查询运行节点状态失败：" + ExceptionUtil.getRootCauseMessage(e));
        }
    }

    @Override
    public ReturnT<RunNodeLogPullResult> pullRunNodeLog(RunNodeLogPullParam param) {
        if (param == null || StrUtil.isBlank(param.getConsumerId())) {
            return ReturnT.error("运行节点日志 consumerId 不能为空。");
        }
        RunNodeLogPullResult result = RunNodeLogEventLog.getInstance()
                .tryPull(param.getConsumerId(), param.getMaxSize());
        if (result == null) {
            return ReturnT.error("运行节点日志正在被其他管理端拉取。");
        }
        return new ReturnT<>(result);
    }

    @Override
    public ReturnT<String> ackRunNodeLog(RunNodeLogAckParam param) {
        if (param == null || StrUtil.isBlank(param.getConsumerId()) || StrUtil.isBlank(param.getLeaseToken())) {
            return ReturnT.error("运行节点日志确认租约不能为空。");
        }
        return RunNodeLogEventLog.getInstance().ack(param.getConsumerId(), param.getLeaseToken(), param.getOffset())
                ? ReturnT.SUCCESS : ReturnT.error("运行节点日志确认租约无效。");
    }

    @Override
    public ReturnT<RunNodeLogPullResult> pullRunNodeLogDetail(RunNodeLogPullParam param) {
        if (param == null || StrUtil.isBlank(param.getConsumerId())) {
            return ReturnT.error("运行节点详细日志 consumerId 不能为空。");
        }
        RunNodeLogPullResult result = RunNodeLogDetailLog.getInstance()
                .tryPull(param.getConsumerId(), param.getMaxSize());
        if (result == null) {
            return ReturnT.error("运行节点详细日志正在被其他管理端拉取。");
        }
        return new ReturnT<>(result);
    }

    @Override
    public ReturnT<String> ackRunNodeLogDetail(RunNodeLogAckParam param) {
        if (param == null || StrUtil.isBlank(param.getConsumerId()) || StrUtil.isBlank(param.getLeaseToken())) {
            return ReturnT.error("运行节点详细日志确认租约不能为空。");
        }
        return RunNodeLogDetailLog.getInstance().ack(param.getConsumerId(), param.getLeaseToken(), param.getOffset())
                ? ReturnT.SUCCESS : ReturnT.error("运行节点详细日志确认租约无效。");
    }
}
