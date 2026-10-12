package com.nbatch.job.core.util;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import lombok.extern.slf4j.Slf4j;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 1、内嵌编译器如"PythonInterpreter"无法引用扩展包，因此推荐使用java调用控制台进程方式"Runtime.getRuntime().exec()"来运行脚本(shell或python)；
 * 2、因为通过java调用控制台进程方式实现，需要保证目标机器PATH路径正确配置对应编译器；
 * 3、暂时脚本执行日志只能在脚本执行结束后一次性获取，无法保证实时性；因此为确保日志实时性，可改为将脚本打印的日志存储在指定的日志文件上；
 * 4、python 异常输出优先级高于标准输出，体现在Log文件中，因此推荐通过logging方式打日志保持和异常信息一致；否则用printf日志顺序会错乱
 *
 * @author Mr.ni
 */
@Slf4j
public class ScriptUtil {

    /**
     * 脚本先等待正常退出，超时后再进入强制结束阶段。
     */
    private static final long PROCESS_GRACEFUL_STOP_SECONDS = 5L;
    private static final long PROCESS_FORCE_STOP_SECONDS = 5L;

    /**
     * nodeLogId -> 外部进程，用于停止请求精确定位脚本进程。
     */
    private static final Map<String, Process> RUN_NODE_PROCESS_MAP = new ConcurrentHashMap<>();

    /**
     * 创建脚本文件
     *
     * @param scriptFileName script file name
     * @param content        script content
     */
    public static void markScriptFile(String scriptFileName, String content) throws IOException {
        // 创建文件，路径格式为 filePath/gluesource/666-123456789.py
        try (FileOutputStream fileOutputStream = new FileOutputStream(scriptFileName)) {
            if (scriptFileName.toLowerCase().endsWith(".ps1")) {
                // Windows PowerShell 5.1 仅能可靠识别带 BOM 的 UTF-8 脚本。
                fileOutputStream.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            }
            fileOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 脚本执行，日志文件实时输出
     *
     * @param param      节点参数
     * @param command    命令
     * @param scriptFile 脚本文件
     * @param params     参数
     */
    public static int execToFile(ExecuteNodeParam param, String command, String scriptFile, String... params) {

        Thread inputThread = null;
        Thread errThread = null;
        Process process = null;
        try {
            // 执行命令
            List<String> cmdarray = new ArrayList<>();
            cmdarray.add(command);
            if ("powershell".equalsIgnoreCase(command)) {
                cmdarray.add("-NoProfile");
                cmdarray.add("-NonInteractive");
                cmdarray.add("-ExecutionPolicy");
                cmdarray.add("Bypass");
                cmdarray.add("-File");
            }
            cmdarray.add(scriptFile);
            if (ArrayUtil.isNotEmpty(params)) {
                Collections.addAll(cmdarray, params);
            }
            String[] cmdArrayFinal = cmdarray.toArray(new String[0]);
            // 启动进程
            process = Runtime.getRuntime().exec(cmdArrayFinal);
            final Process runningProcess = process;
            if (StrUtil.isNotBlank(param.getNodeLogId())) {
                RUN_NODE_PROCESS_MAP.put(param.getNodeLogId(), process);
            }
            inputThread = new Thread(() -> {
                String content = IoUtil.readUtf8(runningProcess.getInputStream());
                if (StrUtil.isNotBlank(content)) {
                    log.info("输出内容：{}", content);
                    param.pushRunNodeLogDetailCallback(content);
                }
            });
            errThread = new Thread(() -> {
                String content = IoUtil.readUtf8(runningProcess.getErrorStream());
                if (StrUtil.isNotBlank(content)) {
                    log.info("错误输出内容：{}", content);
                    param.pushRunNodeLogDetailCallback(content);
                }
            });
            inputThread.start();
            errThread.start();

            // 等待进程结束
            // 退出码：0 表示成功，1 表示失败
            int exitValue = process.waitFor();

            // 等待日志线程结束
            inputThread.join();
            errThread.join();

            return exitValue;
        } catch (InterruptedException e) {
            terminateProcess(process);
            Thread.currentThread().interrupt();
            return -1;
        } catch (Exception e) {
            log.error("脚本执行异常：{}", e.getMessage());
            return -1;
        } finally {
            if (process != null && StrUtil.isNotBlank(param.getNodeLogId())) {
                RUN_NODE_PROCESS_MAP.remove(param.getNodeLogId(), process);
            }
            if (inputThread != null && inputThread.isAlive()) {
                inputThread.interrupt();
            }
            if (errThread != null && errThread.isAlive()) {
                errThread.interrupt();
            }
        }
    }

    /**
     * 先请求脚本正常退出；任务线程被中断后会继续等待并在超时后强制结束进程。
     */
    public static void stopRunNodeProcess(String nodeLogId) {
        if (StrUtil.isBlank(nodeLogId)) {
            return;
        }
        Process process = RUN_NODE_PROCESS_MAP.get(nodeLogId);
        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }

    /**
     * 正常结束失败后强制销毁进程，并保留当前线程的中断状态。
     */
    private static void terminateProcess(Process process) {
        if (process == null || !process.isAlive()) {
            return;
        }
        process.destroy();
        try {
            if (!process.waitFor(PROCESS_GRACEFUL_STOP_SECONDS, TimeUnit.SECONDS) && process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(PROCESS_FORCE_STOP_SECONDS, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
        }
    }

}
