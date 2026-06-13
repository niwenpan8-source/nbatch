package com.nbatch.job.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 运行节点本地事件类型。
 */
@Getter
@RequiredArgsConstructor
public enum RunNodeLogEventTypeEnum {

    STARTED("STARTED"),
    SUCCESS("SUCCESS"),
    FAIL("FAIL"),
    STOPPED("STOPPED");

    private final String value;
}
