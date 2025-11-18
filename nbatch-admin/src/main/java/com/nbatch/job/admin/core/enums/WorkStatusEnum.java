package com.nbatch.job.admin.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 作业状态枚举
 * @author Mr.ni
 */
@Getter
@RequiredArgsConstructor
public enum WorkStatusEnum {

    STOP(0, "停用"),
    START(1, "启用")
    ;

    /**
     * 状态码
     */
    private final int code;

    /**
     * 状态描述
     */
    private final String value;
}
