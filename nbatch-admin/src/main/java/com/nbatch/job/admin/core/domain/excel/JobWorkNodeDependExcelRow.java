package com.nbatch.job.admin.core.domain.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 作业流程依赖导入、导出行。
 *
 * 类型和标识列用于导入，名称列仅用于人工核对。
 */
@Data
public class JobWorkNodeDependExcelRow {

    @ExcelProperty(value = "前置对象类型", index = 0)
    @ColumnWidth(16)
    private String predecessorType;

    @ExcelProperty(value = "前置对象ID", index = 1)
    @ColumnWidth(24)
    private String predecessorId;

    @ExcelProperty(value = "前置对象名称", index = 2)
    @ColumnWidth(30)
    private String predecessorName;

    @ExcelProperty(value = "后置对象类型", index = 3)
    @ColumnWidth(16)
    private String successorType;

    @ExcelProperty(value = "后置对象ID", index = 4)
    @ColumnWidth(24)
    private String successorId;

    @ExcelProperty(value = "后置对象名称", index = 5)
    @ColumnWidth(30)
    private String successorName;
}
