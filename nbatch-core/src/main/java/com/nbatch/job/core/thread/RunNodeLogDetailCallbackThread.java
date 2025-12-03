package com.nbatch.job.core.thread;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjUtil;
import com.nbatch.job.core.biz.AdminBiz;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.RunNodeLogDetailParam;
import com.nbatch.job.core.enums.RegistryConfig;
import com.nbatch.job.core.executor.BatchJobExecutor;
import com.nbatch.job.core.log.JobFileAppender;
import com.nbatch.job.core.util.JdkSerializeTool;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 运行节点日志详情回调线程
 * @author Mr.ni
 * @date 2025/11/05
 */
@Slf4j
public class RunNodeLogDetailCallbackThread {

    private static final RunNodeLogDetailCallbackThread INSTANCE = new RunNodeLogDetailCallbackThread();

    public static RunNodeLogDetailCallbackThread getInstance() {
        return INSTANCE;
    }

    /**
     * job results callback queue
     */
    private final LinkedBlockingQueue<RunNodeLogDetailParam> callBackQueue = new LinkedBlockingQueue<>();

    public static void pushRunNodeLogDetailCallback(RunNodeLogDetailParam callback) {
        getInstance().callBackQueue.add(callback);
        log.debug(">>>>>>>>>>> job, push run node log detail callback request, workId:{}, runWorkId:{}, nodeId:{}, runNodeId:{}"
                , callback.getWorkId(), callback.getRunWorkId(), callback.getNodeId(), callback.getRunNodeId());
    }

    /**
     * callback thread
     */
    private Thread logDetailCallbackThread;
    private Thread logDetailRetryCallbackThread;
    private volatile boolean toStop = false;

    public void start() {

        // valid
        if (BatchJobExecutor.getAdminBizList() == null) {
            log.warn(">>>>>>>>>>> job, run node log detail callback config fail, adminAddresses is null.");
            return;
        }

        // callback
        logDetailCallbackThread = new Thread(() -> {

            // normal callback
            while (!toStop) {
                try {
                    RunNodeLogDetailParam callback = getInstance().callBackQueue.take();
                    if (ObjUtil.isNotEmpty(callback)) {

                        // callback list param
                        List<RunNodeLogDetailParam> callbackParamList = new ArrayList<>();
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
                List<RunNodeLogDetailParam> callbackParamList = new ArrayList<>();
                int drainToNum = getInstance().callBackQueue.drainTo(callbackParamList);
                if (CollUtil.isNotEmpty(callbackParamList)) {
                    doCallback(callbackParamList);
                }
            } catch (Throwable e) {
                if (!toStop) {
                    log.error(e.getMessage(), e);
                }
            }
            log.info(">>>>>>>>>>> job, run node log detail callback thread destroy.");

        });
        logDetailCallbackThread.setDaemon(true);
        logDetailCallbackThread.setName("job, run node log detail logDetailCallbackThread");
        logDetailCallbackThread.start();


        // retry
        logDetailRetryCallbackThread = new Thread(() -> {
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
            log.info(">>>>>>>>>>> job, run node log detail retry callback thread destroy.");
        });
        logDetailRetryCallbackThread.setDaemon(true);
        logDetailRetryCallbackThread.start();

    }

    public void toStop() {
        toStop = true;
        // stop callback, interrupt and wait
        // support empty admin address
        if (logDetailCallbackThread != null) {
            logDetailCallbackThread.interrupt();
            try {
                // 用于等待回调线程执行完毕。当主线程调用此方法时，会阻塞当前线程直到 logDetailCallbackThread 线程终止，
                // 确保线程安全停止。这是线程管理的标准做法，保证线程优雅关闭。
                logDetailCallbackThread.join();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }

        // stop retry, interrupt and wait
        if (logDetailRetryCallbackThread != null) {
            logDetailRetryCallbackThread.interrupt();
            try {
                logDetailRetryCallbackThread.join();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }

    }

    /**
     * do callback, will retry if error
     * @param callbackParamList 回调参数list
     */
    private void doCallback(List<RunNodeLogDetailParam> callbackParamList) {
        boolean callbackRet = false;
        // callback, will retry if error
        for (AdminBiz adminBiz : BatchJobExecutor.getAdminBizList()) {
            try {
                ReturnT<String> callbackResult = adminBiz.callbackRunNodeLogDetail(callbackParamList);
                if (callbackResult != null && ReturnT.SUCCESS_CODE == callbackResult.getCode()) {
                    log.debug(">>>>>>>>>>> job, run node log detail callback finish.");
                    callbackRet = true;
                    break;
                } else {
                    log.debug("<br>----------- job job callback fail, callbackResult:{}", callbackResult);
                }
            } catch (Throwable e) {
                log.error(">>>>>>>>>>> job, run node log detail callback error, logDetail:{}", callbackParamList, e);
            }
        }
        if (!callbackRet) {
            appendFailCallbackFile(callbackParamList);
        }
    }

    // ---------------------- fail-callback file ----------------------

    private static final String FAIL_CALLBACK_FILE_PATH =
            JobFileAppender.getLogPath().concat(File.separator).concat("callbacknodelog").concat(File.separator);
    private static final String FAIL_CALLBACK_FILE_NAME = FAIL_CALLBACK_FILE_PATH.concat("run-node-job-detail-callback-{x}").concat(".log");

    /**
     * 将发送失败的参数缓存到文件当中等待重试
     */
    private void appendFailCallbackFile(List<RunNodeLogDetailParam> callbackParamList) {
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

            List<RunNodeLogDetailParam> callbackParamList = (List<RunNodeLogDetailParam>) JdkSerializeTool.deserialize(callbackParamListBytes, List.class);
            FileUtil.del(callbaclLogFile);
            doCallback(callbackParamList);
        }

    }

}
