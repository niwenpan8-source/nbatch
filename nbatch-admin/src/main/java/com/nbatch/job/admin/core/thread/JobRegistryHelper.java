package com.nbatch.job.admin.core.thread;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrPool;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.conf.JobAdminConfig;
import com.nbatch.job.admin.core.domain.po.JobGroupPo;
import com.nbatch.job.admin.core.domain.po.JobRegistryPo;
import com.nbatch.job.core.biz.model.RegistryParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.RegistryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 作业注册管理线程
 */
@Slf4j
public class JobRegistryHelper {

    private static final JobRegistryHelper INSTANCE = new JobRegistryHelper();

    public static JobRegistryHelper getInstance() {
        return INSTANCE;
    }

    private ThreadPoolExecutor registryOrRemoveThreadPool = null;
    private Thread registryMonitorThread;
    private volatile boolean toStop = false;

    public void start() {

        // 处理注册和注销
        registryOrRemoveThreadPool = new ThreadPoolExecutor(
                2,
                10,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                r -> new Thread(r, "job, admin JobRegistryMonitorHelper-registryOrRemoveThreadPool-" + r.hashCode()),
                (r, executor) -> {
                    r.run();
                    log.warn(">>>>>>>>>>> 作业注册或注销请求过快，线程池已满，立即执行当前请求。");
                });

        // 处理注册监控
        registryMonitorThread = new Thread(() -> {
            while (!toStop) {
                try {
                    JobAdminConfig.getAdminConfig().getRegistryAddressHelper().refreshGroupRegistryAddress();
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(">>>>>>>>>>> 作业注册监控线程发生异常：", e);
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(">>>>>>>>>>> 作业注册监控线程发生异常：", e);
                    }
                }
            }
            log.info(">>>>>>>>>>> 作业注册监控线程已停止。");
        });
        registryMonitorThread.setDaemon(true);
        registryMonitorThread.setName("job, admin JobRegistryMonitorHelper-registryMonitorThread");
        registryMonitorThread.start();
    }

    public void toStop() {
        toStop = true;

        // 停止注册和注销线程池
        registryOrRemoveThreadPool.shutdownNow();

        // 停止监控线程并等待结束
        registryMonitorThread.interrupt();
        try {
            registryMonitorThread.join();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }


    // ---------------------- helper ----------------------

    public ReturnT<String> registry(RegistryParam registryParam) {

        // 校验注册参数
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
                || !StringUtils.hasText(registryParam.getRegistryKey())
                || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "注册参数不合法。");
        }

        // 异步执行注册
        registryOrRemoveThreadPool.execute(() -> {
            JobAdminConfig.getAdminConfig().getRegistryAddressHelper().executeRegistry(registryParam);
        });

        return ReturnT.SUCCESS;
    }

    public ReturnT<String> registryRemove(RegistryParam registryParam) {

        // 校验注册参数
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
                || !StringUtils.hasText(registryParam.getRegistryKey())
                || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "注册参数不合法。");
        }

        // 异步执行注册操作
        registryOrRemoveThreadPool.execute(() -> {
            JobAdminConfig.getAdminConfig().getRegistryAddressHelper().executeRemoveRegistry(registryParam);
        });

        return ReturnT.SUCCESS;
    }

}
