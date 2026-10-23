package com.nbatch.job.admin.core.domain.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class JobWorkNodeExcelRow {

    @ExcelProperty("节点标识")
    @ColumnWidth(24)
    private String sourceNodeId;

    @ExcelProperty("所属组标识")
    @ColumnWidth(24)
    private String sourceGroupId;

    @ExcelProperty("节点名称")
    @ColumnWidth(30)
    private String nodeName;

    @ExcelProperty("节点说明")
    @ColumnWidth(40)
    private String nodeDesc;

    @ExcelProperty("数据库类型")
    @ColumnWidth(16)
    private String dbType;

    @ExcelProperty("执行内容")
    @ColumnWidth(48)
    private String executeContent;

    @ExcelProperty("执行参数")
    @ColumnWidth(32)
    private String executeContentParam;

    @ExcelProperty("执行器")
    @ColumnWidth(30)
    private String executeHandler;

    @ExcelProperty("脚本类型")
    @ColumnWidth(16)
    private String scriptType;

    @ExcelProperty("失败策略")
    @ColumnWidth(14)
    private String errorStrategy;

    @ExcelProperty("重试次数")
    @ColumnWidth(12)
    private Integer retryTimes;

    @ExcelProperty("文件名")
    @ColumnWidth(28)
    private String fileName;

    @ExcelProperty("表名")
    @ColumnWidth(28)
    private String tableName;

    @ExcelProperty("字段")
    @ColumnWidth(36)
    private String tableFields;

    @ExcelProperty("条件")
    @ColumnWidth(36)
    private String tableCondition;

    @ExcelProperty("文件编码")
    @ColumnWidth(14)
    private String fileCode;

    @ExcelProperty("分隔符")
    @ColumnWidth(12)
    private String separator;

    @ExcelProperty("全量更新")
    @ColumnWidth(12)
    private Integer allUpdate;

    @ExcelProperty("Gzip压缩")
    @ColumnWidth(12)
    private Integer gzip;

    @ExcelProperty("文件名参数")
    @ColumnWidth(28)
    private String fileNameParam;
}
