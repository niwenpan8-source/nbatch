package com.nbatch.job.handler.utils;

import cn.hutool.core.util.StrUtil;

/**
 * @description: 隐藏字符工具类
 * @author: Mr.ni
 * @date: 2026/1/19
 */
public class InvisibleCharUtil {

    public static char parseChar(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Input string is null or empty");
        }

        // 1. 如果输入本身就是单个字符（包括不可见字符）
        if (input.length() == 1) {
            return input.charAt(0);
        }

        // 2. 处理 \\uXXXX 格式（如 "\\u0000" 或 "\u0000"）
        if (StrUtil.startWith(input, "\\u")) {
            try {
                String hex = input.substring(input.startsWith("\\u") ? 2 : 1);
                int codePoint = Integer.parseInt(hex, 16);
                if (codePoint >= 0 && codePoint <= Character.MAX_VALUE) {
                    return (char) codePoint;
                }
            } catch (Exception ignored) {}
        }

        // 3. 处理 \xXX 格式（如 "\\x00"）
        if (StrUtil.startWith(input, "\\x")) {
            try {
                String hex = input.substring(2);
                int value = Integer.parseInt(hex, 16);
                if (value >= 0 && value <= 0xFF) {
                    return (char) value;
                }
            } catch (Exception ignored) {}
        }

        // 4. 处理 0xXX 或 XX（纯十六进制）
        if (input.startsWith("0x")) {
            try {
                int value = Integer.parseInt(input.substring(2), 16);
                if (value >= 0 && value <= 0xFF) {
                    return (char) value;
                }
            } catch (Exception ignored) {}
        } else if (input.matches("[0-9a-fA-F]{1,4}")) { // 纯 hex
            try {
                int value = Integer.parseInt(input, 16);
                if (value >= 0 && value <= 0xFFFF) {
                    return (char) value;
                }
            } catch (Exception ignored) {}
        }

        // 5. 处理十进制数字（ASCII 码）
        if (input.matches("\\d+")) {
            try {
                int value = Integer.parseInt(input);
                if (value >= 0 && value <= 0xFFFF) {
                    return (char) value;
                }
            } catch (Exception ignored) {}
        }

        // 6. 处理控制字符名称（可选）
        switch (input.toUpperCase()) {
            case "NUL": return '\0';
            case "SOH": return '\u0001';
            case "STX": return '\u0002';
            case "ETX": return '\u0003';
            case "EOT": return '\u0004';
            case "ENQ": return '\u0005';
            case "ACK": return '\u0006';
            case "BEL": return '\u0007';
            // \u0008
            case "BS":  return '\b';
            // \u0009
            case "TAB": return '\t';
            case "LF":  return '\n';
            case "VT":  return '\u000B';
            // \u000C
            case "FF":  return '\f';
            case "CR":  return '\r';
            case "SO":  return '\u000E';
            case "SI":  return '\u000F';
            // ... 可继续扩展
        }

        throw new IllegalArgumentException("Unrecognized character format: " + input);
    }

}
