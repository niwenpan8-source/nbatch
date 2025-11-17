package com.nbatch.job.admin.core.work;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 作业状态枚举
 * @author Mr.ni
 */
@Getter
@RequiredArgsConstructor
public enum WorkStatusEnum {

    STOP(0, "停止"),
    START(1, "运行")
    ;

    /**
     * 状态码
     */
    private final int code;

    /**
     * 状态描述
     */
    private final String title;
}
