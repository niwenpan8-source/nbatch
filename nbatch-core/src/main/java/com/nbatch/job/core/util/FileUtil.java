package com.nbatch.job.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * file tool
 *
 * @author Mr.ni 2017-12-29 17:56:48
 */
public class FileUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);


    /**
     * delete recursively
     *
     * @param root  directory
     */
    public static boolean deleteRecursively(File root) {
        if (root != null && root.exists()) {
            if (root.isDirectory()) {
                File[] children = root.listFiles();
                if (children != null) {
                    for (File child : children) {
                        deleteRecursively(child);
                    }
                }
            }
            return root.delete();
        }
        return false;
    }


    public static void deleteFile(String fileName) {
        // file
        File file = new File(fileName);
        if (file.exists()) {
            cn.hutool.core.io.FileUtil.del(file);
        }
    }


    public static void writeFileContent(File file, byte[] data) {

        // file
        if (!file.exists()) {
            cn.hutool.core.io.FileUtil.mkdir(file.getParentFile());
        }

        // append file content
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            fos.flush();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    public static byte[] readFileContent(File file) {
        long filelength = file.length();

        // 检查文件长度是否超出int范围，防止内存溢出
        if (filelength > Integer.MAX_VALUE) {
            logger.error("File too large to read into byte array: " + file.length());
            return null;
        }

        byte[] filecontent = new byte[(int) filelength];

        try (FileInputStream in = new FileInputStream(file)) {
            int totalBytesRead = 0;
            int bytesRead;

            // 循环读取直到文件末尾或读取完整
            while (totalBytesRead < filecontent.length &&
                    (bytesRead = in.read(filecontent, totalBytesRead, filecontent.length - totalBytesRead)) != -1) {
                totalBytesRead += bytesRead;
            }

            // 检查是否完整读取了文件
            if (totalBytesRead != filecontent.length) {
                logger.error("Could not completely read file: " + file.getName());
                return null;
            }

            return filecontent;
        } catch (Exception e) {
            logger.error("Error reading file: " + file.getName(), e);
            return null;
        }
    }


}
