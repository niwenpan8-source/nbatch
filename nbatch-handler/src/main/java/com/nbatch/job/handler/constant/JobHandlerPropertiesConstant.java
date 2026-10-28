package com.nbatch.job.handler.constant;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

/**
 * job handler 配置属性常量
 */
@Data
public class JobHandlerPropertiesConstant {

    /**
     * 文件临时路径
     */
    @Value("${nbatch.job.file.path}")
    private String path;

    /**
     * 文件远程临时路径
     */
    @Value("${nbatch.job.file.remotePath}")
    private String remotePath;


}
