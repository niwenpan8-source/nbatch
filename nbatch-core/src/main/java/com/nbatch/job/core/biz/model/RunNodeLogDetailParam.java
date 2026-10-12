package com.nbatch.job.core.biz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * @description: 作业运行节点详细日志
 * @author: Mr.ni
 * @date: 2025-12-03
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class RunNodeLogDetailParam {

    /** 详细日志标识。 */
    private String detailLogId;

    /** 作业标识 */
    private String workId;

    /** 节点执行日志标识 */
    private String nodeLogId;

    /** 步骤标识 */
    private String nodeId;

    /**
     * 翻牌日期
     */
    private Date turnDate;

    /**
     * 执行信息
     */
    private String handleMsg;

    /**
     * 回调时间
     */
    private LocalDateTime executeTime;

    /**
     * 执行-时间
     */
    private LocalDateTime callBackTime;

}
