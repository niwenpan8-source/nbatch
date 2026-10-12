package com.nbatch.job.core.biz.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 运行节点本地日志拉取结果。
 */
@Data
@Accessors(chain = true)
public class RunNodeLogPullResult implements Serializable {

    private static final long serialVersionUID = 42L;

    private Long ackOffset;
    private Long nextOffset;

    /** 租约令牌 */
    private String leaseToken;

    /** 已拉取的通用日志记录。 */
    private List<LogRecord> logList;

    /**
     * 本地可靠日志记录。
     * payload 由状态日志和详细日志各自反序列化，通用层不依赖业务模型。
     */
    @Data
    @Accessors(chain = true)
    public static class LogRecord implements Serializable {

        private static final long serialVersionUID = 42L;

        private Long offset;

        private Long timestamp;

        private String payload;
    }
}
