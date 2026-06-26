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
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    private static final int FAST_COMPRESSION_LEVEL = Deflater.NO_COMPRESSION;

    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();
        gzipFile("C:\\disk\\project\\study\\2025.zip", "C:\\disk\\project\\study\\2025.gz");
        log.info("gzip cost time:{}", System.currentTimeMillis() - startTime);
        unGzipFile("C:\\disk\\project\\study\\2025.gz", "C:\\disk\\project\\study\\2025.zip");
        log.info("ungzip cost time:{}", System.currentTimeMillis() - startTime);

    }

    private static String defaultCompressOutputPath(Path sourcePath) {
        return sourcePath + (Files.isDirectory(sourcePath) ? ".zip" : ".gz");
    }

    /**
     * 自动判断文件或目录并压缩：文件使用 gzip，目录使用 zip。
     */
    public static void compressPath(String importPath, String exportPath) throws IOException {
        compressPath(importPath, exportPath, GZIP_COMPRESSION_LEVEL);
    }

    /**
     * 速度优先压缩：只打包不做 deflate 压缩，输出会更大但 CPU 开销最低。
     */
    public static void compressPathFast(String importPath, String exportPath) throws IOException {
        compressPath(importPath, exportPath, FAST_COMPRESSION_LEVEL);
    }

    private static void compressPath(String importFilePath, String exportFilePath, int compressionLevel) throws IOException {
        Path importPath = Paths.get(importFilePath);
        if (Files.isDirectory(importPath)) {
            zipDirectory(importFilePath, exportFilePath, compressionLevel);
            return;
        }
        gzipFile(importFilePath, exportFilePath, compressionLevel);
    }

    /**
     * 压缩文件，同时删除压缩文件，同时将压缩后的文件修改为压缩文件的名称
     */
    public static void gzipFile(String compressFilePath) throws IOException {
        gzipFile(compressFilePath, GZIP_COMPRESSION_LEVEL);
    }

    public static void gzipFileFast(String compressFilePath) throws IOException {
        gzipFile(compressFilePath, FAST_COMPRESSION_LEVEL);
    }

    private static void gzipFile(String compressFilePath, int compressionLevel) throws IOException {
        Path sourcePath = Paths.get(compressFilePath);
        validateRegularFile(sourcePath, "gzip source");
        Path tempPath = Paths.get(compressFilePath + ".gzip.tmp");
        createParentDirectories(tempPath);
        try {
            gzipFile(sourcePath.toString(), tempPath.toString(), compressionLevel);
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
            log.info("file does not exist:{}", importFilePath);
            return;
        }
        validateRegularFile(importPath, "ungzip source");
        validateOutputPath(exportPath, "ungzip target");
        createParentDirectories(exportPath);
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
        gzipFile(importFilePath, exportFilePath, GZIP_COMPRESSION_LEVEL);
    }

    public static void gzipFileFast(String importFilePath, String exportFilePath) throws IOException {
        gzipFile(importFilePath, exportFilePath, FAST_COMPRESSION_LEVEL);
    }

    private static void gzipFile(String importFilePath, String exportFilePath, int compressionLevel) throws IOException {
        Path importPath = Paths.get(importFilePath);
        Path exportPath = Paths.get(exportFilePath);
        if (Files.isDirectory(importPath)) {
            zipDirectory(importFilePath, exportFilePath, compressionLevel);
            return;
        }
        validateRegularFile(importPath, "gzip source");
        validateOutputPath(exportPath, "gzip target");
        createParentDirectories(exportPath);
        try (
                InputStream inputStream = new BufferedInputStream(Files.newInputStream(importPath), IO_BUFFER_SIZE);
                OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(exportPath), IO_BUFFER_SIZE);
                GZIPOutputStream gzipOutputStream = new LevelGzipOutputStream(outputStream, IO_BUFFER_SIZE, compressionLevel)
        ) {
            copy(inputStream, gzipOutputStream);
        }
    }

    /**
     * 目录压缩(zip)
     */
    public static void zipDirectory(String importDirPath, String exportFilePath) throws IOException {
        zipDirectory(importDirPath, exportFilePath, GZIP_COMPRESSION_LEVEL);
    }

    public static void zipDirectoryFast(String importDirPath, String exportFilePath) throws IOException {
        zipDirectory(importDirPath, exportFilePath, FAST_COMPRESSION_LEVEL);
    }

    private static void zipDirectory(String importDirPath, String exportFilePath, int compressionLevel) throws IOException {
        Path importPath = Paths.get(importDirPath);
        Path exportPath = Paths.get(exportFilePath);
        validateDirectory(importPath, "zip source");
        validateOutputPath(exportPath, "zip target");
        createParentDirectories(exportPath);
        try (
                OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(exportPath), IO_BUFFER_SIZE);
                ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
                Stream<Path> pathStream = Files.walk(importPath)
        ) {
            zipOutputStream.setLevel(compressionLevel);
            for (Path path : (Iterable<Path>) pathStream::iterator) {
                if (!Files.isDirectory(path)) {
                    addZipEntry(importPath, path, zipOutputStream);
                }
            }
        }
    }

    private static void addZipEntry(Path rootPath, Path filePath, ZipOutputStream zipOutputStream) throws IOException {
        String entryName = rootPath.relativize(filePath).toString().replace('\\', '/');
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(filePath), IO_BUFFER_SIZE)) {
            zipOutputStream.putNextEntry(new ZipEntry(entryName));
            copy(inputStream, zipOutputStream);
            zipOutputStream.closeEntry();
        }
    }

    private static void validateRegularFile(Path path, String name) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException(name + " does not exist: " + path);
        }
        if (!Files.isRegularFile(path)) {
            throw new IOException(name + " must be a regular file: " + path);
        }
    }

    private static void validateDirectory(Path path, String name) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException(name + " does not exist: " + path);
        }
        if (!Files.isDirectory(path)) {
            throw new IOException(name + " must be a directory: " + path);
        }
    }

    private static void validateOutputPath(Path path, String name) throws IOException {
        if (Files.exists(path) && Files.isDirectory(path)) {
            throw new IOException(name + " must not be a directory: " + path);
        }
    }

    private static void createParentDirectories(Path path) throws IOException {
        Path parentPath = path.getParent();
        if (parentPath != null && !Files.exists(parentPath)) {
            Files.createDirectories(parentPath);
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
