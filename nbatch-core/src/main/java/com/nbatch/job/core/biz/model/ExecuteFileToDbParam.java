package com.nbatch.job.core.biz.model;

import lombok.Data;

/**
 * @description: 数据库导出到文件参数
 * @author: Mr.ni
 * @date: 2025/11/21
 */
@Data
public class ExecuteFileToDbParam {

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

}
