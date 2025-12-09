package com.nbatch.job.admin.core.complete;

import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.admin.core.domain.po.JobInfoPo;
import com.nbatch.job.admin.core.domain.po.JobLogPo;
import com.nbatch.job.admin.core.enums.TriggerTypeEnum;
import com.nbatch.job.admin.core.thread.JobTriggerPoolHelper;
import com.nbatch.job.admin.core.util.I18nUtil;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;

/**
 * job completion
 * @author Mr.ni
 */
@Slf4j
public class JobCompleter {

    /**
     * common fresh handle entrance (limit only once)
     */
    public static int updateHandleInfoAndFinish(JobLogPo jobLogPo) {
        // finish
        finishJob(jobLogPo);

        // text最大64kb 避免长度过长
        if (jobLogPo.getHandleMsg().length() > 15000) {
            jobLogPo.setHandleMsg(jobLogPo.getHandleMsg().substring(0, 15000));
        }

        // fresh handle
        return JobAdminConfig.getAdminConfig().getJobLogMapper().updateById(jobLogPo);
    }


    /**
     * do somethind to finish job
     */
    private static void finishJob(JobLogPo jobLogPo) {

        // 1、handle success, to trigger child job
        StringBuilder triggerChildMsg = null;
        if (HandleCodeConstant.HANDLE_CODE_SUCCESS == jobLogPo.getHandleCode()) {
            JobInfoPo jobInfo = JobAdminConfig.getAdminConfig().getJobInfoMapper().selectById(jobLogPo.getJobId());
            if (jobInfo != null && StrUtil.isNotBlank(jobInfo.getChildJobid())) {
                triggerChildMsg = new StringBuilder("<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>" + I18nUtil.getString("jobconf_trigger_child_run") + "<<<<<<<<<<< </span><br>");

                String[] childJobIds = jobInfo.getChildJobid().split(StrPool.COMMA);
                for (int i = 0; i < childJobIds.length; i++) {
                    String childJobId = (StrUtil.isNotBlank(childJobIds[i]) && NumberUtil.isNumber(childJobIds[i])) ? childJobIds[i] : "-1";
                    if (NumberUtil.parseInt(childJobId) > 0) {
                        // valid
                        if (StrUtil.equals(childJobId, jobLogPo.getJobId())) {
                            log.debug(">>>>>>>>>>> job, XxlJobCompleter-finishJob ignore childJobId,  childJobId {} is self.", childJobId);
                            continue;
                        }

                        // trigger child job
                        JobTriggerPoolHelper.trigger(childJobId, TriggerTypeEnum.PARENT, -1, null, null, null);
                        ReturnT<String> triggerChildResult = ReturnT.SUCCESS;

                        // add msg
                        triggerChildMsg.append(MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg1"),
                                (i + 1),
                                childJobIds.length,
                                childJobIds[i],
                                (triggerChildResult.getCode() == HandleCodeConstant.HANDLE_CODE_SUCCESS ? I18nUtil.getString("system_success") : I18nUtil.getString("system_fail")),
                                triggerChildResult.getMsg()));
                    } else {
                        triggerChildMsg.append(MessageFormat.format(I18nUtil.getString("jobconf_callback_child_msg2"),
                                (i + 1),
                                childJobIds.length,
                                childJobIds[i]));
                    }
                }

            }
        }

        if (triggerChildMsg != null) {
            jobLogPo.setHandleMsg(jobLogPo.getHandleMsg() + triggerChildMsg);
        }

    }

}
