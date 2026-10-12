package com.nbatch.job.admin.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.param.JobWorkPageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkParam;
import com.nbatch.job.admin.core.domain.po.JobGroupPo;
import com.nbatch.job.admin.core.domain.vo.JobWorkVo;
import com.nbatch.job.admin.mapper.IJobGroupMapper;
import com.nbatch.job.admin.service.IJobWorkService;
import com.nbatch.job.admin.service.IJobWorkRunService;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.WorkTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Map;

/**
 * 作业流程控制器
 *
 * @author Mr.ni
 */
@Slf4j
@Controller
@RequestMapping("/work")
public class JobWorkController {

    @Resource
    private IJobWorkService jobWorkService;

    @Resource
    private IJobWorkRunService jobWorkRunService;

    @Resource
    private IJobGroupMapper jobGroupMapper;

    @RequestMapping
    public String index(Model model) {
        model.addAttribute("runWorkStatusEnum", Arrays.stream(FlowRunStatusEnum.values())
                .filter(status -> status != FlowRunStatusEnum.UNINITIALIZED)
                .toArray(FlowRunStatusEnum[]::new));
        return "jobwork/jobwork.index";
    }

    @ResponseBody
    @PostMapping("/pageList")
    public Map<String, Object> pageList(JobWorkPageParam param) {
        return jobWorkService.pageList(param);
    }

    @RequestMapping("/delete")
    @ResponseBody
    public ReturnT<String> delete(String id) {
        jobWorkService.delete(id);
        return ReturnT.SUCCESS;
    }

    /**
     * 将进行中的运行作业恢复为待执行，用于异常中断后的手动恢复。
     */
    @RequestMapping("/recoverRunWork")
    @ResponseBody
    public ReturnT<String> recoverRunWork(String workId, Boolean force) {
        return jobWorkRunService.recoverRunWork(workId, Boolean.TRUE.equals(force));
    }

    /**
     * 停止指定流程最近一次运行作业。
     */
    @RequestMapping("/stopLatestRunWork")
    @ResponseBody
    public ReturnT<String> stopLatestRunWork(String workId) {
        return jobWorkRunService.stopLatestRunWork(workId);
    }

    /**
     * 重跑指定流程。普通作业重跑最新批次，翻牌作业使用请求指定的翻牌日期。
     */
    @RequestMapping("/rerunRunWork")
    @ResponseBody
    public ReturnT<String> rerunRunWork(String workId, String turnDate, Boolean force) {
        return jobWorkRunService.rerunRunWork(workId, turnDate, Boolean.TRUE.equals(force));
    }

    @RequestMapping("/addModel")
    public String addModel(Model model) {
        model.addAttribute("workTypeEnum", WorkTypeEnum.values());
        addExecutorOptions(model);
        return "jobwork/jobwork.add";
    }

    @ResponseBody
    @PostMapping("/insert")
    public ReturnT<Integer> insert(JobWorkParam param) {
        return new ReturnT<>(jobWorkService.insert(param));
    }

    @RequestMapping("/updateModel")
    public String updateModel(Model model, String workId) {
        JobWorkVo workVo = jobWorkService.getModel(workId);
        model.addAttribute("workTypeEnum", WorkTypeEnum.values());
        model.addAttribute("model", workVo);
        addExecutorOptions(model);
        return "jobwork/jobwork.update";
    }

    private void addExecutorOptions(Model model) {
        model.addAttribute("jobGroupList", jobGroupMapper.selectList(Wrappers.lambdaQuery(JobGroupPo.class)
                .orderByAsc(JobGroupPo::getAppName).orderByAsc(JobGroupPo::getId)));
    }

    @ResponseBody
    @PostMapping("/update")
    public ReturnT<String> update(JobWorkParam param) {
        if (param.getWorkId() == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "修改失败，作业id不可为空");
        }
        jobWorkService.update(param);
        return new ReturnT<>("修改成功");
    }

    @PostMapping("/startSchedule")
    @ResponseBody
    public ReturnT<String> startSchedule(String workId) {
        return jobWorkService.startSchedule(workId);
    }

    @PostMapping("/stopSchedule")
    @ResponseBody
    public ReturnT<String> stopSchedule(String workId) {
        return jobWorkService.stopSchedule(workId);
    }

    @RequestMapping("/editModel")
    public String editModel(Model model, String workId) {
        model.addAttribute("workId", workId);
        model.addAttribute("runStatusEnum", FlowRunStatusEnum.values());
        return "jobwork/jobwork.edit";
    }

}
