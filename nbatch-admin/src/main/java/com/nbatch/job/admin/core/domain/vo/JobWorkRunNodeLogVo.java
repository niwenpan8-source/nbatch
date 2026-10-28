package com.nbatch.job.admin.core.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 作业运行节点日志
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class JobWorkRunNodeLogVo {

    /**
     * 节点日志id
     */
    private String nodeLogId;

    /**
     * 作业id
     */
    private String workId;

    /**
     * 作业节点id
     */
    private String nodeId;

    /**
     * 节点名称，仅用于日志展示。
     */
    private String nodeName;

    private Date turnDate;

    /**
     * 执行状态
     */
    private Integer handleCode;

    /**
     * 执行信息
     */
    private String handleMsg;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 实际开始执行时间
     */
    private LocalDateTime executeStartTime;

    /**
     * 实际结束执行时间
     */
    private LocalDateTime executeEndTime;

    /**
     * 回调时间
     */
    private LocalDateTime callBackTime;

    /**
     * 日志详情
     */
    private String logDetail;

}
