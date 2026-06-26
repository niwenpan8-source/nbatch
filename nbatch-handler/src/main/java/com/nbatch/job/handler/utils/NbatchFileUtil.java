package com.nbatch.job.handler.utils;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.nbatch.job.handler.exception.HandlerException;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

    private static final int IO_BUFFER_SIZE = 1024 * 1024;

    private static final int GZIP_COMPRESSION_LEVEL = Deflater.BEST_SPEED;

    public static void main(String[] args) {

    }

    /**
     * 压缩文件，同时删除压缩文件，同时将压缩后的文件修改为压缩文件的名称
     */
    public static void gzipFile(String compressFilePath) throws IOException {
        Path sourcePath = Paths.get(compressFilePath);
        Path tempPath = Paths.get(compressFilePath + ".gzip.tmp");
        try {
            gzipFile(sourcePath.toString(), tempPath.toString());
            moveReplace(tempPath, sourcePath);
        } finally {
            Files.deleteIfExists(tempPath);
        }
    }

    /**
     * 文件解压(gz)
     */
    public static void unGzipFile(String importFilePath, String exportFilePath) throws IOException {
        Path importPath = Paths.get(importFilePath);
        Path exportPath = Paths.get(exportFilePath);
        if (!Files.exists(importPath)) {
            log.info("文件不存在：{}", importFilePath);
            return;
        }
        // 如果不是压缩文件直接copy本文件
        if (!isGzipFile(importFilePath)) {
            Files.copy(importPath, exportPath, StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        try (
                InputStream inputStream = new BufferedInputStream(Files.newInputStream(importPath), IO_BUFFER_SIZE);
                GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream, IO_BUFFER_SIZE);
                OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(exportPath), IO_BUFFER_SIZE)
        ) {
            copy(gzipInputStream, outputStream);
        }
    }

    /**
     * 通过文件流检查文件是否是压缩文件
     */
    private static boolean isGzipFile(String filePath) {
        // 再检查文件头
        try (InputStream is = new BufferedInputStream(Files.newInputStream(Paths.get(filePath)), 2)) {
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
                InputStream inputStream = new BufferedInputStream(Files.newInputStream(Paths.get(importFilePath)), IO_BUFFER_SIZE);
                OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(Paths.get(exportFilePath)), IO_BUFFER_SIZE);
                GZIPOutputStream gzipOutputStream = new LevelGzipOutputStream(outputStream, IO_BUFFER_SIZE, GZIP_COMPRESSION_LEVEL)
        ) {
            copy(inputStream, gzipOutputStream);
        }
    }

    private static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[IO_BUFFER_SIZE];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
    }

    private static void moveReplace(Path sourcePath, Path targetPath) throws IOException {
        try {
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static class LevelGzipOutputStream extends GZIPOutputStream {
        LevelGzipOutputStream(OutputStream outputStream, int size, int level) throws IOException {
            super(outputStream, size);
            def.setLevel(level);
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
