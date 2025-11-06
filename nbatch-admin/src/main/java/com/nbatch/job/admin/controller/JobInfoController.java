package com.nbatch.job.admin.controller;

import cn.hutool.core.collection.CollUtil;
import com.nbatch.job.admin.controller.interceptor.PermissionInterceptor;
import com.nbatch.job.admin.core.exception.XxlJobException;
import com.nbatch.job.admin.core.model.XxlJobGroup;
import com.nbatch.job.admin.core.model.XxlJobInfo;
import com.nbatch.job.admin.core.model.XxlJobUser;
import com.nbatch.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.nbatch.job.admin.core.scheduler.MisfireStrategyEnum;
import com.nbatch.job.admin.core.scheduler.ScheduleTypeEnum;
import com.nbatch.job.admin.core.thread.JobScheduleHelper;
import com.nbatch.job.admin.core.util.I18nUtil;
import com.nbatch.job.admin.dao.XxlJobGroupDao;
import com.nbatch.job.admin.service.XxlJobService;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.enums.ExecutorBlockStrategyEnum;
import com.nbatch.job.core.glue.GlueTypeEnum;
import com.nbatch.job.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * index controller
 *
 * @author Mr.ni 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/jobinfo")
public class JobInfoController {

    private static final Logger logger = LoggerFactory.getLogger(JobInfoController.class);

    @Resource
    private XxlJobGroupDao xxlJobGroupDao;
    @Resource
    private XxlJobService xxlJobService;

    @RequestMapping
    public String index(HttpServletRequest request, Model model, @RequestParam(required = false, defaultValue = "-1") int jobGroup) {

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

        // 执行器列表
        List<XxlJobGroup> jobGroupListAll = xxlJobGroupDao.findAll();

        // filter group
        List<XxlJobGroup> jobGroupList = PermissionInterceptor.filterJobGroupByRole(request, jobGroupListAll);
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
                                        int jobGroup, int triggerStatus, String jobDesc, String executorHandler, String author) {

        return xxlJobService.pageList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
    }

    @RequestMapping("/add")
    @ResponseBody
    public ReturnT<String> add(HttpServletRequest request, XxlJobInfo jobInfo) {
        // valid permission
        PermissionInterceptor.validJobGroupPermission(request, jobInfo.getJobGroup());

        // opt
        XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
        return xxlJobService.add(jobInfo, loginUser);
    }

    @RequestMapping("/update")
    @ResponseBody
    public ReturnT<String> update(HttpServletRequest request, XxlJobInfo jobInfo) {
        // valid permission
        PermissionInterceptor.validJobGroupPermission(request, jobInfo.getJobGroup());

        // opt
        XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
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
    public ReturnT<String> triggerJob(HttpServletRequest request, int id, String executorParam, String addressList) {
        // login user
        XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
        // trigger
        return xxlJobService.trigger(loginUser, id, executorParam, addressList);
    }

    @RequestMapping("/nextTriggerTime")
    @ResponseBody
    public ReturnT<List<String>> nextTriggerTime(String scheduleType, String scheduleConf) {

        XxlJobInfo paramXxlJobInfo = new XxlJobInfo();
        paramXxlJobInfo.setScheduleType(scheduleType);
        paramXxlJobInfo.setScheduleConf(scheduleConf);

        List<String> result = new ArrayList<>();
        try {
            Date lastTime = new Date();
            for (int i = 0; i < 5; i++) {
                lastTime = JobScheduleHelper.generateNextValidTime(paramXxlJobInfo, lastTime);
                if (lastTime != null) {
                    result.add(DateUtil.formatDateTime(lastTime));
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("nextTriggerTime error. scheduleType = {}, scheduleConf= {}", scheduleType, scheduleConf, e);
            return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")) + e.getMessage());
        }
        return new ReturnT<>(result);

    }

}
