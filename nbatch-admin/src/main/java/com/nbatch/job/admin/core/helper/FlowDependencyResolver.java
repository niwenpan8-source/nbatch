package com.nbatch.job.admin.core.helper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobWorkFlowDependencyPo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.enums.FlowElementTypeEnum;
import com.nbatch.job.admin.mapper.IJobWorkFlowDependencyMapper;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 计算节点和流程组依赖是否满足。
 */
@Component
@RequiredArgsConstructor
public class FlowDependencyResolver {

    private final IJobWorkFlowDependencyMapper jobWorkFlowDependencyMapper;

    /**
     * 分析当前节点可执行状态
     *
     * @param workId 作业ID
     * @param nodes  节点列表
     * @return 节点ID -> 依赖状态
     */
    public Map<String, DependencyState> resolveNodeStates(String workId, List<JobWorkNodePo> nodes) {
        if (CollUtil.isEmpty(nodes)) {
            return Collections.emptyMap();
        }
        // 节点和流程组索引
        Map<String, JobWorkNodePo> nodeMap = new HashMap<>();
        Map<String, List<JobWorkNodePo>> groupNodeMap = new HashMap<>();
        // key: 节点ID，value: 节点翻牌日的开始时间
        Map<String, Long> nodeTurnDayMap = new HashMap<>();
        for (JobWorkNodePo node : nodes) {
            nodeMap.put(node.getNodeId(), node);
            nodeTurnDayMap.put(node.getNodeId(), toTurnDay(node.getTurnDate()));
            if (node.getGroupId() != null) {
                groupNodeMap.computeIfAbsent(node.getGroupId(), key -> new ArrayList<>()).add(node);
            }
        }

        // 只读取当前作业的依赖配置。
        List<JobWorkFlowDependencyPo> dependencies = jobWorkFlowDependencyMapper.selectList(
                Wrappers.lambdaQuery(JobWorkFlowDependencyPo.class)
                        .eq(JobWorkFlowDependencyPo::getWorkId, workId));
        if (CollUtil.isEmpty(dependencies)) {
            return Collections.emptyMap();
        }

        Map<String, DependencyState> states = new HashMap<>();
        Map<GroupTurnDayKey, DependencyState> groupStateCache = new HashMap<>();
        for (JobWorkFlowDependencyPo dependency : dependencies) {
            // 对象可能是节点或流程组，统一展开为具体节点。
            List<JobWorkNodePo> successorNodes = resolveSuccessorNodes(dependency, nodeMap, groupNodeMap);
            for (JobWorkNodePo successorNode : successorNodes) {
                // 得到翻牌日期的开始时间
                Long successorTurnDay = nodeTurnDayMap.get(successorNode.getNodeId());
                DependencyState prerequisiteState = resolveElementState(dependency.getPredecessorType(),
                        dependency.getPredecessorId(), successorTurnDay, nodeMap, groupNodeMap,
                        nodeTurnDayMap, groupStateCache);
                if (prerequisiteState == null) {
                    // 依赖对象已不存在时按阻塞处理，避免绕过错误配置继续执行。
                    prerequisiteState = DependencyState.BLOCKED;
                }
                states.merge(successorNode.getNodeId(), prerequisiteState, DependencyState::merge);
            }
        }
        return states;
    }

    /**
     * 计算前置对象对后置节点的依赖状态。
     */
    private DependencyState resolveElementState(String type, String id, Long successorTurnDay,
                                                Map<String, JobWorkNodePo> nodeMap,
                                                Map<String, List<JobWorkNodePo>> groupNodeMap,
                                                Map<String, Long> nodeTurnDayMap,
                                                Map<GroupTurnDayKey, DependencyState> groupStateCache) {
        if (FlowElementTypeEnum.NODE.name().equals(type)) {
            JobWorkNodePo node = nodeMap.get(id);
            return node == null ? null : fromRunStatus(node, nodeTurnDayMap.get(id), successorTurnDay);
        }
        if (FlowElementTypeEnum.GROUP.name().equals(type)) {
            List<JobWorkNodePo> nodes = groupNodeMap.get(id);
            if (CollUtil.isEmpty(nodes)) {
                return null;
            }
            GroupTurnDayKey cacheKey = new GroupTurnDayKey(id, successorTurnDay);
            // 根据组得到节点状态
            return groupStateCache.computeIfAbsent(cacheKey,
                    key -> resolveGroupState(nodes, successorTurnDay, nodeTurnDayMap));
        }
        return null;
    }

