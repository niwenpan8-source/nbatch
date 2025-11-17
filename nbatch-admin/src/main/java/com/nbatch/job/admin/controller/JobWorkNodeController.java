package com.nbatch.job.admin.controller;

import com.nbatch.job.admin.core.domain.param.JobWorkNodePageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkNodeParam;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeVo;
import com.nbatch.job.admin.core.work.WorkStatusEnum;
import com.nbatch.job.admin.service.IJobWorkNodeService;
import com.nbatch.job.core.biz.model.ReturnT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 作业节点控制器
 *
 * @author Mr.ni
 */
@Slf4j
@Controller
@RequestMapping("/work/node")
public class JobWorkNodeController {

    @Resource
    private IJobWorkNodeService jobWorkNodeService;

    @RequestMapping
    public String index(Model model) {
        // 枚举-字典路由策略-列表
        model.addAttribute("workStatusEnum", WorkStatusEnum.values());
        return "worknode/worknode.index";
    }

    @ResponseBody
    @PostMapping("/pageList")
    public Map<String, Object> pageList(JobWorkNodePageParam param) {
        return jobWorkNodeService.pageList(param);
    }

    @RequestMapping("/delete")
    @ResponseBody
    public ReturnT<String> delete(String id) {
        jobWorkNodeService.delete(id);
        return ReturnT.SUCCESS;
    }

    @RequestMapping("/addModel")
    public String addModel(Model model) {
        // 枚举-字典路由策略-列表
        model.addAttribute("workStatusEnum", WorkStatusEnum.values());
        return "worknode/worknode.add";
    }

    @ResponseBody
    @PostMapping("/insert")
    public ReturnT<Integer> insert(JobWorkNodeParam param) {
        return new ReturnT<>(jobWorkNodeService.insert(param));
    }

    @RequestMapping("/updateModel")
    public String updateModel(Model model, String workNodeId) {
        JobWorkNodeVo workVo = jobWorkNodeService.getModel(workNodeId);
        // 枚举-字典路由策略-列表
        model.addAttribute("workStatusEnum", WorkStatusEnum.values());
        model.addAttribute("model", workVo);
        return "worknode/worknode.update";
    }

    @ResponseBody
    @PostMapping("/update")
    public ReturnT<String> update(JobWorkNodeParam param) {
        if (param.getWorkId() == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "修改失败，作业id不可为空");
        }
        jobWorkNodeService.update(param);
        return new ReturnT<>("修改成功");
    }

}
