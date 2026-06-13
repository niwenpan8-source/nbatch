package com.nbatch.job.admin.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.controller.interceptor.PermissionInterceptor;
import com.nbatch.job.admin.core.complete.JobCompleter;
import com.nbatch.job.admin.core.domain.po.JobGroupPo;
import com.nbatch.job.admin.core.domain.po.JobInfoPo;
import com.nbatch.job.admin.core.domain.po.JobLogPo;
import com.nbatch.job.admin.core.exception.JobException;
import com.nbatch.job.admin.core.executor.ExecutorBizProxy;
import com.nbatch.job.admin.core.util.I18nUtil;
import com.nbatch.job.admin.mapper.IJobGroupMapper;
import com.nbatch.job.admin.mapper.IJobInfoMapper;
import com.nbatch.job.admin.mapper.IJobLogMapper;
import com.nbatch.job.core.biz.model.KillParam;
import com.nbatch.job.core.biz.model.LogParam;
import com.nbatch.job.core.biz.model.LogResult;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * index controller
 *
 * @author Mr.ni
 */
@Slf4j
@Controller
@RequestMapping("/joblog")
public class JobLogController {

    @Resource
    private IJobGroupMapper jobGroupMapper;
    @Resource
    public IJobLogMapper jobLogMapper;
    @Resource
    public IJobInfoMapper jobInfoMapper;

