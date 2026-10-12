package com.nbatch.job.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 运行节点本地事件类型。
 */
@Getter
@RequiredArgsConstructor
public enum RunNodeLogEventTypeEnum {

    /**
     * 开始
     */
    STARTED("STARTED"),

    /**
     * 心跳
     */
    HEARTBEAT("HEARTBEAT"),

    /**
     * 成功
     */
    SUCCESS("SUCCESS"),

    /**
     * 失败
     */
    FAIL("FAIL"),

    /**
     * 停止
     */
    STOPPED("STOPPED");

    private final String value;
}
