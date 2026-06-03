package com.nbatch.job.core.thread;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjUtil;
import com.nbatch.job.core.biz.AdminBiz;
import com.nbatch.job.core.biz.model.HandleCallbackParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.context.BatchJobContext;
import com.nbatch.job.core.context.BatchJobHelper;
import com.nbatch.job.core.enums.RegistryConfig;
import com.nbatch.job.core.executor.BatchJobExecutor;
import com.nbatch.job.core.log.JobFileAppender;
import com.nbatch.job.core.util.JdkSerializeTool;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 触发回调线程
 * @author Mr.ni
 * @date 2025/11/05
 */
@Slf4j
public class TriggerCallbackThread {

    private static final TriggerCallbackThread INSTANCE = new TriggerCallbackThread();

    public static TriggerCallbackThread getInstance() {
        return INSTANCE;
    }

    /**
     * job results callback queue
     */
    private final LinkedBlockingQueue<HandleCallbackParam> callBackQueue = new LinkedBlockingQueue<>();

    public static void pushCallBack(HandleCallbackParam callback) {
        getInstance().callBackQueue.add(callback);
        log.debug(">>>>>>>>>>> job, push callback request, logId:{}", callback.getLogId());
    }

    /**
     * callback thread
     */
    private Thread triggerCallbackThread;
    private Thread triggerRetryCallbackThread;
    private volatile boolean toStop = false;

    public void start() {

        // valid
        if (BatchJobExecutor.getAdminBizList() == null) {
            log.warn(">>>>>>>>>>> job, executor callback config fail, adminAddresses is null.");
            return;
        }

        // callback
        triggerCallbackThread = new Thread(() -> {

            // normal callback
            while (!toStop) {
                try {
                    HandleCallbackParam callback = getInstance().callBackQueue.take();
                    if (ObjUtil.isNotEmpty(callback)) {

                        // callback list param
                        List<HandleCallbackParam> callbackParamList = new ArrayList<>();
                        int drainToNum = getInstance().callBackQueue.drainTo(callbackParamList);
                        callbackParamList.add(callback);

                        // callback, will retry if error
                        if (CollUtil.isNotEmpty(callbackParamList)) {
                            doCallback(callbackParamList);
                        }
                    }
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                    }
                }
            }

            // last callback
            try {
                List<HandleCallbackParam> callbackParamList = new ArrayList<>();
                int drainToNum = getInstance().callBackQueue.drainTo(callbackParamList);
                if (CollUtil.isNotEmpty(callbackParamList)) {
                    doCallback(callbackParamList);
                }
            } catch (Throwable e) {
                if (!toStop) {
                    log.error(e.getMessage(), e);
                }
            }
            log.info(">>>>>>>>>>> job, executor callback thread destroy.");

        });
        triggerCallbackThread.setDaemon(true);
        triggerCallbackThread.setName("job, executor TriggerCallbackThread");
        triggerCallbackThread.start();


        // retry
        triggerRetryCallbackThread = new Thread(() -> {
            while (!toStop) {
                try {
                    retryFailCallbackFile();
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                    }

                }
                try {
                    TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
            log.info(">>>>>>>>>>> job, executor retry callback thread destroy.");
        });
        triggerRetryCallbackThread.setDaemon(true);
        triggerRetryCallbackThread.start();

    }

