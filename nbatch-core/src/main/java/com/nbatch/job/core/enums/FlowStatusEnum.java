package com.nbatch.job.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 流程状态
 * @author Mr.ni
 */
@Getter
@RequiredArgsConstructor
public enum FlowStatusEnum {

    STOP(0, "停用"),
    START(1, "启用"),
    EXCEPTION(2, "异常")
    ;

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String value;
}
