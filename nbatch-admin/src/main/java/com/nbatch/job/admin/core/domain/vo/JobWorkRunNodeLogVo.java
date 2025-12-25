package com.nbatch.job.admin.core.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * @description: 作业运行节点日志
 * @author: Mr.ni
 * @date: 2025-11-25
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
    private String runWorkId;

    /**
     * 作业节点id
     */
    private String nodeId;
    private String runNodeId;

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
     * 回调时间
     */
    private LocalDateTime callBackTime;

    /**
     * 日志详情
     */
    private String logDetail;

}
