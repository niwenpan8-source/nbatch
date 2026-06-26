package com.nbatch.job.handler.utils;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.ExecuteFileToDbParam;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * csv工具类
 *
 * @author: Mr.ni
 * @date: 2026/06/26
 */
@Log4j2
public class NbatchCsvUtil {


    public static int readCsvFirstSize(ExecuteFileToDbParam param, String separator) {
        int actualColumnSize = readCsvFirstSize(param.getFilePath(), separator, param.getFileCode());
        if (actualColumnSize > 0 || StrUtil.equals(param.getFilePath(), param.getRemoteFilePath())) {
            return actualColumnSize;
        }
        return readCsvFirstSize(param.getRemoteFilePath(), separator, param.getFileCode());
    }


    /**
     * 读取文件首行字段数
     */
    public static  int readCsvFirstSize(String filePath, String separator, String charsetName) {
        String actualSeparator = StrUtil.emptyToDefault(separator, "|");
        Charset charset = resolveCharset(charsetName);
        try {
            if (StrUtil.isBlank(filePath)) {
                log.warn("read csv first line column size failed, file path is blank");
                return 0;
            }
            Path path = Paths.get(filePath);
            if (!Files.isRegularFile(path)) {
                log.warn("read csv first line column size failed, file not found: {}", filePath);
                return 0;
            }
            try (BufferedReader reader = Files.newBufferedReader(path, charset)) {
                String firstLine = reader.readLine();
                if (firstLine == null) {
                    log.warn("read csv first line column size failed, file is empty: {}", filePath);
                    return 0;
                }
                return countColumns(firstLine, actualSeparator);
            }
        } catch (Exception e) {
            log.error("read csv first line column size error, filePath:{}, separator:{}, charset:{}",
                    filePath, actualSeparator, charset, e);
        }
        return 0;
    }

    public static String resolveFileSeparator(String separator) {
        if (StrUtil.isBlank(separator)) {
            return "|";
        }
        if (isSqlHexSeparator(separator)) {
            String hexPart = separator.substring(2, separator.length() - 1);
            return AsciiUtil.hexToAscii(hexPart);
        }
        String escapedSeparator = resolveEscapedSeparator(separator);
        if (escapedSeparator != null) {
            return escapedSeparator;
        }
        try {
            return String.valueOf(InvisibleCharUtil.parseChar(separator));
        } catch (IllegalArgumentException ignored) {
            return separator;
        }
    }

    public static  String resolveSqlSeparator(String separator) {
        if (StrUtil.isBlank(separator)) {
            return "|";
        }
        if (isSqlHexSeparator(separator)) {
            return separator;
        }
        String actualSeparator = resolveFileSeparator(separator);
        if (StrUtil.equals(actualSeparator, separator)) {
            return separator;
        }
        return "X'" + AsciiUtil.asciiToHex(actualSeparator) + "'";
    }

    public static  boolean isSqlHexSeparator(String separator) {
        return StrUtil.isNotBlank(separator)
                && separator.length() > 2
                && separator.regionMatches(true, 0, "X'", 0, 2)
                && separator.endsWith("'");
    }

    private static  String resolveEscapedSeparator(String separator) {
        switch (separator) {
            case "\\t":
                return "\t";
            case "\\n":
                return "\n";
            case "\\r":
                return "\r";
            default:
                return null;
        }
    }

    private static  Charset resolveCharset(String charsetName) {
        if (StrUtil.isBlank(charsetName)) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(charsetName);
        } catch (Exception e) {
            log.warn("文件编码不合法，使用UTF-8，charsetName:{}", charsetName);
            return StandardCharsets.UTF_8;
        }
    }

    /**
     * 统计csv首行字段数
     */
    private static int countColumns(String line, String separator) {
        if (StrUtil.isEmpty(separator)) {
            return 1;
        }
        int count = 1;
        int fromIndex = 0;
        while (true) {
            int index = line.indexOf(separator, fromIndex);
            if (index < 0) {
                return count;
            }
            count++;
            fromIndex = index + separator.length();
        }
    }

    public static String escapeSqlString(String value) {
        return StrUtil.emptyToDefault(value, "|").replace("'", "''");
    }

}
