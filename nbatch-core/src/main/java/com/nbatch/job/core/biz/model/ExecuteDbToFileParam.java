package com.nbatch.job.core.biz.model;

import lombok.Data;

/**
 * @description: 执行数据库导出到文件参数
 * @author: Mr.ni
 * @date: 2025/11/21
 */
@Data
public class ExecuteDbToFileParam {

    /**
     * 导出的文件名
     */
    private String fileName;

    /**
     * 导出文件路径
     */
    private String filePath;

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

    /**
     * 数据库类型
     */
    private String dbType;

}
