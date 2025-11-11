package com.nbatch.job.admin.core.domain.param;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @description: 任务锁
 * @author: Mr.ni
 * @date: 2025/11/6
 */
@Data
@Accessors(chain = true)
@TableName("nbatch_job_lock")
public class JobLockParam {

    /**
     * 锁名称
     */
    private String lockName;

}
