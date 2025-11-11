package com.nbatch.job.admin.core.thread;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrPool;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.conf.XxlJobAdminConfig;
import com.nbatch.job.admin.core.domain.po.JobGroupPo;
import com.nbatch.job.admin.core.domain.po.JobRegistryPo;
import com.nbatch.job.core.biz.model.RegistryParam;
import com.nbatch.job.core.biz.model.ReturnT;
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
 * job registry instance
 * @author Mr.ni 2016-10-02 19:10:24
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

        // for registry or remove
        registryOrRemoveThreadPool = new ThreadPoolExecutor(
                2,
                10,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                r -> new Thread(r, "xxl-job, admin JobRegistryMonitorHelper-registryOrRemoveThreadPool-" + r.hashCode()),
                (r, executor) -> {
                    r.run();
                    log.warn(">>>>>>>>>>> xxl-job, registry or remove too fast, match threadpool rejected handler(run now).");
                });

        // for monitor
        registryMonitorThread = new Thread(() -> {
            while (!toStop) {
                try {
                    // auto registry group
                    List<JobGroupPo> groupList = XxlJobAdminConfig.getAdminConfig().getJobGroupMapper()
                            .selectList(Wrappers.lambdaQuery(JobGroupPo.class)
                                    .eq(JobGroupPo::getAddressType, 0)
                                    .orderByDesc(JobGroupPo::getAppName)
                                    .orderByDesc(JobGroupPo::getTitle)
                                    .orderByAsc(JobGroupPo::getId));
                    if (groupList != null && !groupList.isEmpty()) {

                        // remove dead address (admin/executor)
                        List<JobRegistryPo> jobRegistryList = XxlJobAdminConfig.getAdminConfig().getJobRegistryMapper()
                                .selectList(Wrappers.lambdaQuery(JobRegistryPo.class)
                                        .lt(JobRegistryPo::getUpdateTime, DateUtil.offsetSecond(DateUtil.date(), -RegistryConfig.DEAD_TIMEOUT)));
                        if (CollUtil.isNotEmpty(jobRegistryList)) {
                            List<String> ids = jobRegistryList.stream().map(JobRegistryPo::getId).collect(Collectors.toList());
                            XxlJobAdminConfig.getAdminConfig().getJobRegistryMapper().deleteBatchIds(ids);
                        }

                        // fresh online address (admin/executor)
                        HashMap<String, List<String>> appAddressMap = new HashMap<>();

                        List<JobRegistryPo> list = XxlJobAdminConfig.getAdminConfig().getJobRegistryMapper()
                                .selectList(Wrappers.lambdaQuery(JobRegistryPo.class)
                                        .eq(JobRegistryPo::getUpdateTime, DateUtil.offsetSecond(DateUtil.date(), -RegistryConfig.DEAD_TIMEOUT)));
                        if (list != null) {
                            for (JobRegistryPo item : list) {
                                if (RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                                    String appname = item.getRegistryKey();
                                    List<String> registryList = appAddressMap.get(appname);
                                    if (registryList == null) {
                                        registryList = new ArrayList<>();
                                    }

                                    if (!registryList.contains(item.getRegistryValue())) {
                                        registryList.add(item.getRegistryValue());
                                    }
                                    appAddressMap.put(appname, registryList);
                                }
                            }
                        }

                        // fresh group address
                        for (JobGroupPo group : groupList) {
                            List<String> registryList = appAddressMap.get(group.getAppName());
                            String addressListStr = null;
                            if (registryList != null && !registryList.isEmpty()) {
                                Collections.sort(registryList);
                                StringBuilder addressList = new StringBuilder();
                                for (String item : registryList) {
                                    addressList.append(item).append(StrPool.COMMA);
                                }
                                addressListStr = addressList.toString();
                                addressListStr = addressListStr.substring(0, addressListStr.length() - 1);
                            }
                            group.setAddressList(addressListStr);
                            group.setUpdateTime(new Date());

                            XxlJobAdminConfig.getAdminConfig().getJobGroupMapper().updateById(group);
                        }
                    }
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(">>>>>>>>>>> xxl-job, job registry monitor thread error:", e);
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(">>>>>>>>>>> xxl-job, job registry monitor thread error:", e);
                    }
                }
            }
            log.info(">>>>>>>>>>> xxl-job, job registry monitor thread stop");
        });
        registryMonitorThread.setDaemon(true);
        registryMonitorThread.setName("xxl-job, admin JobRegistryMonitorHelper-registryMonitorThread");
        registryMonitorThread.start();
    }

    public void toStop() {
        toStop = true;

        // stop registryOrRemoveThreadPool
        registryOrRemoveThreadPool.shutdownNow();

        // stop monitir (interrupt and wait)
        registryMonitorThread.interrupt();
        try {
            registryMonitorThread.join();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
    }


    // ---------------------- helper ----------------------

    public ReturnT<String> registry(RegistryParam registryParam) {

        // valid
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
                || !StringUtils.hasText(registryParam.getRegistryKey())
                || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Illegal Argument.");
        }

        // async execute
        registryOrRemoveThreadPool.execute(() -> {
            // 0-fail; 1-save suc; 2-update suc;
            JobRegistryPo jobRegistryPo = new JobRegistryPo().setRegistryGroup(registryParam.getRegistryGroup())
                    .setRegistryKey(registryParam.getRegistryKey())
                    .setRegistryValue(registryParam.getRegistryValue())
                    .setUpdateTime(new Date());
            int ret = XxlJobAdminConfig.getAdminConfig().getJobRegistryMapper().insert(jobRegistryPo);
            if (ret == 1) {
                // fresh (add)
                freshGroupRegistryInfo(registryParam);
            }
        });

        return ReturnT.SUCCESS;
    }

    public ReturnT<String> registryRemove(RegistryParam registryParam) {

        // valid
        if (!StringUtils.hasText(registryParam.getRegistryGroup())
                || !StringUtils.hasText(registryParam.getRegistryKey())
                || !StringUtils.hasText(registryParam.getRegistryValue())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "Illegal Argument.");
        }

        // async execute
        registryOrRemoveThreadPool.execute(() -> {
            int ret = XxlJobAdminConfig.getAdminConfig().getJobRegistryMapper().delete(Wrappers.lambdaQuery(JobRegistryPo.class)
                    .eq(JobRegistryPo::getRegistryGroup, registryParam.getRegistryGroup())
                    .eq(JobRegistryPo::getRegistryKey, registryParam.getRegistryKey())
                    .eq(JobRegistryPo::getRegistryValue, registryParam.getRegistryValue()));
            if (ret > 0) {
                // fresh (delete)
                freshGroupRegistryInfo(registryParam);
            }
        });

        return ReturnT.SUCCESS;
    }

    private void freshGroupRegistryInfo(RegistryParam registryParam) {
        // Under consideration, prevent affecting core tables
    }


}
