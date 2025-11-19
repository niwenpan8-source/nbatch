package com.nbatch.job.handler.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 异常代码枚举
 * @author Mr.ni
 */
@Getter
@RequiredArgsConstructor
public enum ExceptionCodeEnum {

    /**
     * 执行更新sql异常
     */
    EXECUTE_UPDATE_SQL_FAIL(10001),

    /**
     * 不支持的节点类型
     */
    NOT_SUPPORT_NODE_TYPE(10001),
    ;

    private final Integer code;



}
