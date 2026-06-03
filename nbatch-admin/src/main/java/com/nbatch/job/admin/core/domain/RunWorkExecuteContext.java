package com.nbatch.job.admin.core.domain;

import com.nbatch.job.core.biz.model.TriggerParam;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 运行作业继续调度上下文，仅作为内存缓存；丢失后可由数据库重建。
 */
@Data
@Accessors(chain = true)
public class RunWorkExecuteContext {

    private String runWorkId;

    private String address;

    private TriggerParam triggerParam;

    private long updateTime;

}
