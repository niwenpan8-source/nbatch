package com.nbatch.job.core.context;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.log.JobFileAppender;
import com.nbatch.job.core.util.ThrowableUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.util.Date;

/**
 * helper for job
 *
 * @author Mr.ni
 */
@Slf4j
public class BatchJobHelper {

    // ---------------------- base info ----------------------

    /**
     * current JobId
     */
    public static String getJobId() {
        BatchJobContext xxlJobContext = BatchJobContext.getBatchJobContext();
        if (xxlJobContext == null) {
            return null;
        }

        return xxlJobContext.getJobId();
    }

    /**
     * current JobParam
     */
    public static String getJobParam() {
        BatchJobContext xxlJobContext = BatchJobContext.getBatchJobContext();
        if (xxlJobContext == null) {
            return null;
        }

        return xxlJobContext.getJobParam();
    }

    // ---------------------- for log ----------------------

    /**
     * current JobLogFileName
     */
    public static String getJobLogFileName() {
        BatchJobContext xxlJobContext = BatchJobContext.getBatchJobContext();
        if (xxlJobContext == null) {
            return null;
        }

        return xxlJobContext.getJobLogFileName();
    }

    // ---------------------- for shard ----------------------

    /**
     * current ShardIndex
     */
    public static int getShardIndex() {
        BatchJobContext xxlJobContext = BatchJobContext.getBatchJobContext();
        if (xxlJobContext == null) {
            return -1;
        }

        return xxlJobContext.getShardIndex();
    }

    /**
     * current ShardTotal
     */
    public static int getShardTotal() {
        BatchJobContext xxlJobContext = BatchJobContext.getBatchJobContext();
        if (xxlJobContext == null) {
            return -1;
        }

        return xxlJobContext.getShardTotal();
    }

    // ---------------------- tool for log ----------------------


    /**
     * append log with pattern
     *
     * @param appendLogPattern   like "aaa {} bbb {} ccc"
     * @param appendLogArguments like "111, true"
     */
    public static boolean log(String appendLogPattern, Object... appendLogArguments) {

        FormattingTuple ft = MessageFormatter.arrayFormat(appendLogPattern, appendLogArguments);
        String appendLog = ft.getMessage();

        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        return logDetail(callInfo, appendLog);
    }

    /**
     * append exception stack
     */
    public static boolean log(Throwable e) {

        String appendLog = ThrowableUtil.toString(e);

        StackTraceElement callInfo = new Throwable().getStackTrace()[1];
        return logDetail(callInfo, appendLog);
    }

    /**
     * append log
     *
     * @param callInfo  call info
     * @param appendLog append log
     */
    private static boolean logDetail(StackTraceElement callInfo, String appendLog) {
        BatchJobContext xxlJobContext = BatchJobContext.getBatchJobContext();
        if (xxlJobContext == null) {
            return false;
        }

        String formatAppendLog = DateUtil.formatDateTime(new Date()) + " " +
                "[" + callInfo.getClassName() + "#" + callInfo.getMethodName() + "]" + "-" +
                "[" + callInfo.getLineNumber() + "]" + "-" +
                "[" + Thread.currentThread().getName() + "]" + " " +
                (appendLog != null ? appendLog : "");

        // appendlog
        String logFileName = xxlJobContext.getJobLogFileName();

        if (StrUtil.isNotBlank(logFileName)) {
            JobFileAppender.appendLog(logFileName, formatAppendLog);
            return true;
        } else {
            log.info(">>>>>>>>>>> {}", formatAppendLog);
            return false;
        }
    }

    // ---------------------- tool for handleResult ----------------------

    /**
     * handle success
     */
    public static boolean handleSuccess() {
        return handleResult(HandleCodeConstant.HANDLE_CODE_SUCCESS, null);
    }

    /**
     * handle success with log msg
     *
     * @param handleMsg handleMsg
     */
    public static boolean handleSuccess(String handleMsg) {
        return handleResult(HandleCodeConstant.HANDLE_CODE_SUCCESS, handleMsg);
    }

    /**
     * handle fail
     */
    public static boolean handleFail() {
        return handleResult(HandleCodeConstant.HANDLE_CODE_FAIL, null);
    }

    /**
     * handle fail with log msg
     *
     * @param handleMsg handleMsg
     */
    public static boolean handleFail(String handleMsg) {
        return handleResult(HandleCodeConstant.HANDLE_CODE_FAIL, handleMsg);
    }

    /**
     * handle fail with throwable
     *
     * @param e throwable
     */
    public static boolean handleFail(Throwable e) {
        return handleResult(HandleCodeConstant.HANDLE_CODE_FAIL, ThrowableUtil.toString(e));
    }

    /**
     * handle timeout
     */
    public static boolean handleTimeout() {
        return handleResult(HandleCodeConstant.HANDLE_CODE_TIMEOUT, null);
    }

    /**
     * handle timeout with log msg
     *
     * @param handleMsg handleMsg
     */
    public static boolean handleTimeout(String handleMsg) {
        return handleResult(HandleCodeConstant.HANDLE_CODE_TIMEOUT, handleMsg);
    }

    /**
     * @param handleCode 200 : success
     *                   500 : fail
     *                   502 : timeout
     * @param handleMsg  处理信息
     */
    public static boolean handleResult(int handleCode, String handleMsg) {
        BatchJobContext xxlJobContext = BatchJobContext.getBatchJobContext();
        if (xxlJobContext == null) {
            return false;
        }

        xxlJobContext.setHandleCode(handleCode);
        if (handleMsg != null) {
            xxlJobContext.setHandleMsg(handleMsg);
        }
        return true;
    }


}
