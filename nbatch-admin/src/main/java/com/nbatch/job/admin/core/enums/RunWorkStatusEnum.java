package com.nbatch.job.admin.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 运行作业状态枚举
 * @author Mr.ni
 */
@Getter
@RequiredArgsConstructor
public enum RunWorkStatusEnum {

    WAIT(0, "待执行"),
    RUNNING(1, "进行中"),
    COMPLETE(2, "执行完毕"),
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
