package com.nbatch.job.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.param.JobWorkNodeLogPageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkNodePageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkNodeParam;
import com.nbatch.job.admin.core.domain.po.JobWorkFlowGroupPo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogDetailPo;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeVo;
import com.nbatch.job.admin.core.domain.vo.JobWorkRunNodeLogVo;
import com.nbatch.job.admin.core.enums.DbTypeEnum;
import com.nbatch.job.admin.mapper.IJobWorkFlowGroupMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.core.enums.NodeTypeEnum;
import com.nbatch.job.admin.service.IJobWorkNodeService;
import com.nbatch.job.admin.service.IJobWorkNodeRunService;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.ScriptTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 作业节点控制器
 */
@Slf4j
@Controller
@RequestMapping("/node")
public class JobWorkNodeController {

    @Resource
    private IJobWorkNodeService jobWorkNodeService;

    @Resource
    private IJobWorkNodeRunService jobWorkNodeRunService;

    @Resource
    private IJobWorkFlowGroupMapper jobWorkFlowGroupMapper;

    @Resource
    private IJobWorkNodeMapper jobWorkNodeMapper;

    @RequestMapping
    public String index(Model model) {
        // 枚举-字典路由策略-列表
        List<JobWorkPo> allEnableWorkList = jobWorkNodeService.getAllWorkList();
        model.addAttribute("allEnableWorkList", allEnableWorkList);
        model.addAttribute("nodeTypeEnum", NodeTypeEnum.values());
        model.addAttribute("runStatusEnum", FlowRunStatusEnum.values());
        return "worknode/worknode.index";
    }

    @ResponseBody
    @PostMapping("/pageList")
    public Map<String, Object> pageList(JobWorkNodePageParam param) {
        return jobWorkNodeService.pageList(param);
    }

    @ResponseBody
    @PostMapping("/detail")
    public ReturnT<Map<String, Object>> detail(String nodeId) {
        return jobWorkNodeService.detail(nodeId);
    }

    @RequestMapping("/delete")
    @ResponseBody
    public ReturnT<String> delete(String id) {
        jobWorkNodeService.delete(id);
        return ReturnT.SUCCESS;
    }

    @RequestMapping("/addModel")
    public String addModel(Model model, String workId, Boolean fromFlow) {
        // 枚举-字典路由策略-列表
        List<JobWorkPo> allEnableWorkList = jobWorkNodeService.getAllWorkList();
        model.addAttribute("allEnableWorkList", allEnableWorkList);
        model.addAttribute("nodeTypeEnum", NodeTypeEnum.values());
        model.addAttribute("scriptTypeEnum", ScriptTypeEnum.values());
        model.addAttribute("dbTypeEnum", DbTypeEnum.values());
        model.addAttribute("selectedWorkId", workId);
        model.addAttribute("fromFlow", Boolean.TRUE.equals(fromFlow));
        return "worknode/worknode.add";
    }

    @ResponseBody
    @PostMapping("/insert")
    public ReturnT<Integer> insert(JobWorkNodeParam param) {
        return new ReturnT<>(jobWorkNodeService.insert(param));
    }

    @RequestMapping("/updateModel")
    public String updateModel(Model model, String workNodeId, Boolean fromFlow) {
        JobWorkNodeVo workVo = jobWorkNodeService.getModel(workNodeId);
        // 枚举-字典路由策略-列表
        List<JobWorkPo> allEnableWorkList = jobWorkNodeService.getAllWorkList();
        model.addAttribute("allEnableWorkList", allEnableWorkList);
        model.addAttribute("nodeTypeEnum", NodeTypeEnum.values());
        model.addAttribute("scriptTypeEnum", ScriptTypeEnum.values());
        model.addAttribute("dbTypeEnum", DbTypeEnum.values());
        model.addAttribute("fromFlow", Boolean.TRUE.equals(fromFlow));
        if (workVo != null) {
            model.addAttribute("model", workVo);
        } else {
            model.addAttribute("model", new JobWorkNodeVo());
        }

        return "worknode/worknode.update";
    }

    @ResponseBody
    @PostMapping("/update")
    public ReturnT<String> update(JobWorkNodeParam param) {
        if (param.getNodeId() == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "修改失败，作业id不可为空");
        }
        jobWorkNodeService.update(param);
        return new ReturnT<>("修改成功");
    }

    /**
     * 运行节点日志统一入口，页面内通过标签切换执行日志和详细日志。
     */
    @RequestMapping("/viewLogModel")
    public String viewLogModel(Model model, String workId, String groupId, String nodeId, String workNodeId) {
        String resolvedNodeId = StringUtils.hasText(nodeId) ? nodeId : workNodeId;
        JobWorkNodePo workNode = StringUtils.hasText(resolvedNodeId) ? jobWorkNodeService.getWorkNode(resolvedNodeId) : null;
        if (workNode != null) {
            workId = workNode.getWorkId();
            nodeId = workNode.getNodeId();
            groupId = null;
        }
        String logScopeValue = workNode != null ? "NODE:" + nodeId
                : (StringUtils.hasText(groupId) ? "GROUP:" + groupId : "WORK");
        model.addAttribute("workId", workId);
        model.addAttribute("logScopeValue", logScopeValue);
        List<JobWorkFlowGroupPo> flowGroups = Collections.emptyList();
        List<JobWorkNodePo> workNodes = Collections.emptyList();
        if (StringUtils.hasText(workId)) {
            flowGroups = jobWorkFlowGroupMapper.selectList(Wrappers.lambdaQuery(JobWorkFlowGroupPo.class)
                    .eq(JobWorkFlowGroupPo::getWorkId, workId)
                    .orderByAsc(JobWorkFlowGroupPo::getSortOrder));
            workNodes = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                    .eq(JobWorkNodePo::getWorkId, workId)
                    .orderByAsc(JobWorkNodePo::getNodeName));
        }
        model.addAttribute("flowGroups", flowGroups);
        model.addAttribute("workNodes", workNodes);
        return "worknode/worknode.view.logs";
    }

    @RequestMapping("/rerun")
    @ResponseBody
    public ReturnT<String> rerunNode(String nodeId, String turnDate) {
        return jobWorkNodeRunService.rerunNode(nodeId, turnDate);
    }

    @RequestMapping("/rerunDependencies")
    @ResponseBody
    public ReturnT<String> rerunNodeWithDependents(String nodeId, String turnDate) {
        return jobWorkNodeRunService.rerunNodeWithDependents(nodeId, turnDate);
    }

    @ResponseBody
    @PostMapping("/logPageList")
    public ReturnT<IPage<JobWorkRunNodeLogVo>> logPageList(JobWorkNodeLogPageParam param) {
        return new ReturnT<>(jobWorkNodeService.logPageList(param));
    }

    @ResponseBody
    @PostMapping("/logDetailPageList")
    public ReturnT<IPage<JobWorkRunNodeLogDetailPo>> logDetailPageList(JobWorkNodeLogPageParam param) {
        return new ReturnT<>(jobWorkNodeService.logDetailPageList(param));
    }

}
