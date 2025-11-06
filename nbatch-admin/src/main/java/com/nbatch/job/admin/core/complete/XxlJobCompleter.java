package com.nbatch.job.admin.core.complete;

import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.admin.core.conf.XxlJobAdminConfig;
import com.nbatch.job.admin.core.model.XxlJobInfo;
import com.nbatch.job.admin.core.model.XxlJobLog;
import com.nbatch.job.admin.core.thread.JobTriggerPoolHelper;
import com.nbatch.job.admin.core.trigger.TriggerTypeEnum;
import com.nbatch.job.admin.core.util.I18nUtil;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.context.XxlJobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

/**
 * @author Mr.ni 2020-10-30 20:43:10
 */
public class XxlJobCompleter {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobCompleter.class);

    /**
     * common fresh handle entrance (limit only once)
     */
    public static int updateHandleInfoAndFinish(XxlJobLog xxlJobLog) {

        // finish
        finishJob(xxlJobLog);

        // text最大64kb 避免长度过长
        if (xxlJobLog.getHandleMsg().length() > 15000) {
            xxlJobLog.setHandleMsg( xxlJobLog.getHandleMsg().substring(0, 15000) );
        }

        // fresh handle
        return XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateHandleInfo(xxlJobLog);
    }


    /**
     * do somethind to finish job
     */
    private static void finishJob(XxlJobLog xxlJobLog){

        // 1、handle success, to trigger child job
        StringBuilder triggerChildMsg = null;
        if (XxlJobContext.HANDLE_CODE_SUCCESS == xxlJobLog.getHandleCode()) {
            XxlJobInfo xxlJobInfo = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().loadById(xxlJobLog.getJobId());
            if (xxlJobInfo != null && StrUtil.isNotBlank(xxlJobInfo.getChildJobId())) {
                triggerChildMsg = new StringBuilder("<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>" + I18nUtil.getString("jobconf_trigger_child_run") + "<<<<<<<<<<< </span><br>");

                String[] childJobIds = xxlJobInfo.getChildJobId().split(StrPool.COMMA);
                for (int i = 0; i < childJobIds.length; i++) {
                    int childJobId = (StrUtil.isNotBlank(childJobIds[i]) && isNumeric(childJobIds[i])) ? NumberUtil.parseInt(childJobIds[i]) :-1;
                    if (childJobId > 0) {
                        // valid
                        if (childJobId == xxlJobLog.getJobId()) {
                            logger.debug(">>>>>>>>>>> xxl-job, XxlJobCompleter-finishJob ignore childJobId,  childJobId {} is self.", childJobId);
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
                                (triggerChildResult.getCode() == ReturnT.SUCCESS_CODE ? I18nUtil.getString("system_success") : I18nUtil.getString("system_fail")),
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
            xxlJobLog.setHandleMsg( xxlJobLog.getHandleMsg() + triggerChildMsg );
        }

        // 2、fix_delay trigger next
        // on the way

    }

    private static boolean isNumeric(String str){
        try {
            int result = NumberUtil.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
