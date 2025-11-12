package com.nbatch.job.admin.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.controller.interceptor.PermissionInterceptor;
import com.nbatch.job.admin.core.domain.param.JobInfoParam;
import com.nbatch.job.admin.core.domain.po.JobGroupPo;
import com.nbatch.job.admin.core.domain.po.JobInfoPo;
import com.nbatch.job.admin.core.domain.po.JobUserPo;
import com.nbatch.job.admin.core.exception.XxlJobException;
import com.nbatch.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.nbatch.job.admin.core.scheduler.MisfireStrategyEnum;
import com.nbatch.job.admin.core.scheduler.ScheduleTypeEnum;
import com.nbatch.job.admin.core.thread.JobScheduleHelper;
import com.nbatch.job.admin.core.util.I18nUtil;
import com.nbatch.job.admin.mapper.IJobGroupMapper;
import com.nbatch.job.admin.service.JobService;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.enums.ExecutorBlockStrategyEnum;
import com.nbatch.job.core.glue.GlueTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * index controller
 *
 * @author Mr.ni
 */
@Slf4j
@Controller
@RequestMapping("/jobinfo")
public class JobInfoController {

    @Resource
    private IJobGroupMapper jobGroupMapper;
    @Resource
    private JobService xxlJobService;

    @RequestMapping
    public String index(HttpServletRequest request, Model model, @RequestParam(required = false, defaultValue = "-1") String jobGroup) {

        // 枚举-字典路由策略-列表
        model.addAttribute("ExecutorRouteStrategyEnum", ExecutorRouteStrategyEnum.values());
        // Glue类型-字典
        model.addAttribute("GlueTypeEnum", GlueTypeEnum.values());
        // 阻塞处理策略-字典
        model.addAttribute("ExecutorBlockStrategyEnum", ExecutorBlockStrategyEnum.values());
        // 调度类型
        model.addAttribute("ScheduleTypeEnum", ScheduleTypeEnum.values());
        // 调度过期策略
        model.addAttribute("MisfireStrategyEnum", MisfireStrategyEnum.values());

        List<JobGroupPo> jobGroupListAll = jobGroupMapper.selectList(Wrappers.lambdaQuery(JobGroupPo.class)
                .orderByDesc(JobGroupPo::getAppName).orderByDesc(JobGroupPo::getTitle)
                .orderByAsc(JobGroupPo::getId));

        // filter group
        List<JobGroupPo> jobGroupList = PermissionInterceptor.filterJobGroupByRole(request, jobGroupListAll);
        if (CollUtil.isEmpty(jobGroupList)) {
            throw new XxlJobException(I18nUtil.getString("jobgroup_empty"));
        }

        model.addAttribute("JobGroupList", jobGroupList);
        model.addAttribute("jobGroup", jobGroup);

        return "jobinfo/jobinfo.index";
    }

    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        String jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author) {

        return xxlJobService.pageList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
    }

    @RequestMapping("/add")
    @ResponseBody
    public ReturnT<String> add(HttpServletRequest request, JobInfoParam jobInfoParam) {
        // valid permission
        PermissionInterceptor.validJobGroupPermission(request, jobInfoParam.getJobGroup());

        // opt
        JobUserPo loginUser = PermissionInterceptor.getLoginUser(request);
        return xxlJobService.add(jobInfoParam, loginUser);
    }

    @RequestMapping("/update")
    @ResponseBody
    public ReturnT<String> update(HttpServletRequest request, JobInfoParam jobInfo) {
        // valid permission
        PermissionInterceptor.validJobGroupPermission(request, jobInfo.getJobGroup());

        // opt
        JobUserPo loginUser = PermissionInterceptor.getLoginUser(request);
        return xxlJobService.update(jobInfo, loginUser);
    }

    @RequestMapping("/remove")
    @ResponseBody
    public ReturnT<String> remove(int id) {
        return xxlJobService.remove(id);
    }

    @RequestMapping("/stop")
    @ResponseBody
    public ReturnT<String> pause(int id) {
        return xxlJobService.stop(id);
    }

    @RequestMapping("/start")
    @ResponseBody
    public ReturnT<String> start(int id) {
        return xxlJobService.start(id);
    }

    @RequestMapping("/trigger")
    @ResponseBody
    public ReturnT<String> triggerJob(HttpServletRequest request, String id, String executorParam, String addressList) {
        // login user
        JobUserPo loginUser = PermissionInterceptor.getLoginUser(request);
        // trigger
        return xxlJobService.trigger(loginUser, id, executorParam, addressList);
    }

    @RequestMapping("/nextTriggerTime")
    @ResponseBody
    public ReturnT<List<String>> nextTriggerTime(String scheduleType, String scheduleConf) {

        JobInfoPo jobInfo = new JobInfoPo();
        jobInfo.setScheduleType(scheduleType);
        jobInfo.setScheduleConf(scheduleConf);

        List<String> result = new ArrayList<>();
        try {
            Date lastTime = new Date();
            for (int i = 0; i < 5; i++) {
                lastTime = JobScheduleHelper.generateNextValidTime(jobInfo, lastTime);
                if (lastTime != null) {
                    result.add(DateUtil.formatDateTime(lastTime));
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("nextTriggerTime error. scheduleType = {}, scheduleConf= {}", scheduleType, scheduleConf, e);
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")) + e.getMessage());
        }
        return new ReturnT<>(result);

    }

}
