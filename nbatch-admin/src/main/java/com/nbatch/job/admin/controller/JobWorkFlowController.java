package com.nbatch.job.admin.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobWorkFlowDependencyPo;
import com.nbatch.job.admin.core.domain.po.JobWorkFlowGroupPo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.enums.DbTypeEnum;
import com.nbatch.job.admin.core.enums.FlowGroupTypeEnum;
import com.nbatch.job.admin.core.helper.FlowGroupHelper;
import com.nbatch.job.admin.mapper.IJobWorkFlowDependencyMapper;
import com.nbatch.job.admin.mapper.IJobWorkFlowGroupMapper;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.service.IJobWorkFlowDependService;
import com.nbatch.job.admin.service.IJobWorkFlowEventService;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.NodeTypeEnum;
import com.nbatch.job.core.enums.WorkTypeEnum;
import org.springframework.stereotype.Controller;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 作业流程组与统一依赖接口。
 */
@Controller
@RequestMapping("/flow")
public class JobWorkFlowController {

    @Resource
    private IJobWorkMapper jobWorkMapper;

    @Resource
    private IJobWorkNodeMapper jobWorkNodeMapper;

    @Resource
    private IJobWorkFlowGroupMapper jobWorkFlowGroupMapper;

    @Resource
    private IJobWorkFlowDependencyMapper jobWorkFlowDependencyMapper;

    @Resource
    private FlowGroupHelper flowGroupHelper;

    @Resource
    private IJobWorkFlowDependService jobWorkNodeDependService;

    @Resource
    private IJobWorkFlowEventService jobWorkFlowEventService;

    /**
     * 流程图运行态 SSE：仅发送作业/节点状态、翻牌日期和当前剩余重试次数。
     */
    @GetMapping(value = "/runtime/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter runtimeStream(String workId) {
        if (StrUtil.isBlank(workId) || jobWorkMapper.selectById(workId) == null) {
            SseEmitter emitter = new SseEmitter(0L);
            emitter.completeWithError(new IllegalArgumentException("作业不存在"));
            return emitter;
        }
        return jobWorkFlowEventService.subscribe(workId);
    }

    @ResponseBody
    @PostMapping("/elements")
    public ReturnT<Map<String, Object>> elements(String workId) {
        JobWorkPo work = StrUtil.isBlank(workId) ? null : jobWorkMapper.selectById(workId);
        if (work == null) {
            return ReturnT.error("作业不存在");
        }
        flowGroupHelper.ensureRootGroup(workId);
        // 得到所有的组
        List<JobWorkFlowGroupPo> groups = jobWorkFlowGroupMapper.selectList(
                Wrappers.lambdaQuery(JobWorkFlowGroupPo.class)
                        .eq(JobWorkFlowGroupPo::getWorkId, workId)
                        .orderByAsc(JobWorkFlowGroupPo::getSortOrder));
        // 得到所有的node
        List<JobWorkNodePo> nodes = jobWorkNodeMapper.selectList(
                Wrappers.lambdaQuery(JobWorkNodePo.class)
                        .eq(JobWorkNodePo::getWorkId, workId)
                        .orderByAsc(JobWorkNodePo::getNodeName));
        Map<String, Object> content = new HashMap<>();
        content.put("workName", work.getWorkName());
        content.put("workType", work.getWorkType());
        content.put("workTypeName", work.getWorkType() != null && WorkTypeEnum.TYPE_TURN.getCode() == work.getWorkType()
                ? WorkTypeEnum.TYPE_TURN.getValue() : WorkTypeEnum.TYPE_SEQUENCE.getValue());
        content.put("runWorkStatus", work.getRunWorkStatus());
        content.put("scheduleStatus", work.getScheduleStatus());
        content.put("scheduleConfigured", StrUtil.isNotBlank(work.getScheduleConf()));
        content.put("workTurnDate", work.getTurnDate() == null ? null : DateUtil.formatDate(work.getTurnDate()));
        content.put("groups", groups);
        content.put("nodes", nodes.stream().map(node -> {
            Map<String, Object> item = new HashMap<>();
            item.put("nodeId", node.getNodeId());
            item.put("nodeName", node.getNodeName());
            item.put("nodeDesc", node.getNodeDesc());
            item.put("groupId", node.getGroupId());
            item.put("nodeType", node.getNodeType());
            item.put("nodeTypeName", NodeTypeEnum.getValueByCode(node.getNodeType()));
            item.put("dbType", node.getDbType());
            item.put("dbTypeName", DbTypeEnum.getValue(node.getDbType()));
            item.put("nodeRunStatus", node.getNodeRunStatus());
            item.put("turnDate", node.getTurnDate() == null ? null : DateUtil.formatDate(node.getTurnDate()));
            item.put("errorStrategy", node.getErrorStrategy());
            item.put("currentRetryTimes", node.getCurrentRetryTimes());
            return item;
        }).collect(Collectors.toList()));
        return ReturnT.success(content);
    }

