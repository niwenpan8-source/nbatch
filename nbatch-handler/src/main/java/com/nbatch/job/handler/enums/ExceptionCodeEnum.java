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
    NOT_SUPPORT_NODE_TYPE(10002),

    /**
     * db导出文件异常
     */
    DB_TO_FILE_FAIL(10003),

    /**
     * 文件导入数据库异常
     */
    FILE_TO_DB_FAIL(10004),

    /**
     * db导出文件异常
     */
    SCRIPT_FAIL(10005),


    LUA_SCRIPT_FAIL(10006),
    ;

    private final Integer code;



}
