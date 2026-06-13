package com.nbatch.job.core.biz.impl;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.nbatch.job.core.biz.ExecutorBiz;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
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
import com.nbatch.job.core.enums.ExecutorBlockStrategyEnum;
import com.nbatch.job.core.executor.BatchJobExecutor;
import com.nbatch.job.core.glue.GlueFactory;
import com.nbatch.job.core.glue.GlueTypeEnum;
import com.nbatch.job.core.handler.IJobHandler;
import com.nbatch.job.core.handler.RunNodeStopHandler;
import com.nbatch.job.core.handler.impl.GlueJobHandler;
import com.nbatch.job.core.handler.impl.ScriptJobHandler;
import com.nbatch.job.core.handler.impl.WorkJobHandler;
import com.nbatch.job.core.log.JobFileAppender;
import com.nbatch.job.core.thread.RunNodeLogEventLog;
import com.nbatch.job.core.thread.JobThread;
import com.nbatch.job.core.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 *
 * @author 执行器业务逻辑
 * @date 2025/11/05
 */
@Slf4j
public class ExecutorBizImpl implements ExecutorBiz {

    @Override
    public ReturnT<String> beat() {
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> idleBeat(IdleBeatParam idleBeatParam) {

        // isRunningOrHasQueue
        boolean isRunningOrHasQueue = false;
        JobThread jobThread = BatchJobExecutor.loadJobThread(idleBeatParam.getJobId());
        if (jobThread != null && jobThread.isRunningOrHasQueue()) {
            isRunningOrHasQueue = true;
        }

        if (isRunningOrHasQueue) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "job thread is running or has trigger queue.");
        }
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        // load old：jobHandler + jobThread
        JobThread jobThread = BatchJobExecutor.loadJobThread(triggerParam.getJobId());
        IJobHandler jobHandler = jobThread!=null?jobThread.getHandler():null;
        String removeOldReason = null;

