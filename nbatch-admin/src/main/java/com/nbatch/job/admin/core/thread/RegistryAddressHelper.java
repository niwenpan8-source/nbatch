package com.nbatch.job.admin.core.thread;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobGroupPo;
import com.nbatch.job.admin.core.domain.po.JobRegistryPo;
import com.nbatch.job.admin.mapper.IJobGroupMapper;
import com.nbatch.job.admin.mapper.IJobRegistryMapper;
import com.nbatch.job.core.biz.model.RegistryParam;
import com.nbatch.job.core.enums.RegistryConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 注册中心工具类
 */
@Component
@RequiredArgsConstructor
public class RegistryAddressHelper {

    private static final int MAX_ROUTE_COUNT = 1_000_000;

    private final IJobGroupMapper jobGroupMapper;

    private final IJobRegistryMapper jobRegistryMapper;

    private final ConcurrentMap<String, AtomicInteger> groupAddressIndexMap = new ConcurrentHashMap<>();

    /**
     * 通过顺序查找从组中得到地址
     */
    public String getAddressByOrder(String jobGroup) {
        JobGroupPo group = jobGroupMapper.selectById(jobGroup);
        if (group == null || CollUtil.isEmpty(group.getRegistryList())) {
            return null;
        }
        List<String> addressList = group.getRegistryList();
        AtomicInteger index = groupAddressIndexMap.computeIfAbsent(jobGroup, key -> new AtomicInteger());
        int current = index.getAndUpdate(value -> value == MAX_ROUTE_COUNT - 1 ? 0 : value + 1);
        return addressList.get(Math.floorMod(current, addressList.size()));
    }

    /**
     * 获取当前有效的执行器地址。
     */
    public List<String> getActiveExecutorAddressList() {
        List<JobRegistryPo> registryList = jobRegistryMapper.selectList(Wrappers.lambdaQuery(JobRegistryPo.class)
                .eq(JobRegistryPo::getRegistryGroup, RegistryConfig.RegistType.EXECUTOR.name())
                .ge(JobRegistryPo::getUpdateTime, DateUtil.offsetSecond(new Date(), -RegistryConfig.DEAD_TIMEOUT)));
        if (CollUtil.isEmpty(registryList)) {
            return Collections.emptyList();
        }

        Set<String> addressSet = new LinkedHashSet<>();
        for (JobRegistryPo registryPo : registryList) {
            if (StrUtil.isBlank(registryPo.getRegistryValue())) {
                continue;
            }
            for (String address : registryPo.getRegistryValue().split(StrPool.COMMA)) {
                if (StrUtil.isNotBlank(address)) {
                    addressSet.add(address.trim());
                }
            }
        }
        return new ArrayList<>(addressSet);
    }

    /**
     * 刷新作业组注册地址
     */
    public void refreshGroupRegistryAddress() {
        // 自动注册的执行器组
        List<JobGroupPo> groupList = jobGroupMapper
                .selectList(Wrappers.lambdaQuery(JobGroupPo.class)
                        .eq(JobGroupPo::getAddressType, 0)
                        .orderByDesc(JobGroupPo::getAppName)
                        .orderByDesc(JobGroupPo::getTitle)
                        .orderByAsc(JobGroupPo::getId));
        if (groupList != null && !groupList.isEmpty()) {

            // 移除过期的注册 address
            List<JobRegistryPo> jobRegistryList = jobRegistryMapper
                    .selectList(Wrappers.lambdaQuery(JobRegistryPo.class)
                            .lt(JobRegistryPo::getUpdateTime, DateUtil.offsetSecond(DateUtil.date(), -RegistryConfig.DEAD_TIMEOUT)));
            if (CollUtil.isNotEmpty(jobRegistryList)) {
                List<String> ids = jobRegistryList.stream().map(JobRegistryPo::getId).collect(Collectors.toList());
                jobRegistryMapper.deleteByIds(ids);
            }

            // 刷新地址
            HashMap<String, List<String>> appAddressMap = new HashMap<>();

            // 得到没有过期的地址
            List<JobRegistryPo> list = jobRegistryMapper
                    .selectList(Wrappers.lambdaQuery(JobRegistryPo.class)
                            .ge(JobRegistryPo::getUpdateTime, DateUtil.offsetSecond(DateUtil.date(), -RegistryConfig.DEAD_TIMEOUT)));
            if (list != null) {
                for (JobRegistryPo item : list) {
                    if (RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                        appAddressMap
                                .computeIfAbsent(item.getRegistryKey(), k -> new ArrayList<>())
                                .add(item.getRegistryValue());
                    }
                }
            }

            // 刷新组中的地址
            for (JobGroupPo group : groupList) {
                List<String> registryList = appAddressMap.get(group.getAppName());
                String addressListStr = null;
                if (registryList != null && !registryList.isEmpty()) {
                    Collections.sort(registryList);
                    addressListStr = String.join(StrPool.COMMA, registryList);
                }
                group.setAddressList(addressListStr);
                group.setUpdateTime(new Date());
                jobGroupMapper.updateById(group);
            }
        }
    }

    /**
     * 写入或更新执行器注册信息
     */
    public void executeRegistry(RegistryParam registryParam) {
        JobRegistryPo jobRegistryPo = new JobRegistryPo()
                .setRegistryGroup(registryParam.getRegistryGroup())
                .setRegistryKey(registryParam.getRegistryKey())
                .setRegistryValue(registryParam.getRegistryValue())
                .setUpdateTime(new Date());
        LambdaQueryWrapper<JobRegistryPo> registryWrapper = Wrappers.lambdaQuery(JobRegistryPo.class)
                .eq(JobRegistryPo::getRegistryKey, registryParam.getRegistryKey())
                .eq(JobRegistryPo::getRegistryValue, registryParam.getRegistryValue())
                .eq(JobRegistryPo::getRegistryGroup, registryParam.getRegistryGroup());
        if (jobRegistryMapper.selectCount(registryWrapper) > 0) {
            jobRegistryMapper.update(jobRegistryPo, registryWrapper);
        } else {
            jobRegistryMapper.insert(jobRegistryPo);
        }
    }

    /**
     * 删除执行器注册信息
     */
    public void executeRemoveRegistry(RegistryParam registryParam) {
        jobRegistryMapper.delete(Wrappers.lambdaQuery(JobRegistryPo.class)
                .eq(JobRegistryPo::getRegistryGroup, registryParam.getRegistryGroup())
                .eq(JobRegistryPo::getRegistryKey, registryParam.getRegistryKey())
                .eq(JobRegistryPo::getRegistryValue, registryParam.getRegistryValue()));
    }

}
