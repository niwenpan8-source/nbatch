package com.nbatch.job.admin.core.domain.param;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description: 作业节点导出文件表
 * @author: Mr.ni
 * @date: 2025-11-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobWorkExportFileParam {

    /**
     * 导出文件id
     */
    private String exportFileId;

    /**
     * 作业节点id
     */
    private String nodeId;

    /**
     * 导出的文件名
     */
    private String fileName;

    /**
     * 导出的表名
     */
    private String exportTableName;

    /**
     * 导出的列
     */
    private String exportTableFiled;

    /**
     * 删除条件
     */
    private String exportTableCondition;

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

}
