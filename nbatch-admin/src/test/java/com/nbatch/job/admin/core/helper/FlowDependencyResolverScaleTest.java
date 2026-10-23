package com.nbatch.job.admin.core.helper;

import com.nbatch.job.admin.core.domain.po.JobWorkFlowDependencyPo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.enums.FlowElementTypeEnum;
import com.nbatch.job.admin.mapper.IJobWorkFlowDependencyMapper;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowDependencyResolverScaleTest {

    private static final int GROUP_COUNT = 50;
    private static final int NODES_PER_GROUP = 100;

    @Test
    void shouldResolveDenseGroupDependenciesWithinReasonableTime() {
        List<JobWorkNodePo> nodes = buildNodes();
        List<JobWorkFlowDependencyPo> dependencies = buildDenseDependencies();
        FlowDependencyResolver resolver = new FlowDependencyResolver(mapperReturning(dependencies));

        long startedAt = System.nanoTime();
        Map<String, FlowDependencyResolver.DependencyState> states = resolver.resolveNodeStates("scale-work", nodes);
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000L;

        assertEquals((GROUP_COUNT - 1) * NODES_PER_GROUP, states.size());
        assertTrue(states.values().stream().allMatch(state -> state == FlowDependencyResolver.DependencyState.READY));
        System.out.println("FLOW_DEPENDENCY_SCALE_METRICS nodes=" + nodes.size()
                + " groups=" + GROUP_COUNT
                + " dependencies=" + dependencies.size()
                + " resolvedStates=" + states.size()
                + " elapsedMs=" + elapsedMillis);
    }

    @Test
    void shouldResolveGroupDependencyByEachSuccessorTurnDate() {
        JobWorkNodePo predecessor = node("predecessor", "predecessor-group", 2,
                FlowRunStatusEnum.COMPLETE.getCode());
        JobWorkNodePo earlierSuccessor = node("earlier-successor", "successor-group", 1,
                FlowRunStatusEnum.WAIT.getCode());
        JobWorkNodePo laterSuccessor = node("later-successor", "successor-group", 3,
                FlowRunStatusEnum.WAIT.getCode());
        FlowDependencyResolver resolver = new FlowDependencyResolver(mapperReturning(
                Collections.singletonList(groupDependency("predecessor-group", "successor-group"))));

        Map<String, FlowDependencyResolver.DependencyState> states = resolver.resolveNodeStates("scale-work",
                Arrays.asList(predecessor, earlierSuccessor, laterSuccessor));

        assertEquals(FlowDependencyResolver.DependencyState.READY, states.get("earlier-successor"));
        assertEquals(FlowDependencyResolver.DependencyState.PENDING, states.get("later-successor"));
    }

    @Test
    void shouldBlockGroupDependencyWhenAnyPredecessorIsFailed() {
        JobWorkNodePo completePredecessor = node("complete-predecessor", "predecessor-group", 1,
                FlowRunStatusEnum.COMPLETE.getCode());
        JobWorkNodePo failedPredecessor = node("failed-predecessor", "predecessor-group", 1,
                FlowRunStatusEnum.EXCEPTION.getCode());
        JobWorkNodePo successor = node("successor", "successor-group", 1, FlowRunStatusEnum.WAIT.getCode());
        FlowDependencyResolver resolver = new FlowDependencyResolver(mapperReturning(
                Collections.singletonList(groupDependency("predecessor-group", "successor-group"))));

        Map<String, FlowDependencyResolver.DependencyState> states = resolver.resolveNodeStates("scale-work",
                Arrays.asList(completePredecessor, failedPredecessor, successor));

        assertEquals(FlowDependencyResolver.DependencyState.BLOCKED, states.get("successor"));
    }

    private List<JobWorkNodePo> buildNodes() {
        List<JobWorkNodePo> nodes = new ArrayList<>();
        Date turnDate = new Date(0L);
        for (int group = 0; group < GROUP_COUNT; group++) {
            for (int node = 0; node < NODES_PER_GROUP; node++) {
                JobWorkNodePo item = new JobWorkNodePo();
                item.setNodeId("node-" + group + "-" + node);
                item.setGroupId("group-" + group);
                item.setTurnDate(turnDate);
                item.setNodeRunStatus(FlowRunStatusEnum.COMPLETE.getCode());
                nodes.add(item);
            }
        }
        return nodes;
    }

    private List<JobWorkFlowDependencyPo> buildDenseDependencies() {
        List<JobWorkFlowDependencyPo> dependencies = new ArrayList<>();
        for (int predecessor = 0; predecessor < GROUP_COUNT; predecessor++) {
            for (int successor = predecessor + 1; successor < GROUP_COUNT; successor++) {
                JobWorkFlowDependencyPo dependency = new JobWorkFlowDependencyPo();
                dependency.setWorkId("scale-work");
                dependency.setPredecessorType(FlowElementTypeEnum.GROUP.name());
                dependency.setPredecessorId("group-" + predecessor);
                dependency.setSuccessorType(FlowElementTypeEnum.GROUP.name());
                dependency.setSuccessorId("group-" + successor);
                dependencies.add(dependency);
            }
        }
        return dependencies;
    }

    private JobWorkNodePo node(String nodeId, String groupId, int turnDay, int status) {
        JobWorkNodePo node = new JobWorkNodePo();
        node.setNodeId(nodeId);
        node.setGroupId(groupId);
        node.setTurnDate(new Date(turnDay * 86_400_000L));
        node.setNodeRunStatus(status);
        return node;
    }

    private JobWorkFlowDependencyPo groupDependency(String predecessorGroupId, String successorGroupId) {
        JobWorkFlowDependencyPo dependency = new JobWorkFlowDependencyPo();
        dependency.setWorkId("scale-work");
        dependency.setPredecessorType(FlowElementTypeEnum.GROUP.name());
        dependency.setPredecessorId(predecessorGroupId);
        dependency.setSuccessorType(FlowElementTypeEnum.GROUP.name());
        dependency.setSuccessorId(successorGroupId);
        return dependency;
    }

    @SuppressWarnings("unchecked")
    private IJobWorkFlowDependencyMapper mapperReturning(List<JobWorkFlowDependencyPo> dependencies) {
        return (IJobWorkFlowDependencyMapper) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IJobWorkFlowDependencyMapper.class}, (proxy, method, args) -> {
                    if ("selectList".equals(method.getName())) {
                        return dependencies;
                    }
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    if (method.getReturnType() == int.class) {
                        return 0;
                    }
                    if (method.getReturnType() == long.class) {
                        return 0L;
                    }
                    return null;
                });
    }
}