        // valid：jobHandler + jobThread
        GlueTypeEnum glueTypeEnum = GlueTypeEnum.match(triggerParam.getGlueType());
        if (GlueTypeEnum.BEAN == glueTypeEnum) {

            // new jobhandler
            IJobHandler newJobHandler = BatchJobExecutor.loadJobHandler(triggerParam.getExecutorHandler());

            // valid old jobThread
            if (jobThread!=null && jobHandler != newJobHandler) {
                // change handler, need kill old thread
                removeOldReason = "change jobhandler or glue type, and terminate the old job thread.";

                jobThread = null;
                jobHandler = null;
            }

            // valid handler
            if (jobHandler == null) {
                jobHandler = newJobHandler;
                if (jobHandler == null) {
                    return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "job handler [" + triggerParam.getExecutorHandler() + "] not found.");
                }
            }

        } else if (GlueTypeEnum.WORK == glueTypeEnum) {
            // valid old jobThread
            if (jobThread != null && !(jobThread.getHandler() instanceof WorkJobHandler)) {
                removeOldReason = "change job source or glue type, and terminate the old job thread.";

                jobThread = null;
                jobHandler = null;
            }
            ExecuteWorkParam executeWorkParam = triggerParam.getExecuteWorkParam();
            executeWorkParam.setJobId(triggerParam.getJobId());
            executeWorkParam.setJobLogId(triggerParam.getLogId());
            // valid handler
            if (jobHandler == null) {
                jobHandler = new WorkJobHandler(executeWorkParam);
            } else {
                WorkJobHandler handler = (WorkJobHandler) jobThread.getHandler();
                handler.setWorkNodeParam(executeWorkParam);
            }

        } else if (GlueTypeEnum.GLUE_GROOVY == glueTypeEnum) {

            // valid old jobThread
            if (jobThread != null &&
                    !(jobThread.getHandler() instanceof GlueJobHandler
                        && ((GlueJobHandler) jobThread.getHandler()).getGlueUpdatetime()==triggerParam.getGlueUpdatetime() )) {
                // change handler or gluesource updated, need kill old thread
                removeOldReason = "change job source or glue type, and terminate the old job thread.";

                jobThread = null;
                jobHandler = null;
            }

            // valid handler
            if (jobHandler == null) {
                try {
                    IJobHandler originJobHandler = GlueFactory.getInstance().loadNewInstance(triggerParam.getGlueSource());
                    jobHandler = new GlueJobHandler(originJobHandler, triggerParam.getGlueUpdatetime());
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, ExceptionUtil.getRootCauseMessage(e));
                }
            }
        } else if (glueTypeEnum!=null && glueTypeEnum.isScript()) {

            // valid old jobThread
            if (jobThread != null &&
                    !(jobThread.getHandler() instanceof ScriptJobHandler
                            && ((ScriptJobHandler) jobThread.getHandler()).getGlueUpdatetime()==triggerParam.getGlueUpdatetime() )) {
                // change script or gluesource updated, need kill old thread
                removeOldReason = "change job source or glue type, and terminate the old job thread.";

                jobThread = null;
                jobHandler = null;
            }

            // valid handler
            if (jobHandler == null) {
                jobHandler = new ScriptJobHandler(triggerParam.getJobId(), triggerParam.getGlueUpdatetime(), triggerParam.getGlueSource(), GlueTypeEnum.match(triggerParam.getGlueType()));
            }
        } else {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "glueType[" + triggerParam.getGlueType() + "] is not valid.");
        }

        // executor block strategy
        if (jobThread != null) {
            ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(triggerParam.getExecutorBlockStrategy(), null);
            if (ExecutorBlockStrategyEnum.DISCARD_LATER == blockStrategy) {
                // discard when running
                if (jobThread.isRunningOrHasQueue()) {
                    return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "block strategy effect：" + ExecutorBlockStrategyEnum.DISCARD_LATER.getTitle());
                }
            } else if (ExecutorBlockStrategyEnum.COVER_EARLY == blockStrategy) {
                // kill running jobThread
                if (jobThread.isRunningOrHasQueue()) {
                    removeOldReason = "block strategy effect：" + ExecutorBlockStrategyEnum.COVER_EARLY.getTitle();

                    jobThread = null;
                }
            } else {
                // just queue trigger
            }
        }

        // replace thread (new or exists invalid)
        if (jobThread == null) {
            jobThread = BatchJobExecutor.registerJobThread(triggerParam.getJobId(), jobHandler, removeOldReason);
        }

        // push data to queue
        return jobThread.pushTriggerQueue(triggerParam);
    }

    @Override
    public ReturnT<String> kill(KillParam killParam) {
        // kill handlerThread, and create new one
        JobThread jobThread = BatchJobExecutor.loadJobThread(killParam.getJobId());
        if (jobThread != null) {
            BatchJobExecutor.removeJobThread(killParam.getJobId(), "scheduling center kill job.");
            return ReturnT.SUCCESS;
        }

        return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_SUCCESS, "job thread already killed.");
    }

    @Override
    public ReturnT<String> stopRunNode(StopRunNodeParam stopRunNodeParam) {
        try {
            RunNodeStopHandler stopHandler = SpringUtil.getBean(RunNodeStopHandler.class);
            return stopHandler.stopRunNode(stopRunNodeParam);
        } catch (Exception e) {
            log.warn("run node stop handler not found or stop failed", e);
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, ExceptionUtil.getRootCauseMessage(e));
        }
    }

    @Override
    public ReturnT<LogResult> log(LogParam logParam) {
        // log filename: logPath/yyyy-MM-dd/9999.log
        String logFileName = JobFileAppender.makeLogFileName(new Date(logParam.getLogDateTim()), logParam.getLogId());

        LogResult logResult = JobFileAppender.readLog(logFileName, logParam.getFromLineNum());
        return new ReturnT<>(logResult);
    }

    @Override
    public ReturnT<RunNodeLogPullResult> pullRunNodeLog(RunNodeLogPullParam pullParam) {
        return new ReturnT<>(RunNodeLogEventLog.getInstance().pull(
                pullParam == null ? null : pullParam.getOffset(),
                pullParam == null ? null : pullParam.getMaxSize()));
    }

    @Override
    public ReturnT<String> ackRunNodeLog(RunNodeLogAckParam ackParam) {
        if (ackParam != null) {
            RunNodeLogEventLog.getInstance().ack(ackParam.getOffset());
        }
        return ReturnT.SUCCESS;
    }

}
