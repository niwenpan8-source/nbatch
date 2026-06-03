package com.nbatch.job.handler.utils;

import cn.hutool.core.compress.Gzip;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.nbatch.job.handler.exception.HandlerException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Map;

import static com.nbatch.job.handler.constant.JobHandlerConstant.FILE_NAME_REPLACE_CHAR_PREFIX;
import static com.nbatch.job.handler.constant.JobHandlerConstant.FILE_NAME_REPLACE_CHAR_SUFFIX;
import static com.nbatch.job.handler.enums.ExceptionCodeEnum.DB_TO_FILE_FAIL;

/**
 * @description: 文件工具类
 * @author: Mr.ni
 * @date: 2025/11/26
 */
@Slf4j
public class NbatchFileUtil {

    /**
     * 文件解压(gz)
     */
    public static void unGzipFile(String importFilePath, String exportFilePath) throws IOException {
        if (!FileUtil.exist(importFilePath)) {
            log.info("文件不存在：{}", importFilePath);
            return;
        }
        // 如果不是压缩文件直接copy本文件
        if (!isGzipFile(importFilePath)) {
            if (FileUtil.exist(exportFilePath)) {
                FileUtil.del(exportFilePath);
            }
            FileUtil.copyFile(importFilePath, exportFilePath);
            return;
        }
        try (
                InputStream inputStream = Files.newInputStream(Paths.get(importFilePath));
                OutputStream outputStream = Files.newOutputStream(Paths.get(exportFilePath));
                Gzip gzip = Gzip.of(inputStream, outputStream)
        ) {
            gzip.unGzip();
        }
    }

    /**
     * 通过文件流检查文件是否是压缩文件
     */
    private static boolean isGzipFile(String filePath) {
        // 再检查文件头
        try (InputStream is = Files.newInputStream(Paths.get(filePath))) {
            byte[] header = new byte[2];
            int read = is.read(header);
            return read >= 2 && header[0] == (byte) 0x1f && header[1] == (byte) 0x8b;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 文件压缩(gz)
     */
    public static void gzipFile(String importFilePath, String exportFilePath) throws IOException {
        try (
                InputStream inputStream = Files.newInputStream(Paths.get(importFilePath));
                OutputStream outputStream = Files.newOutputStream(Paths.get(exportFilePath));
                Gzip gzip = Gzip.of(inputStream, outputStream)
        ) {
            gzip.gzip();
        }
    }

    /**
     * 生成文件名称
     */
    public static String generateFileName(String templateName, JSONObject replaceObj) {
        if (StrUtil.isEmpty(templateName)) {
            throw new HandlerException(DB_TO_FILE_FAIL.getCode(), "导出文件配置，文件名称不可为空");
        }
        String fileName = templateName;
        for (Map.Entry<String, Object> entry : replaceObj.entrySet()) {
            String key = FILE_NAME_REPLACE_CHAR_PREFIX + entry.getKey() + FILE_NAME_REPLACE_CHAR_SUFFIX;
            if (entry.getValue() instanceof String) {
                fileName = StrUtil.replace(fileName, key, (String) entry.getValue());
            }
            if (entry.getValue() instanceof Date) {
                fileName = StrUtil.replace(fileName, key, DateUtil.format((Date) entry.getValue(), DatePattern.PURE_DATE_FORMAT));
            }
        }
        return fileName;
    }

    /**
     * 容差比例
     */
    public static double TOLERANCE_NUM_RATIO = 0.8;

    /**
     * 检查文件导入行数
     */
    public static void checkImportDataNum(int totalLines, long importTotalNum) {
        double toleranceNum;
        // 只有当容忍度为数字同事大于0小于1的时候才进行容差处理
        toleranceNum = totalLines * TOLERANCE_NUM_RATIO;
        if (importTotalNum + 20 < toleranceNum) {
            throw new HandlerException(DB_TO_FILE_FAIL.getCode(), StrUtil.format("数据导入失败总条数{}，导入条数{}", totalLines, importTotalNum));
        }
        log.info("外部文件导入数据完成，总条数{}，导入条数{}", totalLines, importTotalNum);
    }

}
