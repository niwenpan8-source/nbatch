package com.nbatch.job.core.biz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 处理回调
 * @author Mr.ni
 * @date 2025/11/20
 */
@Data
@ToString
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class HandleCallbackParam implements Serializable {

    private static final long serialVersionUID = 42L;

    private String logId;

    private String callBackType;

    private LogCallBackParam logCallBackParam = new LogCallBackParam();

    private NodeStatusCallbackParam nodeStatusCallbackParam = new NodeStatusCallbackParam();

    /**
     * 日志回调参数
     */
    @Data
    @Accessors(chain = true)
    public static class LogCallBackParam {

        /**
         * 日志时间
         */
        private long logDateTim;

        /**
         * 处理结果
         */
        private int handleCode;

        /**
         * 处理信息
         */
        private String handleMsg;

    }

    /**
     * 节点状态回调参数
     */
    @Data
    @Accessors(chain = true)
    public static class NodeStatusCallbackParam {

        /**
         * 作业id
         */
        private String workId;

        /**
         * 节点id
         */
        private String nodeId;

        /**
         * 处理时间
         */
        private long handleTime;

        /**
         * 任务执行状态
         * 1: 成功
         * 0: 失败
         */
        private int handleCode;

        /**
         * 任务执行信息
         */
        private String handleMsg;

    }


}
