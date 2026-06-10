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
 * beat thread, send registry info.
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

        // valid
        if (StrUtil.isBlank(appName)) {
            log.warn(">>>>>>>>>>> job, executor registry config fail, appName is null.");
            return;
        }
        if (BatchJobExecutor.getAdminBizList() == null) {
            log.warn(">>>>>>>>>>> job, executor registry config fail, adminAddresses is null.");
            return;
        }

        registryThread = new Thread(() -> {
            // registry
            while (!toStop) {
                try {
                    RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appName, address);
                    for (AdminBiz adminBiz: BatchJobExecutor.getAdminBizList()) {
                        try {
                            ReturnT<String> registryResult = adminBiz.registry(registryParam);
                            if (registryResult!=null && HandleCodeConstant.HANDLE_CODE_SUCCESS == registryResult.getCode()) {
                                registryResult = ReturnT.SUCCESS;
                                log.debug(">>>>>>>>>>> job registry success, registryParam:{}, registryResult:{}", registryParam, registryResult);
                                break;
                            } else {
                                log.info(">>>>>>>>>>> job registry fail, registryParam:{}, registryResult:{}", registryParam, registryResult);
                            }
                        } catch (Throwable e) {
                            log.info(">>>>>>>>>>> job registry error, registryParam:{}", registryParam, e);
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
                        log.warn(">>>>>>>>>>> job, executor registry thread interrupted, error msg:{}", ExceptionUtil.getRootCauseMessage(e));
                    }
                }
            }

            // registry remove
            try {
                RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(), appName, address);
                for (AdminBiz adminBiz: BatchJobExecutor.getAdminBizList()) {
                    try {
                        ReturnT<String> registryResult = adminBiz.registryRemove(registryParam);
                        if (registryResult!=null && HandleCodeConstant.HANDLE_CODE_SUCCESS == registryResult.getCode()) {
                            registryResult = ReturnT.SUCCESS;
                            log.info(">>>>>>>>>>> job registry-remove success, registryParam:{}, registryResult:{}", registryParam, registryResult);
                            break;
                        } else {
                            log.info(">>>>>>>>>>> job registry-remove fail, registryParam:{}, registryResult:{}", registryParam, registryResult);
                        }
                    } catch (Throwable e) {
                        if (!toStop) {
                            log.info(">>>>>>>>>>> job registry-remove error, registryParam:{}", registryParam, e);
                        }

                    }

                }
            } catch (Throwable e) {
                if (!toStop) {
                    log.error(e.getMessage(), e);
                }
            }
            log.info(">>>>>>>>>>> job, executor registry thread destroy.");

        });
        registryThread.setDaemon(true);
        registryThread.setName("job, executor ExecutorRegistryThread");
        registryThread.start();
    }

    public void toStop() {
        toStop = true;

        // interrupt and wait
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