    @ResponseBody
    @PostMapping("/groups")
    public ReturnT<List<Map<String, Object>>> groups(String workId) {
        if (StrUtil.isBlank(workId)) {
            return ReturnT.error("作业ID不能为空");
        }
        if (jobWorkMapper.selectById(workId) == null) {
            return ReturnT.error("作业不存在");
        }
        flowGroupHelper.ensureRootGroup(workId);
        List<JobWorkFlowGroupPo> groups = jobWorkFlowGroupMapper.selectList(
                Wrappers.lambdaQuery(JobWorkFlowGroupPo.class)
                        .eq(JobWorkFlowGroupPo::getWorkId, workId)
                        .orderByAsc(JobWorkFlowGroupPo::getSortOrder));
        Map<String, Integer> statusMap = flowGroupHelper.resolveGroupStatusMap(workId);
        Map<String, Long> nodeCountMap = new HashMap<>();
        List<JobWorkNodePo> nodes = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, workId));
        for (JobWorkNodePo node : nodes) {
            nodeCountMap.merge(node.getGroupId(), 1L, Long::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (JobWorkFlowGroupPo group : groups) {
            Map<String, Object> item = new HashMap<>();
            item.put("groupId", group.getGroupId());
            item.put("groupName", group.getGroupName());
            item.put("groupDesc", group.getGroupDesc());
            item.put("groupType", group.getGroupType());
            item.put("sortOrder", group.getSortOrder());
            Integer status = statusMap.get(group.getGroupId());
            item.put("runStatus", status);
            item.put("runStatusName", status == null ? null : FlowRunStatusEnum.getValueByCode(status));
            item.put("nodeCount", nodeCountMap.getOrDefault(group.getGroupId(), 0L));
            result.add(item);
        }
        return ReturnT.success(result);
    }

    @ResponseBody
    @PostMapping("/group/save")
    public ReturnT<String> saveGroup(JobWorkFlowGroupPo group) {
        if (group == null || StrUtil.isBlank(group.getWorkId()) || StrUtil.isBlank(group.getGroupName())) {
            return ReturnT.error("作业ID和流程组名称不能为空");
        }
        if (jobWorkMapper.selectById(group.getWorkId()) == null) {
            return ReturnT.error("作业不存在");
        }
        Date now = new Date();
        if (StrUtil.isBlank(group.getGroupId())) {
            group.setGroupId(null);
            group.setGroupType(FlowGroupTypeEnum.NORMAL.name());
            group.setSortOrder(group.getSortOrder() == null ? 0 : group.getSortOrder());
            group.setVersion(1);
            group.setCreateTime(now);
            group.setUpdateTime(now);
            jobWorkFlowGroupMapper.insert(group);
            return ReturnT.success(group.getGroupId());
        }
        JobWorkFlowGroupPo current = jobWorkFlowGroupMapper.selectById(group.getGroupId());
        if (current == null || !group.getWorkId().equals(current.getWorkId())) {
            return ReturnT.error("流程组不存在或不属于当前作业");
        }
        current.setGroupName(group.getGroupName());
        if (!StrUtil.equals(FlowGroupTypeEnum.ROOT.name(), current.getGroupType())) {
            current.setGroupDesc(group.getGroupDesc());
            current.setSortOrder(group.getSortOrder() == null ? current.getSortOrder() : group.getSortOrder());
        }
        current.setUpdateTime(now);
        jobWorkFlowGroupMapper.updateById(current);
        return ReturnT.SUCCESS;
    }

    @ResponseBody
    @PostMapping("/group/delete")
    public ReturnT<String> deleteGroup(String workId, String groupId) {
        try {
            jobWorkNodeDependService.deleteGroup(workId, groupId);
            return ReturnT.SUCCESS;
        } catch (IllegalArgumentException exception) {
            return ReturnT.error(exception.getMessage());
        }
    }

    @ResponseBody
    @PostMapping("/dependencies")
    public ReturnT<List<JobWorkFlowDependencyPo>> dependencies(String workId) {
        if (StrUtil.isBlank(workId)) {
            return ReturnT.error("作业ID不能为空");
        }
        return ReturnT.success(jobWorkFlowDependencyMapper.selectList(
                Wrappers.lambdaQuery(JobWorkFlowDependencyPo.class).eq(JobWorkFlowDependencyPo::getWorkId, workId)));
    }

    @ResponseBody
    @PostMapping("/dependency/save")
    public ReturnT<String> saveDependency(JobWorkFlowDependencyPo dependency) {
        try {
            return ReturnT.success(jobWorkNodeDependService.save(dependency));
        } catch (IllegalArgumentException exception) {
            return ReturnT.error(exception.getMessage());
        }
    }

    /**
     * 导出当前作业的流程配置工作簿，包含组、各节点类型和依赖关系 Sheet。
     */
    @PostMapping("/dependency/export")
    public void exportDependencies(String workId, HttpServletResponse response) throws IOException {
        jobWorkNodeDependService.export(workId, response);
    }

    /**
     * 下载流程配置导入模板，Sheet 顺序为组、各节点类型、依赖关系。
     */
    @PostMapping("/dependency/template")
    public void downloadDependencyTemplate(HttpServletResponse response) throws IOException {
        jobWorkNodeDependService.downloadTemplate(response);
    }

    /**
     * 导入流程配置工作簿；服务层按 Sheet 顺序处理并在事务中完成校验与写入。
     */
    @ResponseBody
    @PostMapping("/dependency/import")
    public ReturnT<Integer> importDependencies(String workId, @RequestParam("file") MultipartFile file) {
        try {
            return ReturnT.success(jobWorkNodeDependService.importExcel(workId, file));
        } catch (IllegalArgumentException exception) {
            return ReturnT.error(exception.getMessage());
        }
    }

    @ResponseBody
    @PostMapping("/dependency/delete")
    public ReturnT<String> deleteDependency(String workId, String dependencyId) {
        int count = jobWorkFlowDependencyMapper.delete(Wrappers.lambdaQuery(JobWorkFlowDependencyPo.class)
                .eq(JobWorkFlowDependencyPo::getWorkId, workId)
                .eq(JobWorkFlowDependencyPo::getDependencyId, dependencyId));
        return count == 1 ? ReturnT.SUCCESS : new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "依赖不存在");
    }

}
