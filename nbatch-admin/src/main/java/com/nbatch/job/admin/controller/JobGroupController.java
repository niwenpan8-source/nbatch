package com.nbatch.job.admin.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.controller.annotation.PermissionLimit;
import com.nbatch.job.admin.core.domain.param.JobGroupParam;
import com.nbatch.job.admin.core.domain.po.JobGroupPo;
import com.nbatch.job.admin.core.domain.po.JobInfoPo;
import com.nbatch.job.admin.core.domain.po.JobRegistryPo;
import com.nbatch.job.admin.core.util.I18nUtil;
import com.nbatch.job.admin.mapper.IJobGroupMapper;
import com.nbatch.job.admin.mapper.IJobInfoMapper;
import com.nbatch.job.admin.mapper.IJobRegistryMapper;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.RegistryConfig;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * job group controller
 *
 * @author Mr.ni
 */
@Controller
@RequestMapping("/jobgroup")
public class JobGroupController {

    @Resource
    public IJobInfoMapper jobInfoMapper;
    @Resource
    public IJobGroupMapper jobGroupMapper;
    @Resource
    private IJobRegistryMapper jobRegistryMapper;

    @RequestMapping
    @PermissionLimit(adminuser = true)
    public String index() {
        return "jobgroup/jobgroup.index";
    }

    @RequestMapping("/pageList")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        String appName,
                                        String title) {

        LambdaQueryWrapper<JobGroupPo> jobGroupQuery = Wrappers.lambdaQuery(JobGroupPo.class)
                .eq(StrUtil.isNotEmpty(appName), JobGroupPo::getAppName, appName)
                .eq(StrUtil.isNotEmpty(title), JobGroupPo::getTitle, title);
        Page<JobGroupPo> jobGroupPage = new Page<>((start / length) + 1, length);
        Page<JobGroupPo> list = jobGroupMapper.selectPage(jobGroupPage, jobGroupQuery);

        // package result
        Map<String, Object> maps = new HashMap<>();
        // 总记录数
        maps.put("recordsTotal", list.getTotal());
        // 过滤后的总记录数
        maps.put("recordsFiltered", list.getTotal());
        // 分页列表
        maps.put("data", list.getRecords());
        return maps;
    }

    @RequestMapping("/save")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> save(JobGroupParam jobGroupParam) {
        // valid
        if (StrUtil.isBlank(jobGroupParam.getAppName())) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "AppName"));
        }
        if (jobGroupParam.getAppName().length() < 4 || jobGroupParam.getAppName().length() > 64) {
            return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_appName_length"));
        }
        if (jobGroupParam.getAppName().contains(">") || jobGroupParam.getAppName().contains("<")) {
            return new ReturnT<>(500, "AppName" + I18nUtil.getString("system_unvalid"));
        }
        if (StrUtil.isBlank(jobGroupParam.getTitle())) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title")));
        }
        if (jobGroupParam.getTitle().contains(">") || jobGroupParam.getTitle().contains("<")) {
            return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_title") + I18nUtil.getString("system_unvalid"));
        }
        if (jobGroupParam.getAddressType() != 0) {
            if (StrUtil.isBlank(jobGroupParam.getAddressList())) {
                return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_addressType_limit"));
            }
            if (jobGroupParam.getAddressList().contains(">") || jobGroupParam.getAddressList().contains("<")) {
                return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_registryList") + I18nUtil.getString("system_unvalid"));
            }

            String[] addresss = jobGroupParam.getAddressList().split(StrPool.COMMA);
            for (String item : addresss) {
                if (StrUtil.isBlank(item)) {
                    return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_registryList_unvalid"));
                }
            }
        }

        // process
        jobGroupParam.setUpdateTime(new Date());
        JobGroupPo jobGroupPo = BeanUtil.toBean(jobGroupParam, JobGroupPo.class);
        int ret = jobGroupMapper.insert(jobGroupPo);

        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @RequestMapping("/update")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> update(JobGroupParam jobGroupParam) {
        // valid
        if (StrUtil.isBlank(jobGroupParam.getAppName())) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "AppName"));
        }
        if (jobGroupParam.getAppName().length() < 4 || jobGroupParam.getAppName().length() > 64) {
            return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_appName_length"));
        }
        if (StrUtil.isBlank(jobGroupParam.getTitle())) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title")));
        }
        if (jobGroupParam.getAddressType() == 0) {
            // 0=自动注册
            List<String> registryList = findRegistryByAppName(jobGroupParam.getAppName());
            StringBuilder addressListStr = null;
            if (registryList != null && !registryList.isEmpty()) {
                Collections.sort(registryList);
                addressListStr = new StringBuilder();
                for (String item : registryList) {
                    addressListStr.append(item).append(",");
                }
                addressListStr = new StringBuilder(addressListStr.substring(0, addressListStr.length() - 1));
            }
            jobGroupParam.setAddressList(StrUtil.toString(addressListStr));
        } else {
            // 1=手动录入
            if (StrUtil.isBlank(jobGroupParam.getAddressList())) {
                return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_addressType_limit"));
            }
            String[] addresss = jobGroupParam.getAddressList().split(",");
            for (String item : addresss) {
                if (StrUtil.isBlank(item)) {
                    return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_registryList_unvalid"));
                }
            }
        }

        // process
        jobGroupParam.setUpdateTime(new Date());
        JobGroupPo jobGroupPo = BeanUtil.toBean(jobGroupParam, JobGroupPo.class);
        int ret = jobGroupMapper.updateById(jobGroupPo);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    private List<String> findRegistryByAppName(String appNameParam) {
        HashMap<String, List<String>> appAddressMap = new HashMap<>();
        DateTime currentOffsetTime = DateUtil.offsetSecond(new Date(), -RegistryConfig.DEAD_TIMEOUT);
        LambdaQueryWrapper<JobRegistryPo> registryQuery = Wrappers.lambdaQuery(JobRegistryPo.class)
                .gt(JobRegistryPo::getUpdateTime, currentOffsetTime);
        List<JobRegistryPo> list = jobRegistryMapper.selectList(registryQuery);
        if (CollUtil.isNotEmpty(list)) {
            for (JobRegistryPo item : list) {
                if (RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                    String appName = item.getRegistryKey();
                    List<String> registryList = appAddressMap.get(appName);
                    if (registryList == null) {
                        registryList = new ArrayList<>();
                    }

                    if (!registryList.contains(item.getRegistryValue())) {
                        registryList.add(item.getRegistryValue());
                    }
                    appAddressMap.put(appName, registryList);
                }
            }
        }
        return appAddressMap.get(appNameParam);
    }

    @RequestMapping("/remove")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> remove(int id) {
        // valid
        Long count = jobInfoMapper.selectCount(Wrappers.lambdaQuery(JobInfoPo.class)
                .eq(JobInfoPo::getJobGroup, id)
                .eq(JobInfoPo::getTriggerStatus, -1));
        if (count > 0) {
            return new ReturnT<>(500, I18nUtil.getString("jobgroup_del_limit_0"));
        }

        Long jobGroupCount = jobGroupMapper.selectCount(Wrappers.lambdaQuery(JobGroupPo.class));
        if (jobGroupCount == 1) {
            return new ReturnT<>(500, I18nUtil.getString("jobgroup_del_limit_1"));
        }

        int ret = jobGroupMapper.deleteById(id);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @RequestMapping("/loadById")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<JobGroupPo> loadById(int id) {
        JobGroupPo jobGroup = jobGroupMapper.selectById(id);
        return jobGroup != null ? new ReturnT<>(jobGroup) : new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, null);
    }

}