    public void toStop() {
        toStop = true;
        // stop callback, interrupt and wait
        // support empty admin address
        if (triggerCallbackThread != null) {
            triggerCallbackThread.interrupt();
            try {
                triggerCallbackThread.join();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }

        // stop retry, interrupt and wait
        if (triggerRetryCallbackThread != null) {
            triggerRetryCallbackThread.interrupt();
            try {
                triggerRetryCallbackThread.join();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }

    }

    /**
     * do callback, will retry if error
     * @param callbackParamList 回调参数list
     */
    private void doCallback(List<HandleCallbackParam> callbackParamList) {
        boolean callbackRet = false;
        // callback, will retry if error
        for (AdminBiz adminBiz : BatchJobExecutor.getAdminBizList()) {
            try {
                ReturnT<String> callbackResult = adminBiz.callback(callbackParamList);
                if (callbackResult != null && HandleCodeConstant.HANDLE_CODE_SUCCESS == callbackResult.getCode()) {
                    callbackLog(callbackParamList, "<br>----------- job job callback finish.");
                    callbackRet = true;
                    break;
                } else {
                    callbackLog(callbackParamList, "<br>----------- job job callback fail, callbackResult:" + callbackResult);
                }
            } catch (Throwable e) {
                callbackLog(callbackParamList, "<br>----------- job job callback error, errorMsg:" + e.getMessage());
            }
        }
        if (!callbackRet) {
            appendFailCallbackFile(callbackParamList);
        }
    }

    /**
     * callback log
     */
    private void callbackLog(List<HandleCallbackParam> callbackParamList, String logContent) {
        for (HandleCallbackParam callbackParam : callbackParamList) {
            if (callbackParam.getLogCallBackParam().getLogDateTim() <= 0) {
                continue;
            }
            String logFileName = JobFileAppender.makeLogFileName(new Date(callbackParam.getLogCallBackParam().getLogDateTim()), callbackParam.getLogId());
            BatchJobContext.setBatchJobContext(new BatchJobContext(
                    null,
                    null,
                    logFileName,
                    -1,
                    -1));
            BatchJobHelper.log(logContent);
        }
    }


    // ---------------------- fail-callback file ----------------------

    private static final String FAIL_CALLBACK_FILE_PATH = JobFileAppender.getLogPath().concat(File.separator).concat("callbacklog").concat(File.separator);
    private static final String FAIL_CALLBACK_FILE_NAME = FAIL_CALLBACK_FILE_PATH.concat("job-callback-{x}").concat(".log");

    private void appendFailCallbackFile(List<HandleCallbackParam> callbackParamList) {
        // valid
        if (CollUtil.isEmpty(callbackParamList)) {
            return;
        }

        // append file
        byte[] callbackParamListBytes = JdkSerializeTool.serialize(callbackParamList);

        File callbackLogFile = new File(FAIL_CALLBACK_FILE_NAME.replace("{x}", String.valueOf(System.currentTimeMillis())));
        if (callbackLogFile.exists()) {
            for (int i = 0; i < 100; i++) {
                callbackLogFile = new File(FAIL_CALLBACK_FILE_NAME.replace("{x}", String.valueOf(System.currentTimeMillis()).concat("-").concat(String.valueOf(i))));
                if (!callbackLogFile.exists()) {
                    break;
                }
            }
        }
        if (callbackParamListBytes != null) {
            FileUtil.writeBytes(callbackParamListBytes, callbackLogFile);
        }

    }

    private void retryFailCallbackFile() {

        // valid
        File callbackLogPath = new File(FAIL_CALLBACK_FILE_PATH);
        if (!callbackLogPath.exists()) {
            return;
        }
        if (callbackLogPath.isFile()) {
            FileUtil.del(callbackLogPath);
        }
        if (!(callbackLogPath.isDirectory() && ArrayUtil.isNotEmpty(callbackLogPath.list()))) {
            return;
        }

        // load and clear file, retry
        for (File callbaclLogFile : Objects.requireNonNull(callbackLogPath.listFiles())) {
            byte[] callbackParamListBytes = FileUtil.readBytes(callbaclLogFile);

            // avoid empty file
            if (callbackParamListBytes == null || callbackParamListBytes.length < 1) {
                FileUtil.del(callbaclLogFile);
                continue;
            }

            List<HandleCallbackParam> callbackParamList = (List<HandleCallbackParam>) JdkSerializeTool.deserialize(callbackParamListBytes, List.class);
            FileUtil.del(callbaclLogFile);
            doCallback(callbackParamList);
        }

    }

}
