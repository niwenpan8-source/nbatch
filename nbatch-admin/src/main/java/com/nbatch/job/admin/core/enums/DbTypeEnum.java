package com.nbatch.job.admin.core.enums;

import com.baomidou.mybatisplus.annotation.DbType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @description: 节点类型枚举
 * @author: Mr.ni
 * @date: 2025/11/13
 */
@Getter
@RequiredArgsConstructor
public enum DbTypeEnum {

    GAUSS_DB(DbType.OPENGAUSS.getDb(), DbType.OPENGAUSS.getDesc()),
    GBASE_DB(DbType.GBASE.getDb(), DbType.GBASE.getDesc()),
    MYSQL_DB(DbType.MYSQL.getDb(), DbType.MYSQL.getDesc()),
    ;

    private final String code;

    private final String value;

    public static String getValue(String code) {
        for (DbTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value.getValue();
            }
        }
        return null;
    }
}
