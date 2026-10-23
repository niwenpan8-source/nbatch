package com.nbatch.job.core.files;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.io.File;

/**
 * 运行节点本地数据目录。
 */
public class RunNodeFilePath {

    @Getter
    private static String dataBasePath = System.getProperty("user.dir")
            .concat(File.separator).concat("data")
            .concat(File.separator).concat("jobhandler");

    /** 运行节点脚本文件目录。 */
    @Getter
    private static String scriptPath = new File(dataBasePath, "gluesource").getPath();

    public static void initDataPath(String dataPath) {
        if (StrUtil.isNotBlank(dataPath)) {
            dataBasePath = dataPath;
        }
        File dataPathDir = new File(dataBasePath);
        if (!dataPathDir.exists()) {
            FileUtil.mkdir(dataPathDir);
        }
        dataBasePath = dataPathDir.getPath();

        File scriptPathDir = new File(dataPathDir, "gluesource");
        if (!scriptPathDir.exists()) {
            FileUtil.mkdir(scriptPathDir);
        }
        scriptPath = scriptPathDir.getPath();
    }

}
