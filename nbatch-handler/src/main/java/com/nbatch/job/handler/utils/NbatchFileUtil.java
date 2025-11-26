package com.nbatch.job.handler.utils;

import cn.hutool.core.compress.Gzip;
import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

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

}
