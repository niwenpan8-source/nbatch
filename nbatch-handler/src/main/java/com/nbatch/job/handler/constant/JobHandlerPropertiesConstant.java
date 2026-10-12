package com.nbatch.job.handler.constant;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

/**
 * @description:
 * @author: Mr.ni
 * @date: 2025/11/25
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
