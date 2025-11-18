package com.nbatch.job.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.core.cron.CronExpression;
import com.nbatch.job.admin.core.domain.param.JobInfoParam;
import com.nbatch.job.admin.core.domain.po.JobGroupPo;
import com.nbatch.job.admin.core.domain.po.JobInfoPo;
import com.nbatch.job.admin.core.domain.po.JobLogPo;
import com.nbatch.job.admin.core.domain.po.JobLogReportPo;
import com.nbatch.job.admin.core.domain.po.JobLoggluePo;
import com.nbatch.job.admin.core.domain.po.JobUserPo;
import com.nbatch.job.admin.core.enums.ExecutorRouteStrategyEnum;
import com.nbatch.job.admin.core.enums.MisfireStrategyEnum;
import com.nbatch.job.admin.core.enums.ScheduleTypeEnum;
import com.nbatch.job.admin.core.thread.JobScheduleHelper;
import com.nbatch.job.admin.core.thread.JobTriggerPoolHelper;
import com.nbatch.job.admin.core.enums.TriggerTypeEnum;
import com.nbatch.job.admin.core.util.I18nUtil;
import com.nbatch.job.admin.mapper.IJobGroupMapper;
import com.nbatch.job.admin.mapper.IJobInfoMapper;
import com.nbatch.job.admin.mapper.IJobLogMapper;
import com.nbatch.job.admin.mapper.IJobLogReportMapper;
import com.nbatch.job.admin.mapper.IJobLogglueMapper;
import com.nbatch.job.admin.service.IJobService;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.enums.ExecutorBlockStrategyEnum;
import com.nbatch.job.core.glue.GlueTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * core job action for job
 *
 * @author Mr.ni
 */
@Slf4j
@Service
public class JobServiceImpl implements IJobService {

    @Resource
    private IJobGroupMapper jobGroupMapper;
    @Resource
    private IJobInfoMapper jobInfoMapper;
    @Resource
    public IJobLogMapper jobLogMapper;
    @Resource
    private IJobLogglueMapper jobLogglueMapper;
    @Resource
    private IJobLogReportMapper jobLogReportMapper;

    @Override
    public Map<String, Object> pageList(int start, int length, String jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author) {
        Page<JobInfoPo> jobInfoPoPage = jobInfoMapper.selectPage(new Page<>(start, length), Wrappers.lambdaQuery(JobInfoPo.class)
                .eq(StrUtil.isNotEmpty(jobGroup), JobInfoPo::getJobGroup, jobGroup)
                .eq(triggerStatus != -1, JobInfoPo::getTriggerStatus, triggerStatus)
                .eq(StrUtil.isNotEmpty(jobDesc), JobInfoPo::getJobDesc, jobDesc)
                .eq(StrUtil.isNotEmpty(executorHandler), JobInfoPo::getExecutorHandler, executorHandler)
                .eq(StrUtil.isNotEmpty(author), JobInfoPo::getAuthor, author)
                .orderByDesc(JobInfoPo::getId));
        // package result
        Map<String, Object> maps = new HashMap<>();
        // 总记录数
        maps.put("recordsTotal", jobInfoPoPage.getTotal());
        // 过滤后的总记录数
        maps.put("recordsFiltered", jobInfoPoPage.getTotal());
        // 分页列表
        maps.put("data", jobInfoPoPage.getRecords());
        return maps;
    }

