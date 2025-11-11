package com.nbatch.job.admin.core.alarm;

import com.nbatch.job.admin.core.domain.po.JobInfoPo;
import com.nbatch.job.admin.core.domain.po.JobLogPo;

/**
 * @author Mr.ni 2020-01-19
 */
public interface JobAlarm {

    /**
     * job alarm
     */
    boolean doAlarm(JobInfoPo info, JobLogPo jobLog);

}
