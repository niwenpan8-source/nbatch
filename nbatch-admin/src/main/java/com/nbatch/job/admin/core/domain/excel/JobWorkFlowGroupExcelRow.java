package com.nbatch.job.admin.core.domain.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class JobWorkFlowGroupExcelRow {

    @ExcelProperty(value = "组标识", index = 0)
    @ColumnWidth(24)
    private String sourceGroupId;

    @ExcelProperty(value = "组名称", index = 1)
    @ColumnWidth(30)
    private String groupName;

    @ExcelProperty(value = "组说明", index = 2)
    @ColumnWidth(40)
    private String groupDesc;

    @ExcelProperty(value = "组类型", index = 3)
    @ColumnWidth(14)
    private String groupType;

    @ExcelProperty(value = "排序", index = 4)
    @ColumnWidth(12)
    private Integer sortOrder;
}
