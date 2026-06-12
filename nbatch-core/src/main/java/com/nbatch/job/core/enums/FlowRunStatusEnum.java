package com.nbatch.job.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 运行流程状态
 *
 * @author Mr.ni
 */
@Getter
@RequiredArgsConstructor
public enum FlowRunStatusEnum {

    WAIT(0, "待执行"),

    RUNNING(1, "进行中"),

    COMPLETE(2, "执行完毕"),

    EXCEPTION(3, "执行异常"),

    DISPATCHED(4, "已下发"),
    ;

    /**
     * 状态码
     */
    private final int code;

    /**
     * 状态描述
     */
    private final String value;

    public static String getValueByCode(int code) {
        for (FlowRunStatusEnum value : FlowRunStatusEnum.values()) {
            if (value.getCode() == code) {
                return value.getValue();
            }
        }
        return null;
    }
}
