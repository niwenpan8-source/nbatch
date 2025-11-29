package com.nbatch.job.handler.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @description: 节点类型枚举
 * @author: Mr.ni
 * @date: 2025/11/13
 */
@Getter
@RequiredArgsConstructor
public enum NodeTypeEnum {

    NODE_TYPE_SCRIPT("script", "脚本", 1),
    NODE_TYPE_STORE_PROCEDURE("store_procedure", "存储过程", 1),
    NODE_TYPE_EXECUTE_SQL("execute_sql", "执行sql", 1),
    NODE_TYPE_FILE_TO_DB("file_to_db", "文件导入到数据库", 1),
    NODE_TYPE_DB_TO_FILE("db_to_file", "数据库导出到文件", 1),
    NODE_TYPE_BEAN("bean", "bean模式", 1),
    ;

    private final String code;

    private final String value;

    private final Integer threadPoolNum;

    public static boolean isSupport(String code) {
        for (NodeTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }

    public static NodeTypeEnum getByCode(String code) {
        for (NodeTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
