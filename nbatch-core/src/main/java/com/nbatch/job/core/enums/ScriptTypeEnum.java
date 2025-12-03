package com.nbatch.job.core.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @description: 脚本类型
 * @author: Mr.ni
 * @date: 2025/11/13
 */
@Getter
@RequiredArgsConstructor
public enum ScriptTypeEnum {

    JAVA("java", null, null),
    SHELL("shell", "bash", ".sh"),
    PYTHON("python", "python", ".py"),
    PHP("php", "php", ".php"),
    NODEJS("nodejs", "node", ".js"),
    POWER_SHELL("powershell", "powershell", ".ps1");

    private final String code;
    private final String cmd;
    private final String suffix;

    public static boolean isSupport(String code) {
        for (ScriptTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }

    public static ScriptTypeEnum getByCode(String code) {
        for (ScriptTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }

}
