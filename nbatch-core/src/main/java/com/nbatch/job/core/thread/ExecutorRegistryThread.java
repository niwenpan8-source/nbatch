package com.nbatch.job.core.thread;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.AdminBiz;
import com.nbatch.job.core.biz.model.RegistryParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.RegistryConfig;
import com.nbatch.job.core.executor.BatchJobExecutor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * 心跳线程，定期发送执行器注册信息。
 * @author Mr.ni
 * @date 2025/11/05
 */
@Slf4j
public class ExecutorRegistryThread {

    @Getter
    private static final ExecutorRegistryThread INSTANCE = new ExecutorRegistryThread();

    public static ExecutorRegistryThread getInstance() {
        return INSTANCE;
    }

    private Thread registryThread;
    private volatile boolean toStop = false;
    public void start(final String appName, final String address){

        // 校验配置
        if (StrUtil.isBlank(appName)) {
            log.warn(">>>>>>>>>>> 作业执行器注册配置失败，appName 为空。");
            return;
        }
        if (BatchJobExecutor.getAdminBizList() == null) {
            log.warn(">>>>>>>>>>> 作业执行器注册配置失败，管理端地址为空。");
            return;
        }

        registryThread = new Thread(() -> {
            // 注册执行器
            while (!toStop) {
                try {
                    RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appName, address);
                    for (AdminBiz adminBiz: BatchJobExecutor.getAdminBizList()) {
                        try {
                            ReturnT<String> registryResult = adminBiz.registry(registryParam);
                            if (registryResult!=null && HandleCodeConstant.HANDLE_CODE_SUCCESS == registryResult.getCode()) {
                                registryResult = ReturnT.SUCCESS;
                                log.debug(">>>>>>>>>>> 作业注册成功，注册参数：{}，注册结果：{}", registryParam, registryResult);
                                break;
                            } else {
                                log.info(">>>>>>>>>>> 作业注册失败，注册参数：{}，注册结果：{}", registryParam, registryResult);
                            }
                        } catch (Throwable e) {
                            log.info(">>>>>>>>>>> 作业注册异常，注册参数：{}", registryParam, e);
                        }

                    }
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                    }

                }

                try {
                    if (!toStop) {
                        TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                    }
                } catch (Throwable e) {
                    if (!toStop) {
                        log.warn(">>>>>>>>>>> 作业执行器注册线程被中断，错误信息：{}", ExceptionUtil.getRootCauseMessage(e));
                    }
                }
            }

            // 注销执行器
            try {
                RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appName, address);
                for (AdminBiz adminBiz: BatchJobExecutor.getAdminBizList()) {
                    try {
                        ReturnT<String> registryResult = adminBiz.registryRemove(registryParam);
                        if (registryResult!=null && HandleCodeConstant.HANDLE_CODE_SUCCESS == registryResult.getCode()) {
                            registryResult = ReturnT.SUCCESS;
                            log.info(">>>>>>>>>>> 作业注销成功，注册参数：{}，注销结果：{}", registryParam, registryResult);
                            break;
                        } else {
                            log.info(">>>>>>>>>>> 作业注销失败，注册参数：{}，注销结果：{}", registryParam, registryResult);
                        }
                    } catch (Throwable e) {
                        if (!toStop) {
                            log.info(">>>>>>>>>>> 作业注销异常，注册参数：{}", registryParam, e);
                        }

                    }

                }
            } catch (Throwable e) {
                if (!toStop) {
                    log.error(e.getMessage(), e);
                }
            }
            log.info(">>>>>>>>>>> 作业执行器注册线程已销毁。");

        });
        registryThread.setDaemon(true);
        registryThread.setName("job, executor ExecutorRegistryThread");
        registryThread.start();
    }

    public void toStop() {
        toStop = true;

        // 中断线程并等待结束
        if (registryThread != null) {
            registryThread.interrupt();
            try {
                registryThread.join();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }

    }

}