    @Override
    public ReturnT<String> add(JobInfoParam jobInfo, JobUserPo loginUser) {

        // valid base
        JobGroupPo group = jobGroupMapper.selectOne(Wrappers.lambdaQuery(JobGroupPo.class)
                .eq(JobGroupPo::getId, jobInfo.getJobGroup()));
        if (group == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_choose") + I18nUtil.getString("jobinfo_field_jobgroup")));
        }
        if (StrUtil.isBlank(jobInfo.getJobDesc())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_jobdesc")));
        }
        if (StrUtil.isBlank(jobInfo.getAuthor())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_author")));
        }

        // valid trigger
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
        if (scheduleTypeEnum == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
        }
        if (scheduleTypeEnum == ScheduleTypeEnum.CRON) {
            if (jobInfo.getScheduleConf() == null || !CronExpression.isValidExpression(jobInfo.getScheduleConf())) {
                return new ReturnT<>(ReturnT.FAIL_CODE, "Cron" + I18nUtil.getString("system_unvalid"));
            }
        } else if (scheduleTypeEnum == ScheduleTypeEnum.FIX_RATE) {
            if (jobInfo.getScheduleConf() == null) {
                return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")));
            }
            try {
                int fixSecond = Integer.parseInt(jobInfo.getScheduleConf());
                if (fixSecond < 1) {
                    return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
                }
            } catch (Exception e) {
                return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
            }
        }

        // valid job
        if (GlueTypeEnum.match(jobInfo.getGlueType()) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_gluetype") + I18nUtil.getString("system_unvalid")));
        }
        if (GlueTypeEnum.BEAN == GlueTypeEnum.match(jobInfo.getGlueType()) && (StrUtil.isBlank(jobInfo.getExecutorHandler()))) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + "JobHandler"));
        }
        // 》fix "\r" in shell
        if (GlueTypeEnum.GLUE_SHELL == GlueTypeEnum.match(jobInfo.getGlueType()) && jobInfo.getGlueSource() != null) {
            jobInfo.setGlueSource(jobInfo.getGlueSource().replaceAll("\r", ""));
        }

        // valid advanced
        if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorRouteStrategy") + I18nUtil.getString("system_unvalid")));
        }
        if (MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), null) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("misfire_strategy") + I18nUtil.getString("system_unvalid")));
        }
        if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorBlockStrategy") + I18nUtil.getString("system_unvalid")));
        }

        // 》ChildJobId valid
        if (StrUtil.isNotBlank(jobInfo.getChildJobid())) {
            String[] childJobIds = jobInfo.getChildJobid().split(StrPool.COMMA);
            for (String childJobIdItem : childJobIds) {
                if (StrUtil.isNotBlank(childJobIdItem) && isNumeric(childJobIdItem)) {
                    JobInfoPo childJobInfo = jobInfoMapper.selectById(childJobIdItem);
                    if (childJobInfo == null) {
                        return new ReturnT<>(ReturnT.FAIL_CODE,
                                MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_not_found")), childJobIdItem));
                    }
                    if (!loginUser.validPermission(childJobInfo.getJobGroup())) {
                        return new ReturnT<>(ReturnT.FAIL_CODE,
                                MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_permission_limit")), childJobIdItem));
                    }
                } else {
                    return new ReturnT<>(ReturnT.FAIL_CODE,
                            MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_unvalid")), childJobIdItem));
                }
            }

            // join , avoid "xxx,,"
            StringBuilder temp = new StringBuilder();
            for (String item : childJobIds) {
                temp.append(item).append(",");
            }
            temp = new StringBuilder(temp.substring(0, temp.length() - 1));

            jobInfo.setChildJobid(temp.toString());
        }

        // add in db
        jobInfo.setId(IdUtil.getSnowflakeNextIdStr());
        jobInfo.setAddTime(new Date());
        jobInfo.setUpdateTime(new Date());
        jobInfo.setGlueUpdatetime(new Date());
        jobInfoMapper.insert(BeanUtil.toBean(jobInfo, JobInfoPo.class));
        if (StrUtil.isBlank(jobInfo.getId())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_add") + I18nUtil.getString("system_fail")));
        }

        return new ReturnT<>(String.valueOf(jobInfo.getId()));
    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public ReturnT<String> update(JobInfoParam jobInfo, JobUserPo loginUser) {

        // valid base
        if (StrUtil.isBlank(jobInfo.getJobDesc())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_jobdesc")));
        }
        if (StrUtil.isBlank(jobInfo.getAuthor())) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_author")));
        }

        // valid trigger
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
        if (scheduleTypeEnum == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
        }
        if (scheduleTypeEnum == ScheduleTypeEnum.CRON) {
            if (jobInfo.getScheduleConf() == null || !CronExpression.isValidExpression(jobInfo.getScheduleConf())) {
                return new ReturnT<>(ReturnT.FAIL_CODE, "Cron" + I18nUtil.getString("system_unvalid"));
            }
        } else if (scheduleTypeEnum == ScheduleTypeEnum.FIX_RATE) {
            if (jobInfo.getScheduleConf() == null) {
                return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
            }
            try {
                int fixSecond = Integer.parseInt(jobInfo.getScheduleConf());
                if (fixSecond < 1) {
                    return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
                }
            } catch (Exception e) {
                return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
            }
        }

        // valid advanced
        if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorRouteStrategy") + I18nUtil.getString("system_unvalid")));
        }
        if (MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), null) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("misfire_strategy") + I18nUtil.getString("system_unvalid")));
        }
        if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_executorBlockStrategy") + I18nUtil.getString("system_unvalid")));
        }

        // 》ChildJobId valid
        if (StrUtil.isNotBlank(jobInfo.getChildJobid())) {
            String[] childJobIds = jobInfo.getChildJobid().split(StrPool.COMMA);
            for (String childJobIdItem : childJobIds) {
                if (StrUtil.isNotBlank(childJobIdItem) && isNumeric(childJobIdItem)) {
                    if (StrUtil.equals(childJobIdItem, jobInfo.getId())) {
                        return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_childJobId") + "(" + childJobIdItem + ")" + I18nUtil.getString("system_unvalid")));
                    }

                    // valid child
                    JobInfoPo childJobInfo = jobInfoMapper.selectById(childJobIdItem);
                    if (childJobInfo == null) {
                        return new ReturnT<>(ReturnT.FAIL_CODE,
                                MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_not_found")), childJobIdItem));
                    }
                    if (!loginUser.validPermission(childJobInfo.getJobGroup())) {
                        return new ReturnT<>(ReturnT.FAIL_CODE,
                                MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_permission_limit")), childJobIdItem));
                    }
                } else {
                    return new ReturnT<>(ReturnT.FAIL_CODE,
                            MessageFormat.format((I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil.getString("system_unvalid")), childJobIdItem));
                }
            }

            // join , avoid "xxx,,"
            StringBuilder temp = new StringBuilder();
            for (String item : childJobIds) {
                temp.append(item).append(StrPool.COMMA);
            }
            temp = new StringBuilder(temp.substring(0, temp.length() - 1));

            jobInfo.setChildJobid(temp.toString());
        }

        // group valid
        JobGroupPo jobGroup = jobGroupMapper.selectById(jobInfo.getJobGroup());
        if (jobGroup == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_jobgroup") + I18nUtil.getString("system_unvalid")));
        }

        // stage job info
        JobInfoPo existsJobInfo = jobInfoMapper.selectById(jobInfo.getId());

        if (existsJobInfo == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_not_found")));
        }

        // next trigger time (5s后生效，避开预读周期)
        long nextTriggerTime = existsJobInfo.getTriggerNextTime();
        boolean scheduleDataNotChanged = jobInfo.getScheduleType().equals(existsJobInfo.getScheduleType()) && jobInfo.getScheduleConf().equals(existsJobInfo.getScheduleConf());
        if (existsJobInfo.getTriggerStatus() == 1 && !scheduleDataNotChanged) {
            try {
                Date nextValidTime = JobScheduleHelper.generateNextValidTime(BeanUtil.toBean(jobInfo, JobInfoPo.class),
                        new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
                if (nextValidTime == null) {
                    return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
                }
                nextTriggerTime = nextValidTime.getTime();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
            }
        }

        existsJobInfo.setJobGroup(jobInfo.getJobGroup());
        existsJobInfo.setJobDesc(jobInfo.getJobDesc());
        existsJobInfo.setAuthor(jobInfo.getAuthor());
        existsJobInfo.setAlarmEmail(jobInfo.getAlarmEmail());
        existsJobInfo.setScheduleType(jobInfo.getScheduleType());
        existsJobInfo.setScheduleConf(jobInfo.getScheduleConf());
        existsJobInfo.setMisfireStrategy(jobInfo.getMisfireStrategy());
        existsJobInfo.setExecutorRouteStrategy(jobInfo.getExecutorRouteStrategy());
        existsJobInfo.setExecutorHandler(jobInfo.getExecutorHandler());
        existsJobInfo.setExecutorParam(jobInfo.getExecutorParam());
        existsJobInfo.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        existsJobInfo.setExecutorTimeout(jobInfo.getExecutorTimeout());
        existsJobInfo.setExecutorFailRetryCount(jobInfo.getExecutorFailRetryCount());
        existsJobInfo.setChildJobid(jobInfo.getChildJobid());
        existsJobInfo.setTriggerNextTime(nextTriggerTime);

        existsJobInfo.setUpdateTime(new Date());
        jobInfoMapper.updateById(existsJobInfo);


        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> remove(String id) {
        JobInfoPo jobInfoPo = jobInfoMapper.selectById(id);
        if (jobInfoPo == null) {
            return ReturnT.SUCCESS;
        }
        jobInfoMapper.deleteById(id);
        jobLogMapper.delete(Wrappers.lambdaQuery(JobLogPo.class).eq(JobLogPo::getJobId, id));
        jobLogglueMapper.delete(Wrappers.lambdaQuery(JobLoggluePo.class).eq(JobLoggluePo::getJobId, id));
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> start(String id) {
        JobInfoPo jobInfoPo = jobInfoMapper.selectById(id);

        // valid
        ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfoPo.getScheduleType(), ScheduleTypeEnum.NONE);
        if (ScheduleTypeEnum.NONE == scheduleTypeEnum) {
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type_none_limit_start")));
        }

        // next trigger time (5s后生效，避开预读周期)
        long nextTriggerTime;
        try {
            Date nextValidTime = JobScheduleHelper.generateNextValidTime(jobInfoPo
                    , new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
            if (nextValidTime == null) {
                return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
            }
            nextTriggerTime = nextValidTime.getTime();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
        }

        jobInfoPo.setTriggerStatus(1);
        jobInfoPo.setTriggerLastTime(0L);
        jobInfoPo.setTriggerNextTime(nextTriggerTime);

        jobInfoPo.setUpdateTime(new Date());
        jobInfoMapper.updateById(jobInfoPo);
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> stop(String id) {
        JobInfoPo jobInfoPo = jobInfoMapper.selectById(id);

        jobInfoPo.setTriggerStatus(0);
        jobInfoPo.setTriggerLastTime(0L);
        jobInfoPo.setTriggerNextTime(0L);

        jobInfoPo.setUpdateTime(new Date());
        jobInfoMapper.updateById(jobInfoPo);
        return ReturnT.SUCCESS;
    }


    @Override
    public ReturnT<String> trigger(JobUserPo loginUser, String jobId, String executorParam, String addressList) {
        // permission
        if (loginUser == null) {
            return new ReturnT<>(ReturnT.FAIL.getCode(), I18nUtil.getString("system_permission_limit"));
        }
        JobInfoPo jobInfoPo = jobInfoMapper.selectById(jobId);
        if (jobInfoPo == null) {
            return new ReturnT<>(ReturnT.FAIL.getCode(), I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        if (!hasPermission(loginUser, jobInfoPo.getJobGroup())) {
            return new ReturnT<>(ReturnT.FAIL.getCode(), I18nUtil.getString("system_permission_limit"));
        }

        // force cover job param
        if (executorParam == null) {
            executorParam = "";
        }

        JobTriggerPoolHelper.trigger(jobId, TriggerTypeEnum.MANUAL, -1, null, executorParam, addressList);
        return ReturnT.SUCCESS;
    }

    private boolean hasPermission(JobUserPo loginUser, String jobGroup) {
        if (loginUser.getRole() == 1) {
            return true;
        }
        List<String> groupIdStrs = new ArrayList<>();
        if (StrUtil.isNotBlank(loginUser.getPermission())) {
            groupIdStrs = Arrays.asList(loginUser.getPermission().trim().split(StrPool.COMMA));
        }
        return groupIdStrs.contains(String.valueOf(jobGroup));
    }

    @Override
    public Map<String, Object> dashboardInfo() {

        Integer jobInfoCount = jobInfoMapper.selectCount(Wrappers.lambdaQuery(JobInfoPo.class));
        int jobLogCount = 0;
        int jobLogSuccessCount = 0;
        LambdaQueryWrapper<JobLogReportPo> lambdaQueryWrapper = Wrappers.lambdaQuery(JobLogReportPo.class);
        List<JobLogReportPo> jobLogReportList = jobLogReportMapper.selectList(lambdaQueryWrapper);
        JobLogReportPo jobLogReportPo = null;
        if (CollUtil.isNotEmpty(jobLogReportList)) {
            jobLogReportPo = new JobLogReportPo();
            jobLogReportPo.setRunningCount(jobLogReportList.stream().mapToInt(JobLogReportPo::getRunningCount).sum());
            jobLogReportPo.setSucCount(jobLogReportList.stream().mapToInt(JobLogReportPo::getSucCount).sum());
            jobLogReportPo.setFailCount(jobLogReportList.stream().mapToInt(JobLogReportPo::getFailCount).sum());
        }
        if (jobLogReportPo != null) {
            jobLogCount = jobLogReportPo.getRunningCount() + jobLogReportPo.getSucCount() + jobLogReportPo.getFailCount();
            jobLogSuccessCount = jobLogReportPo.getSucCount();
        }

        // executor count
        Set<String> executorAddressSet = new HashSet<>();
        // 执行器列表
        List<JobGroupPo> groupList = jobGroupMapper.selectList(Wrappers.lambdaQuery(JobGroupPo.class)
                .orderByDesc(JobGroupPo::getAppName).orderByDesc(JobGroupPo::getTitle)
                .orderByAsc(JobGroupPo::getId));
        if (groupList != null && !groupList.isEmpty()) {
            for (JobGroupPo group : groupList) {
                if (group.getRegistryList() != null && !group.getRegistryList().isEmpty()) {
                    executorAddressSet.addAll(group.getRegistryList());
                }
            }
        }

        int executorCount = executorAddressSet.size();

        Map<String, Object> dashboardMap = new HashMap<>();
        dashboardMap.put("jobInfoCount", jobInfoCount);
        dashboardMap.put("jobLogCount", jobLogCount);
        dashboardMap.put("jobLogSuccessCount", jobLogSuccessCount);
        dashboardMap.put("executorCount", executorCount);
        return dashboardMap;
    }

    @Override
    public ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate) {

        // process
        List<String> triggerDayList = new ArrayList<>();
        List<Integer> triggerDayCountRunningList = new ArrayList<>();
        List<Integer> triggerDayCountSucList = new ArrayList<>();
        List<Integer> triggerDayCountFailList = new ArrayList<>();
        int triggerCountRunningTotal = 0;
        int triggerCountSucTotal = 0;
        int triggerCountFailTotal = 0;

        List<JobLogReportPo> logReportList = jobLogReportMapper.selectList(Wrappers.lambdaQuery(JobLogReportPo.class)
                .between(JobLogReportPo::getTriggerDay, startDate, endDate)
                .orderByAsc(JobLogReportPo::getTriggerDay));
        if (CollUtil.isNotEmpty(logReportList)) {
            for (JobLogReportPo item : logReportList) {
                String day = DateUtil.formatDate(item.getTriggerDay());
                int triggerDayCountRunning = item.getRunningCount();
                int triggerDayCountSuc = item.getSucCount();
                int triggerDayCountFail = item.getFailCount();

                triggerDayList.add(day);
                triggerDayCountRunningList.add(triggerDayCountRunning);
                triggerDayCountSucList.add(triggerDayCountSuc);
                triggerDayCountFailList.add(triggerDayCountFail);

                triggerCountRunningTotal += triggerDayCountRunning;
                triggerCountSucTotal += triggerDayCountSuc;
                triggerCountFailTotal += triggerDayCountFail;
            }
        } else {
            for (int i = -6; i <= 0; i++) {
                triggerDayList.add(DateUtil.formatDate(DateUtil.offsetDay(new Date(), i)));
                triggerDayCountRunningList.add(0);
                triggerDayCountSucList.add(0);
                triggerDayCountFailList.add(0);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("triggerDayList", triggerDayList);
        result.put("triggerDayCountRunningList", triggerDayCountRunningList);
        result.put("triggerDayCountSucList", triggerDayCountSucList);
        result.put("triggerDayCountFailList", triggerDayCountFailList);

        result.put("triggerCountRunningTotal", triggerCountRunningTotal);
        result.put("triggerCountSucTotal", triggerCountSucTotal);
        result.put("triggerCountFailTotal", triggerCountFailTotal);

        return new ReturnT<>(result);
    }

}
