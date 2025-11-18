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
    NODE_TYPE_IMPORT("import", "导入"),
    NODE_TYPE_EXPORT("export", "导出"),
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
