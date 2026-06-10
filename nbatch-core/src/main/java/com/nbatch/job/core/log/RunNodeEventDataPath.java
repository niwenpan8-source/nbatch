package com.nbatch.job.core.log;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.io.File;

/**
 * 运行节点事件数据目录。
 */
public class RunNodeEventDataPath {

    @Getter
    private static String dataBasePath = System.getProperty("user.dir")
            .concat(File.separator).concat("data")
            .concat(File.separator).concat("jobhandler");

    public static void initDataPath(String dataPath) {
        if (StrUtil.isNotBlank(dataPath)) {
            dataBasePath = buildPath(dataPath);
        }
        File dataPathDir = new File(dataBasePath);
        if (!dataPathDir.exists()) {
            FileUtil.mkdir(dataPathDir);
        }
        dataBasePath = dataPathDir.getPath();
    }

    private static String buildPath(String dataPath) {
        File file = new File(dataPath);
        if (file.isAbsolute()) {
            return dataPath;
        }
        return System.getProperty("user.dir").concat(dataPath);
    }
}
