package com.nbatch.job.core.biz;

import com.nbatch.job.core.biz.model.IdleBeatParam;
import com.nbatch.job.core.biz.model.KillParam;
import com.nbatch.job.core.biz.model.LogParam;
import com.nbatch.job.core.biz.model.LogResult;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.RunNodeLogDetailParam;
import com.nbatch.job.core.biz.model.TriggerParam;

import java.util.List;

/**
 * @description: 执行器服务
 * @author Mr.ni
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
     * log
     *
     * @param logParam 日志参数
     */
    ReturnT<LogResult> log(LogParam logParam);

}
