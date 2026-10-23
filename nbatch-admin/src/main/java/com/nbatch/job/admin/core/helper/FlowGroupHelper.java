package com.nbatch.job.admin.core.helper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobWorkFlowGroupPo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.enums.FlowGroupTypeEnum;
import com.nbatch.job.admin.mapper.IJobWorkFlowGroupMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程组归属和状态汇总。
 * 组状态不持久化，始终由当前节点状态计算，避免状态双写。
 */
@Component
@RequiredArgsConstructor
public class FlowGroupHelper {

    private static final String ROOT_GROUP_NAME = "默认组";

    private final IJobWorkFlowGroupMapper jobWorkFlowGroupMapper;

    private final IJobWorkNodeMapper jobWorkNodeMapper;

    /**
     * 返回作业的默认组；旧作业首次访问时补建默认组。
     */
    public JobWorkFlowGroupPo ensureRootGroup(String workId) {
        if (workId == null || workId.trim().isEmpty()) {
            throw new IllegalArgumentException("作业ID不能为空");
        }
        JobWorkFlowGroupPo root = findRootGroup(workId);
        if (root != null) {
            return root;
        }
        // 生成默认组，每个work都需要有
        JobWorkFlowGroupPo group = new JobWorkFlowGroupPo();
        group.setGroupId(workId);
        group.setWorkId(workId);
        group.setGroupName(ROOT_GROUP_NAME);
        group.setGroupType(FlowGroupTypeEnum.ROOT.name());
        group.setSortOrder(0);
        group.setVersion(1);
        group.setCreateTime(DateUtil.date());
        group.setUpdateTime(DateUtil.date());
        try {
            jobWorkFlowGroupMapper.insert(group);
            return group;
        } catch (RuntimeException e) {
            // 唯一索引保证并发创建时只保留一个默认组。
            root = findRootGroup(workId);
            if (root != null) {
                return root;
            }
            throw e;
        }
    }

    /**
     * 查找有没有默认值
     */
    public JobWorkFlowGroupPo findRootGroup(String workId) {
        return jobWorkFlowGroupMapper.selectOne(Wrappers.lambdaQuery(JobWorkFlowGroupPo.class)
                .eq(JobWorkFlowGroupPo::getWorkId, workId)
                .eq(JobWorkFlowGroupPo::getGroupType, FlowGroupTypeEnum.ROOT.name())
                .last("LIMIT 1"));
    }

    /**
     * 未指定组时归入默认组；指定组必须属于当前作业。
     */
    public String resolveGroupId(String workId, String groupId) {
        if (groupId == null || groupId.trim().isEmpty()) {
            return ensureRootGroup(workId).getGroupId();
        }
        JobWorkFlowGroupPo group = jobWorkFlowGroupMapper.selectById(groupId);
        if (group == null || !workId.equals(group.getWorkId())) {
            throw new IllegalArgumentException("流程组不存在或不属于当前作业");
        }
        return groupId;
    }

    /**
     * 汇总当前作业各流程组的状态。
     */
    public Map<String, Integer> resolveGroupStatusMap(String workId) {
        List<JobWorkFlowGroupPo> groups = jobWorkFlowGroupMapper.selectList(
                Wrappers.lambdaQuery(JobWorkFlowGroupPo.class).eq(JobWorkFlowGroupPo::getWorkId, workId));
        if (CollUtil.isEmpty(groups)) {
            return Collections.emptyMap();
        }
        List<JobWorkNodePo> nodes = jobWorkNodeMapper.selectList(
                Wrappers.lambdaQuery(JobWorkNodePo.class).eq(JobWorkNodePo::getWorkId, workId));
        Map<String, Integer> result = new HashMap<>();
        for (JobWorkFlowGroupPo group : groups) {
            // 根据节点的状态得到组的状态优先级
            List<JobWorkNodePo> groupNodes = CollUtil.isEmpty(nodes) ? Collections.emptyList()
                    : nodes.stream().filter(node -> group.getGroupId().equals(node.getGroupId()))
                      .collect(java.util.stream.Collectors.toList());
            result.put(group.getGroupId(), resolveStatus(groupNodes));
        }
        return result;
    }

    /**
     * 状态优先级：异常、停止中、已停止、进行中、已下发、等待、未初始化、完成、已跳过。
     */
    public int resolveStatus(List<JobWorkNodePo> nodes) {
        if (CollUtil.isEmpty(nodes)) {
            return FlowRunStatusEnum.WAIT.getCode();
        }
        boolean hasWait = false;
        boolean hasDispatched = false;
        boolean hasRunning = false;
        boolean hasStopping = false;
        boolean hasStopped = false;
        boolean hasException = false;
        boolean hasUninitialized = false;
        boolean hasComplete = false;
        boolean hasSkipped = false;
        for (JobWorkNodePo node : nodes) {
            Integer status = node.getNodeRunStatus();
            if (status == null || ObjUtil.equals(status, FlowRunStatusEnum.WAIT.getCode())) {
                hasWait = true;
            } else if (ObjUtil.equals(status, FlowRunStatusEnum.DISPATCHED.getCode())) {
                hasDispatched = true;
            } else if (ObjUtil.equals(status, FlowRunStatusEnum.RUNNING.getCode())) {
                hasRunning = true;
            } else if (ObjUtil.equals(status, FlowRunStatusEnum.STOPPING.getCode())) {
                hasStopping = true;
            } else if (ObjUtil.equals(status, FlowRunStatusEnum.STOPPED.getCode())) {
                hasStopped = true;
            } else if (ObjUtil.equals(status, FlowRunStatusEnum.EXCEPTION.getCode())) {
                hasException = true;
            } else if (ObjUtil.equals(status, FlowRunStatusEnum.UNINITIALIZED.getCode())) {
                hasUninitialized = true;
            } else if (ObjUtil.equals(status, FlowRunStatusEnum.COMPLETE.getCode())) {
                hasComplete = true;
            } else if (ObjUtil.equals(status, FlowRunStatusEnum.SKIPPED.getCode())) {
                hasSkipped = true;
            }
        }
        if (hasException) {
            return FlowRunStatusEnum.EXCEPTION.getCode();
        }
        if (hasStopping) {
            return FlowRunStatusEnum.STOPPING.getCode();
        }
        if (hasStopped) {
            return FlowRunStatusEnum.STOPPED.getCode();
        }
        if (hasRunning) {
            return FlowRunStatusEnum.RUNNING.getCode();
        }
        if (hasDispatched) {
            return FlowRunStatusEnum.DISPATCHED.getCode();
        }
        if (hasWait) {
            return FlowRunStatusEnum.WAIT.getCode();
        }
        if (hasUninitialized) {
            return FlowRunStatusEnum.UNINITIALIZED.getCode();
        }
        if (hasComplete) {
            return FlowRunStatusEnum.COMPLETE.getCode();
        }
        return hasSkipped ? FlowRunStatusEnum.SKIPPED.getCode() : FlowRunStatusEnum.COMPLETE.getCode();
    }
}
