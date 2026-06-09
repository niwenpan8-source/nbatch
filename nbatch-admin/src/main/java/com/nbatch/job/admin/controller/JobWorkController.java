package com.nbatch.job.admin.controller;

import com.nbatch.job.admin.core.domain.param.JobWorkNodeRelationParam;
import com.nbatch.job.admin.core.domain.param.JobWorkPageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkParam;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeRelationVo;
import com.nbatch.job.admin.core.domain.vo.JobWorkVo;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.WorkTypeEnum;
import com.nbatch.job.admin.service.IJobWorkNodeService;
import com.nbatch.job.admin.service.IJobWorkService;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * index controller
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
    private IJobWorkNodeService jobWorkNodeService;

    @RequestMapping
    public String index(Model model) {
        // 枚举-字典路由策略-列表
        model.addAttribute("workStatusEnum", com.nbatch.job.core.enums.FlowStatusEnum.values());
        model.addAttribute("runWorkStatusEnum", FlowRunStatusEnum.values());
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
    public ReturnT<String> recoverRunWork(String runWorkId) {
        return jobWorkService.recoverRunWork(runWorkId);
    }

    /**
     * 一键重跑指定流程最近一次运行作业。
     */
    @RequestMapping("/rerunLatestRunWork")
    @ResponseBody
    public ReturnT<String> rerunLatestRunWork(String workId) {
        return jobWorkService.rerunLatestRunWork(workId);
    }

    @RequestMapping("/addModel")
    public String addModel(Model model) {
        // 枚举-字典路由策略-列表
        model.addAttribute("workStatusEnum", com.nbatch.job.core.enums.FlowStatusEnum.values());
        model.addAttribute("workTypeEnum", WorkTypeEnum.values());
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
        // 枚举-字典路由策略-列表
        model.addAttribute("workStatusEnum", com.nbatch.job.core.enums.FlowStatusEnum.values());
        model.addAttribute("workTypeEnum", WorkTypeEnum.values());
        model.addAttribute("model", workVo);
        return "jobwork/jobwork.update";
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

    @RequestMapping("/editModel")
    public String editModel(Model model, String workId) {
        List<JobWorkNodePo> list = jobWorkNodeService.getWorkNodeList(workId);
        List<JobWorkNodeRelationVo> relationList = jobWorkNodeService.getWorkNodeRelationByWorkId(workId);
        // 枚举-字典路由策略-列表
        model.addAttribute("workId", workId);
        model.addAttribute("list", list);
        model.addAttribute("relationList", relationList);
        return "jobwork/jobwork.edit";
    }

    @RequestMapping("/edit")
    @ResponseBody
    public ReturnT<String> editModel(@RequestBody JobWorkNodeRelationParam param) {
        jobWorkNodeService.updateWorkNodeRelation(param);
        return new ReturnT<>("修改成功");
    }

}
