package com.nbatch.job.admin.core.enums;

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

    NODE_TYPE_SCRIPT("script", "脚本"),
    NODE_TYPE_STORE_PROCEDURE("store_procedure", "存储过程"),
    NODE_TYPE_EXECUTE_SQL("execute_sql", "执行sql"),
    NODE_TYPE_FILE_TO_DB("file_to_db", "文件导入到数据库"),
    NODE_TYPE_DB_TO_FILE("db_to_file", "数据库导出到文件"),
    NODE_TYPE_BEAN("bean", "bean模式"),
    ;

    private final String code;

    private final String value;

    public static String getValue(String code) {
        for (NodeTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value.getValue();
            }
        }
        return null;
    }
}
