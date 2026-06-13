package com.nbatch.job.core.biz;

import com.nbatch.job.core.biz.model.*;

/**
 * @author Mr.ni
 * @description: 执行器服务
 * @date 2025/11/05
 */
public interface ExecutorBiz {

    /**
     * beat
     */
    ReturnT<String> beat();

    /**
     * idle beat
     *
     * @param idleBeatParam 心跳参数
     */
    ReturnT<String> idleBeat(IdleBeatParam idleBeatParam);

    /**
     * run
     *
     * @param triggerParam 定时调度参数
     */
    ReturnT<String> run(TriggerParam triggerParam);

    /**
     * kill
     *
     * @param killParam kill 参数
     */
    ReturnT<String> kill(KillParam killParam);

    /**
     * stop run node
     */
    ReturnT<String> stopRunNode(StopRunNodeParam stopRunNodeParam);

    /**
     * log
     *
     * @param logParam 日志参数
     */
    ReturnT<LogResult> log(LogParam logParam);

    /**
     * 拉取运行节点本地事件日志。
     */
    ReturnT<RunNodeLogPullResult> pullRunNodeLog(RunNodeLogPullParam pullParam);

    /**
     * 确认运行节点本地事件日志。
     */
    ReturnT<String> ackRunNodeLog(RunNodeLogAckParam ackParam);

}
