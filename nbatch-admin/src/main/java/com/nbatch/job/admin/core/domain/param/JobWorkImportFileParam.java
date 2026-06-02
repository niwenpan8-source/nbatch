package com.nbatch.job.admin.core.domain.param;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @description: 作业节点导入节点表
 * @author: Mr.ni
 * @date: 2025-11-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobWorkImportFileParam {

    /**
     * 导入文件id
     */
    private String importFileId;

    /**
     * 作业节点id
     */
    private String nodeId;

    /**
     * 导入的文件名
     */
    private String fileName;

    /**
     * 导入的表名
     */
    private String importTableName;

    /**
     * 导入的列
     */
    private String importTableFiled;

    /**
     * 导入条件
     */
    private String importTableCondition;

    /**
     * 文件编码
     */
    private String fileCode;

    /**
     * 分隔符
     */
    private String sep;

    /**
     * 是否全量文件：1全量 0增量
     */
    private Integer allUpdate;

    /**
     * 是否压缩：1压缩 0不压缩
     */
    private Integer isGzip;

    /**
     * 生成文件名，特殊替换字符
     */
    private String fileNameParam;

    private Date createTime;

    private Date updateTime;

}