    /**
     * 同一前置组相对同一后置翻牌日的状态只计算一次。
     */
    private DependencyState resolveGroupState(List<JobWorkNodePo> nodes, Long successorTurnDay,
                                              Map<String, Long> nodeTurnDayMap) {
        DependencyState result = DependencyState.READY;
        for (JobWorkNodePo node : nodes) {
            result = DependencyState.merge(result,
                    fromRunStatus(node, nodeTurnDayMap.get(node.getNodeId()), successorTurnDay));
            if (result == DependencyState.BLOCKED) {
                return result;
            }
        }
        return result;
    }

    /**
     * 将后置对象展开为具体节点。
     */
    private List<JobWorkNodePo> resolveSuccessorNodes(JobWorkFlowDependencyPo dependency,
                                                      Map<String, JobWorkNodePo> nodeMap,
                                                      Map<String, List<JobWorkNodePo>> groupNodeMap) {
        if (FlowElementTypeEnum.NODE.name().equals(dependency.getSuccessorType())) {
            JobWorkNodePo node = nodeMap.get(dependency.getSuccessorId());
            return node == null ? Collections.emptyList() : Collections.singletonList(node);
        }
        if (!FlowElementTypeEnum.GROUP.name().equals(dependency.getSuccessorType())) {
            return Collections.emptyList();
        }
        List<JobWorkNodePo> groupNodes = groupNodeMap.get(dependency.getSuccessorId());
        return CollUtil.isEmpty(groupNodes) ? Collections.emptyList() : groupNodes;
    }

    /**
     * 根据前置节点状态和批次日期计算依赖状态
     *
     * @param predecessorNode    前置节点
     * @param predecessorTurnDay 前置节点翻牌日
     * @param successorTurnDay   后置节点翻牌日
     * @return 依赖状态
     */
    private DependencyState fromRunStatus(JobWorkNodePo predecessorNode, Long predecessorTurnDay,
                                          Long successorTurnDay) {
        // 前置节点为空
        if (predecessorNode == null) {
            return DependencyState.BLOCKED;
        }

        if (predecessorTurnDay != null && successorTurnDay != null) {
            int dateCompare = Long.compare(predecessorTurnDay, successorTurnDay);
            // 如果前置节点时间 大于 后置节点时间，则认为前置节点已完成，可以执行后置节点
            if (dateCompare > 0) {
                return DependencyState.READY;
            }
            // 如果前置节点时间 小于 后置节点时间，则认为前置节点未完成，不能执行后置节点
            if (dateCompare < 0) {
                return DependencyState.PENDING;
            }
        }
        // 如果时间不存在或者相等则根据状态判断
        Integer status = predecessorNode.getNodeRunStatus();
        if (status == null || ObjUtil.equals(status, FlowRunStatusEnum.WAIT.getCode())
                || ObjUtil.equals(status, FlowRunStatusEnum.UNINITIALIZED.getCode())
                || ObjUtil.equals(status, FlowRunStatusEnum.DISPATCHED.getCode())
                || ObjUtil.equals(status, FlowRunStatusEnum.RUNNING.getCode())) {
            return DependencyState.PENDING;
        }
        if (ObjUtil.equals(status, FlowRunStatusEnum.COMPLETE.getCode())
                || ObjUtil.equals(status, FlowRunStatusEnum.SKIPPED.getCode())) {
            return DependencyState.READY;
        }
        return DependencyState.BLOCKED;
    }

    private Long toTurnDay(Date turnDate) {
        // 获取某天的开始时间
        return turnDate == null ? null : DateUtil.beginOfDay(turnDate).getTime();
    }

    /**
     * 流程组状态取决于后置节点翻牌日，因此该值必须参与缓存键。
     */
    private static final class GroupTurnDayKey {

        private final String groupId;
        private final Long successorTurnDay;

        private GroupTurnDayKey(String groupId, Long successorTurnDay) {
            this.groupId = groupId;
            this.successorTurnDay = successorTurnDay;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof GroupTurnDayKey)) {
                return false;
            }
            GroupTurnDayKey that = (GroupTurnDayKey) other;
            return Objects.equals(groupId, that.groupId)
                    && Objects.equals(successorTurnDay, that.successorTurnDay);
        }

        @Override
        public int hashCode() {
            return Objects.hash(groupId, successorTurnDay);
        }
    }

    /**
     * 依赖判断结果。
     */
    public enum DependencyState {

        /**
         * 全部前置条件已满足。
         */
        READY,

        /**
         * 前置节点尚未完成。
         */
        PENDING,

        /**
         * 前置节点失败、停止或依赖对象不存在。
         */
        BLOCKED;

        private static DependencyState merge(DependencyState left, DependencyState right) {
            if (left == BLOCKED || right == BLOCKED) {
                return BLOCKED;
            }
            if (left == PENDING || right == PENDING) {
                return PENDING;
            }
            return READY;
        }
    }
}
