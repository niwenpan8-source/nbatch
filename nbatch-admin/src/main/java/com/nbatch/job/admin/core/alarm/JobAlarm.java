package com.nbatch.job.admin.core.alarm;

import com.nbatch.job.admin.core.model.XxlJobInfo;
import com.nbatch.job.admin.core.model.XxlJobLog;

/**
 * @author Mr.ni 2020-01-19
 */
public interface JobAlarm {

    /**
     * job alarm
     */
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog);

}
