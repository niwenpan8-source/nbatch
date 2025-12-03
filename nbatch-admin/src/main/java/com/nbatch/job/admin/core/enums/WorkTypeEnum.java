package com.nbatch.job.admin.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 作业状态枚举
 * @author Mr.ni
 */
@Getter
@RequiredArgsConstructor
public enum WorkTypeEnum {

    //0:翻牌类型，1：顺序类型
    TYPE_TURN(0, "翻牌类型"),
    TYPE_SEQUENCE(1, "顺序类型")
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
