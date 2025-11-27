package com.nbatch.job.handler.utils;

/**
 * @description: ascii转换
 * @author: Mr.ni
 * @date: 2025-09-26
 */
public class AsciiUtil {

    public static void main(String[] args) {
        // 原始字符串
        String parseStr = "X'017C01'";
        System.out.println("原始字符串: " + parseStr);

        // 1. 将字符串转换为ASCII码
        String asciiCodes = stringToAscii("Hello");
        System.out.println("字符串 'Hello' 转换为ASCII码: " + asciiCodes);

        // 2. 将ASCII码转换为字符串
        String text = asciiToString("72 101 108 108 111");
        System.out.println("ASCII码 '72 101 108 108 111' 转换为字符串: " + text);

        // 3. 处理原始字符串中的十六进制部分
        String hexPart = parseStr.replaceAll("X'|'", "");
        System.out.println("提取的十六进制部分: " + hexPart);

        // 4. 将十六进制转换为ASCII字符
        String hexToAsciiResult = hexToAscii(hexPart);
        System.out.println("十六进制 '" + hexPart + "' 转换为ASCII: " + hexToAsciiResult);

        // 5. 将ASCII字符转换为十六进制
        String asciiToHexResult = asciiToHex("ABC");
        System.out.println("ASCII 'ABC' 转换为十六进制: " + asciiToHexResult);
    }

    /**
     * 将字符串转换为ASCII码（以空格分隔）
     */
    public static String stringToAscii(String str) {
        StringBuilder asciiCodes = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            if (i > 0) {
                asciiCodes.append(" ");
            }
            asciiCodes.append((int) str.charAt(i));
        }
        return asciiCodes.toString();
    }

    /**
     * 将ASCII码（以空格分隔）转换为字符串
     */
    public static String asciiToString(String asciiCodes) {
        StringBuilder result = new StringBuilder();
        String[] codes = asciiCodes.split(" ");
        for (String code : codes) {
            try {
                int asciiValue = Integer.parseInt(code);
                result.append((char) asciiValue);
            } catch (NumberFormatException e) {
                // 忽略无效的ASCII码
            }
        }
        return result.toString();
    }

    /**
     * 将十六进制字符串转换为ASCII字符串
     */
    public static String hexToAscii(String hex) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            String str = hex.substring(i, i + 2);
            try {
                output.append((char) Integer.parseInt(str, 16));
            } catch (NumberFormatException e) {
                // 忽略无效的十六进制值
            }
        }
        return output.toString();
    }

    /**
     * 将ASCII字符串转换为十六进制字符串
     */
    public static String asciiToHex(String ascii) {
        StringBuilder hex = new StringBuilder();
        for (int i = 0; i < ascii.length(); i++) {
            hex.append(String.format("%02X", (int) ascii.charAt(i)));
        }
        return hex.toString();
    }

}