    @RequestMapping
    public String index(HttpServletRequest request, Model model,
                        @RequestParam(required = false) String jobId) {

        List<JobGroupPo> jobGroupListAll = jobGroupMapper.selectList(Wrappers.lambdaQuery(JobGroupPo.class)
                .orderByDesc(JobGroupPo::getAppName).orderByDesc(JobGroupPo::getTitle)
                .orderByAsc(JobGroupPo::getId));

        // filter group
        List<JobGroupPo> jobGroupList = PermissionInterceptor.filterJobGroupByRole(request, jobGroupListAll);
        if (CollUtil.isEmpty(jobGroupList)) {
            throw new JobException(I18nUtil.getString("jobgroup_empty"));
        }

        model.addAttribute("JobGroupList", jobGroupList);

        // 任务
        if (StrUtil.isNotEmpty(jobId)) {
            JobInfoPo jobInfo = jobInfoMapper.selectById(jobId);
            if (jobInfo == null) {
                throw new RuntimeException(I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_unvalid"));
            }

            model.addAttribute("jobInfo", jobInfo);

            // valid permission
            PermissionInterceptor.validJobGroupPermission(request, jobInfo.getJobGroup());
        }

        return "joblog/joblog.index";
    }

    @RequestMapping("/getJobsByGroup")
    @ResponseBody
    public ReturnT<List<JobInfoPo>> getJobsByGroup(String jobGroup) {
        List<JobInfoPo> list = jobInfoMapper.selectList(Wrappers.lambdaQuery(JobInfoPo.class)
                .eq(JobInfoPo::getJobGroup, jobGroup));
        return new ReturnT<>(list);
    }

    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(HttpServletRequest request,
                                        @RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        String jobGroup, String jobId, int logStatus, String filterTime) {

        // valid permission
        // 仅管理员支持查询全部；普通用户仅支持查询有权限的 jobGroup
        PermissionInterceptor.validJobGroupPermission(request, jobGroup);

        // parse param
        Date triggerTimeStart = null;
        Date triggerTimeEnd = null;
        if (StrUtil.isNotBlank(filterTime)) {
            String[] temp = filterTime.split(" - ");
            if (temp.length == 2) {
                triggerTimeStart = DateUtil.parseDateTime(temp[0]);
                triggerTimeEnd = DateUtil.parseDateTime(temp[1]);
            }
        }

        Page<JobLogPo> jobLogPage = jobLogMapper.selectPage(new Page<>((start / length) + 1, length), Wrappers.lambdaQuery(JobLogPo.class)
                .eq(StrUtil.isNotEmpty(jobId) && StrUtil.isNotEmpty(jobGroup), JobLogPo::getJobGroup, jobGroup)
                .eq(StrUtil.isNotEmpty(jobId), JobLogPo::getJobId, jobId)
                .ge(triggerTimeStart != null, JobLogPo::getTriggerTime, triggerTimeStart)
                .le(triggerTimeEnd != null, JobLogPo::getTriggerTime, triggerTimeEnd)
                .eq(logStatus == 1, JobLogPo::getTriggerCode, 200)
                .and(logStatus == 2, wrapper -> wrapper.in(JobLogPo::getTriggerCode, 0, 200).or()
                        .in(JobLogPo::getHandleCode, 0, 200))
                .and(logStatus == 3, wrapper -> wrapper.eq(JobLogPo::getTriggerCode, 200).or()
                        .eq(JobLogPo::getHandleCode, 0))
        );
        // package result
        Map<String, Object> maps = new HashMap<>();
        // 总记录数
        maps.put("recordsTotal", jobLogPage.getTotal());
        // 过滤后的总记录数
        maps.put("recordsFiltered", jobLogPage.getTotal());
        // 分页列表
        maps.put("data", jobLogPage.getRecords());

        return maps;
    }

    @RequestMapping("/logDetailPage")
    public String logDetailPage(String id, Model model) {
        JobLogPo jobLog = jobLogMapper.selectById(id);
        if (jobLog == null) {
            throw new RuntimeException(I18nUtil.getString("joblog_logid_unvalid"));
        }

        model.addAttribute("triggerCode", jobLog.getTriggerCode());
        model.addAttribute("handleCode", jobLog.getHandleCode());
        model.addAttribute("logId", jobLog.getId());
        return "joblog/joblog.detail";
    }

    @RequestMapping("/logDetailCat")
    @ResponseBody
    public ReturnT<LogResult> logDetailCat(String logId, int fromLineNum) {
        try {
            // valid
            JobLogPo jobLog = jobLogMapper.selectById(logId);
            if (jobLog == null) {
                return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, I18nUtil.getString("joblog_logid_unvalid"));
            }

            // log cat
            ReturnT<LogResult> logResult = ExecutorBizProxy.log(jobLog.getExecutorAddress(),
                    new LogParam(jobLog.getTriggerTime().getTime(), logId, fromLineNum));

            // is end
            if (logResult.getContent() != null && logResult.getContent().getFromLineNum() > logResult.getContent().getToLineNum()) {
                if (jobLog.getHandleCode() > 0) {
                    logResult.getContent().setEnd(true);
                }
            }

            // fix xss
            if (logResult.getContent() != null && StringUtils.hasText(logResult.getContent().getLogContent())) {
                String newLogContent = logResult.getContent().getLogContent();
                newLogContent = HtmlUtils.htmlEscape(newLogContent, "UTF-8");
                logResult.getContent().setLogContent(newLogContent);
            }

            return logResult;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, e.getMessage());
        }
    }

    @RequestMapping("/logKill")
    @ResponseBody
    public ReturnT<String> logKill(String id) {
        // base check
        JobLogPo logInfo = jobLogMapper.selectById(id);
        JobInfoPo jobInfo = jobInfoMapper.selectById(logInfo.getJobId());
        if (jobInfo == null) {
            return new ReturnT<>(500, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        if (HandleCodeConstant.HANDLE_CODE_SUCCESS != logInfo.getTriggerCode()) {
            return new ReturnT<>(500, I18nUtil.getString("joblog_kill_log_limit"));
        }

        // request of kill
        ReturnT<String> runResult;
        try {
            runResult = ExecutorBizProxy.kill(logInfo.getExecutorAddress(), new KillParam(jobInfo.getId()));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            runResult = new ReturnT<>(500, e.getMessage());
        }

        if (HandleCodeConstant.HANDLE_CODE_SUCCESS == runResult.getCode()) {
            logInfo.setHandleCode(HandleCodeConstant.HANDLE_CODE_FAIL);
            logInfo.setHandleMsg(I18nUtil.getString("joblog_kill_log_byman") + ":" + (runResult.getMsg() != null ? runResult.getMsg() : ""));
            logInfo.setHandleTime(new Date());
            JobCompleter.updateHandleInfoAndFinish(logInfo);
            return new ReturnT<>(runResult.getMsg());
        } else {
            return new ReturnT<>(500, runResult.getMsg());
        }
    }

    @RequestMapping("/clearLog")
    @ResponseBody
    public ReturnT<String> clearLog(HttpServletRequest request, String jobGroup, String jobId, int type) {
        // valid permission
        PermissionInterceptor.validJobGroupPermission(request, jobGroup);

        // opt
        Date clearBeforeTime;
        if (type == 1) {
            // 清理一个月之前日志数据
            clearBeforeTime = DateUtil.offsetMonth(new Date(), -1);
        } else if (type == 2) {
            // 清理三个月之前日志数据
            clearBeforeTime = DateUtil.offsetMonth(new Date(), -3);
        } else if (type == 3) {
            // 清理六个月之前日志数据
            clearBeforeTime = DateUtil.offsetMonth(new Date(), -6);
        } else if (type == 4) {
            // 清理一年之前日志数据
            clearBeforeTime = DateUtil.offsetYear(new Date(), -1);
        } else {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, I18nUtil.getString("joblog_clean_type_unvalid"));
        }

        List<String> logIds = null;
        do {
            Page<JobLogPo> jobLogPoPage = jobLogMapper.selectPage(new Page<>(0, 1000L), Wrappers.lambdaQuery(JobLogPo.class)
                    .eq(StrUtil.isNotEmpty(jobGroup), JobLogPo::getJobGroup, jobGroup)
                    .eq(StrUtil.isNotEmpty(jobId), JobLogPo::getJobId, jobId)
                    .eq(clearBeforeTime != null, JobLogPo::getTriggerTime, clearBeforeTime)
            );
            if (jobLogPoPage != null && CollUtil.isNotEmpty(jobLogPoPage.getRecords())) {
                logIds = jobLogPoPage.getRecords().stream().map(JobLogPo::getId).collect(Collectors.toList());
                jobLogMapper.deleteBatchIds(logIds);
            }
        } while (CollUtil.isNotEmpty(logIds));

        return ReturnT.SUCCESS;
    }

}
