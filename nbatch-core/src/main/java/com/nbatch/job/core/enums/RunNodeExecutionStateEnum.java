package com.nbatch.job.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 执行器中的运行节点实时状态。
 */
@Getter
@RequiredArgsConstructor
public enum RunNodeExecutionStateEnum {

    /** 已进入线程池队列，尚未开始执行。 */
    QUEUED("QUEUED"),

    /** 工作线程已经开始执行任务。 */
    RUNNING("RUNNING"),

    /** 已请求中断，但任务线程尚未真实退出。 */
    STOP_REQUESTED("STOP_REQUESTED"),

    /** 无旧任务 Future，且执行器保留了该尝试的停止屏障。 */
    STOPPED("STOPPED"),

    /** 当前进程既没有任务 Future，也没有停止确认。 */
    ABSENT("ABSENT");

    /** 管理端与执行器之间传输的状态协议值。 */
    private final String value;
}
