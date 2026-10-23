package com.nbatch.job.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobWorkFlowDependencyPo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogPo;
import com.nbatch.job.admin.core.enums.FlowElementTypeEnum;
import com.nbatch.job.admin.core.executor.ExecutorBizProxy;
import com.nbatch.job.admin.core.helper.RunNodeStopCommandHelper;
import com.nbatch.job.admin.core.helper.RunWorkHelper;
import com.nbatch.job.admin.mapper.IJobWorkFlowDependencyMapper;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeLogMapper;
import com.nbatch.job.admin.service.IJobWorkNodeRunService;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.RunNodeStatusQueryParam;
import com.nbatch.job.core.biz.model.RunNodeStatusQueryResult;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.RunNodeExecutionStateEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 作业节点运行控制：节点重跑、执行器停止和节点退出确认。
 */
@Service
@Slf4j
public class JobWorkNodeRunServiceImpl implements IJobWorkNodeRunService {

    @Resource
    private IJobWorkNodeMapper jobWorkNodeMapper;
    @Resource
    private IJobWorkMapper jobWorkMapper;
    @Resource
    private IJobWorkFlowDependencyMapper jobWorkFlowDependencyMapper;
    @Resource
    private IJobWorkRunNodeLogMapper jobWorkRunNodeLogMapper;
    @Resource
    private RunWorkHelper runWorkHelper;
    @Resource
    private RunNodeStopCommandHelper runNodeStopCommandHelper;

    /**
     * 回拨重跑当前节点。日期为空时使用当前作业批次翻牌日期。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> rerunNode(String nodeId, String turnDate) {
        if (StrUtil.isBlank(nodeId)) {
            return ReturnT.error("步骤ID不能为空");
        }
        JobWorkNodePo runNode = jobWorkNodeMapper.selectById(nodeId);
        if (runNode == null) {
            return ReturnT.error("运行节点不存在");
        }
        JobWorkPo runWork = getCurrentWork(runNode.getWorkId());
        if (runWork == null) {
            return ReturnT.error("运行作业不存在");
        }
        runWorkHelper.lockRunWork(runWork.getWorkId());
        runNode = jobWorkNodeMapper.selectById(nodeId);
        if (runNode == null) {
            return ReturnT.error("运行作业或运行节点不存在");
        }
        // 判断是否可以进行重跑
        if (!isRerunnableStatus(runNode.getNodeRunStatus())) {
            return ReturnT.error("只有执行成功、执行异常、已停止或未初始化的步骤可以重跑");
        }
        List<JobWorkNodePo> allNodes = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, runWork.getWorkId()));
        Map<String, JobWorkNodePo> nodeMap = allNodes.stream().collect(Collectors.toMap(
                JobWorkNodePo::getNodeId, node -> node, (left, right) -> left));
        List<JobWorkFlowDependencyPo> dependencies = jobWorkFlowDependencyMapper.selectList(
                Wrappers.lambdaQuery(JobWorkFlowDependencyPo.class)
                        .eq(JobWorkFlowDependencyPo::getWorkId, runWork.getWorkId()));
        // 得到该节点的依赖节点
        List<JobWorkNodePo> activeDependentNodes = resolveDependentNodeIds(nodeId, allNodes, dependencies).stream()
                .filter(dependentNodeId -> !StrUtil.equals(nodeId, dependentNodeId))
                .map(nodeMap::get)
                .filter(Objects::nonNull)
                .filter(this::isActiveNode)
                .collect(Collectors.toList());
        // 如果存在依赖节点在客户端运行
        if (CollUtil.isNotEmpty(activeDependentNodes)) {
            String nodeNames = activeDependentNodes.stream()
                    .map(node -> StrUtil.blankToDefault(node.getNodeName(), node.getNodeId()))
                    .limit(3).collect(Collectors.joining("、"));
            String suffix = activeDependentNodes.size() > 3 ? "等" + activeDependentNodes.size() + "个节点" : "";
            return ReturnT.error("下游依赖节点正在执行，暂不能单独重跑：" + nodeNames + suffix);
        }
        Date rerunTurnDate;
        try {
            rerunTurnDate = StrUtil.isBlank(turnDate) ? runWork.getTurnDate() : DateUtil.parseDate(turnDate);
        } catch (Exception e) {
            return ReturnT.error("翻牌日期格式不正确，请使用 yyyy-MM-dd");
        }
        if (rerunTurnDate != null) {
            rerunTurnDate = DateUtil.beginOfDay(rerunTurnDate);
        }
        if (rerunTurnDate != null && runWork.getTurnDate() != null
                && DateUtil.compare(rerunTurnDate, DateUtil.beginOfDay(runWork.getTurnDate())) > 0) {
            return ReturnT.error("翻牌日期不能晚于当前批次翻牌日期");
        }
        if (!isUninitialized(runNode)) {
            ReturnT<String> exitCheck = stopAndConfirmRunNodesExited(
                    runWork.getWorkId(), Collections.singletonList(runNode));
            if (!exitCheck.isSuccess()) {
                return exitCheck;
            }
        }
        int updateCount = resetNode(runNode, rerunTurnDate);
        if (updateCount != 1) {
            return ReturnT.error("步骤状态已变化，请刷新后重试");
        }
        if (!isUninitialized(runNode)) {
            closeUnclosedRunNodeLogs(Collections.singletonList(runNode), "节点回拨重跑，关闭旧的未闭环日志");
        }
        resetRunWorkStatus(runWork.getWorkId());
        return ReturnT.SUCCESS;
    }

    /**
     * 重跑当前节点及其所有下游依赖节点；翻牌作业可从指定日期开始逐日追平当前批次。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> rerunNodeWithDependents(String nodeId, String turnDate) {
        if (StrUtil.isBlank(nodeId)) {
            return ReturnT.error("步骤ID不能为空");
        }
        JobWorkNodePo selectedNode = jobWorkNodeMapper.selectById(nodeId);
        if (selectedNode == null) {
            return ReturnT.error("运行节点不存在");
        }
        JobWorkPo runWork = getCurrentWork(selectedNode.getWorkId());
        if (runWork == null) {
            return ReturnT.error("运行作业不存在");
        }
        runWorkHelper.lockRunWork(runWork.getWorkId());
        List<JobWorkNodePo> allNodes = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, runWork.getWorkId()));
        Map<String, JobWorkNodePo> nodeMap = allNodes.stream().collect(Collectors.toMap(
                JobWorkNodePo::getNodeId, node -> node, (left, right) -> left));
        if (!nodeMap.containsKey(nodeId)) {
            return ReturnT.error("运行节点不存在");
        }
        if (!isRerunnableStatus(nodeMap.get(nodeId).getNodeRunStatus())) {
            return ReturnT.error("只有执行成功、执行异常、已停止或未初始化的步骤可以重跑");
        }
        List<JobWorkFlowDependencyPo> dependencies = jobWorkFlowDependencyMapper.selectList(
                Wrappers.lambdaQuery(JobWorkFlowDependencyPo.class)
                        .eq(JobWorkFlowDependencyPo::getWorkId, runWork.getWorkId()));
        List<JobWorkNodePo> rerunNodes = resolveDependentNodeIds(nodeId, allNodes, dependencies).stream()
                .map(nodeMap::get).filter(Objects::nonNull).collect(Collectors.toList());
        for (JobWorkNodePo node : rerunNodes) {
            if (!isRerunnableStatus(node.getNodeRunStatus())) {
                return ReturnT.error(StrUtil.format("只有执行成功、执行异常、已停止或未初始化的步骤可以重跑,{}-状态:{}",
                        node.getNodeName(), FlowRunStatusEnum.getValueByCode(node.getNodeRunStatus())));
            }
        }
        Date rerunTurnDate;
        try {
            rerunTurnDate = StrUtil.isBlank(turnDate) ? runWork.getTurnDate() : DateUtil.parseDate(turnDate);
        } catch (Exception e) {
            return ReturnT.error("翻牌日期格式不正确，请使用 yyyy-MM-dd");
        }
        if (rerunTurnDate != null) {
            rerunTurnDate = DateUtil.beginOfDay(rerunTurnDate);
        }
        if (rerunTurnDate != null && runWork.getTurnDate() != null
                && DateUtil.compare(rerunTurnDate, DateUtil.beginOfDay(runWork.getTurnDate())) > 0) {
            return ReturnT.error("翻牌日期不能晚于当前批次翻牌日期");
        }
        // 这段代码是为了保险
        List<JobWorkNodePo> previousRunNodes = rerunNodes.stream()
                .filter(node -> !isUninitialized(node))
                .collect(Collectors.toList());
        ReturnT<String> exitCheck = stopAndConfirmRunNodesExited(runWork.getWorkId(), previousRunNodes);
        if (!exitCheck.isSuccess()) {
            return exitCheck;
        }
        for (JobWorkNodePo node : rerunNodes) {
            if (resetNode(node, rerunTurnDate) != 1) {
                return ReturnT.error("步骤状态已变化，请刷新后重试");
            }
        }
        closeUnclosedRunNodeLogs(previousRunNodes, "节点及依赖节点重跑，关闭旧的未闭环日志");
        resetRunWorkStatus(runWork.getWorkId());
        return ReturnT.SUCCESS;
    }

    /**
     * 状态重置前再次下发停止命令，并要求执行器明确确认旧尝试已经停止。
     */
    @Override
    public ReturnT<String> stopRunNodes(String workId, List<JobWorkNodePo> runNodeList) {
        return runNodeStopCommandHelper.send(getUnclosedRunNodeLogs(workId, runNodeList));
    }

    /**
     * 停止目标节点，并确认执行器已经返回停止状态。
     */
    @Override
    public ReturnT<String> stopAndConfirmRunNodesExited(String workId, List<JobWorkNodePo> runNodeList) {
        // 停止命令尚未被全部执行器确认，暂不能重置
        ReturnT<String> stopResult = stopRunNodes(workId, runNodeList);
        if (!stopResult.isSuccess()) {
            return ReturnT.error("停止命令尚未被全部执行器确认，暂不能重置：" + stopResult.getMsg());
        }
        return confirmRunNodeLogsStopped(getUnclosedRunNodeLogs(workId, runNodeList));
    }

    /**
     * 通过未闭环的日志，通过远程重新确认他的状态是否闭环，如果存在没有闭环的则不给通过，否则给通过，同时刷新日志的状态
     */
    private ReturnT<String> confirmRunNodeLogsStopped(List<JobWorkRunNodeLogPo> runNodeLogs) {
        if (CollUtil.isEmpty(runNodeLogs)) {
            return ReturnT.SUCCESS;
        }
        for (JobWorkRunNodeLogPo logPo : runNodeLogs) {
            if (StrUtil.isBlank(logPo.getNodeLogId()) || StrUtil.isBlank(logPo.getExecutorAddress())) {
                return ReturnT.error("运行节点缺少执行器或执行日志标识，无法确认旧任务已经退出，nodeId:" + logPo.getNodeId());
            }
        }
        Map<String, List<JobWorkRunNodeLogPo>> addressLogMap = runNodeLogs.stream()
                .collect(Collectors.groupingBy(JobWorkRunNodeLogPo::getExecutorAddress));
        for (Map.Entry<String, List<JobWorkRunNodeLogPo>> entry : addressLogMap.entrySet()) {
            List<String> nodeLogIds = entry.getValue().stream().map(JobWorkRunNodeLogPo::getNodeLogId)
                    .collect(Collectors.toList());
            // 获取未闭环的运行节点日志在管理端的执行状态
            ReturnT<RunNodeStatusQueryResult> result = ExecutorBizProxy.queryRunNodeStatus(entry.getKey(),
                    new RunNodeStatusQueryParam().setNodeLogIdList(nodeLogIds));
            if (!result.isSuccess() || result.getContent() == null || CollUtil.isEmpty(result.getContent().getNodeStateMap())) {
                return ReturnT.error("执行器状态查询失败，暂不能重置，executor:" + entry.getKey());
            }
            for (JobWorkRunNodeLogPo logPo : entry.getValue()) {
                String state = result.getContent().getNodeStateMap().get(logPo.getNodeLogId());
                if (!RunNodeExecutionStateEnum.STOPPED.getValue().equals(state)) {
                    return ReturnT.error(buildRecoveryBlockedMessage(logPo, state));
                }
                markConfirmedStopped(logPo);
            }
        }
        return ReturnT.SUCCESS;
    }

    /**
     * 执行器明确确认已停止时，完成停止中的节点和日志闭环。
     */
    private void markConfirmedStopped(JobWorkRunNodeLogPo logPo) {
        jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                .set(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.STOPPED.getCode())
                .eq(JobWorkNodePo::getWorkId, logPo.getWorkId())
                .eq(JobWorkNodePo::getNodeId, logPo.getNodeId())
                .eq(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.STOPPING.getCode()));
        jobWorkRunNodeLogMapper.update(null, Wrappers.lambdaUpdate(JobWorkRunNodeLogPo.class)
                .set(JobWorkRunNodeLogPo::getHandleCode, HandleCodeConstant.HANDLE_CODE_FAIL)
                .set(JobWorkRunNodeLogPo::getHandleMsg, "执行器确认运行节点已停止")
                .set(JobWorkRunNodeLogPo::getLastActiveTime, LocalDateTime.now())
                .set(JobWorkRunNodeLogPo::getCallBackTime, LocalDateTime.now())
                .eq(JobWorkRunNodeLogPo::getNodeLogId, logPo.getNodeLogId())
                .isNull(JobWorkRunNodeLogPo::getCallBackTime));
        long stoppingCount = jobWorkNodeMapper.selectCount(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, logPo.getWorkId())
                .eq(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.STOPPING.getCode()));
        if (stoppingCount == 0L) {
            jobWorkMapper.update(null, Wrappers.lambdaUpdate(JobWorkPo.class)
                    .set(JobWorkPo::getRunWorkStatus, FlowRunStatusEnum.STOPPED.getCode())
                    .eq(JobWorkPo::getWorkId, logPo.getWorkId())
                    .eq(JobWorkPo::getRunWorkStatus, FlowRunStatusEnum.STOPPING.getCode()));
        }
    }

    /**
     * 得到未闭环的日志
     */
    @Override
    public List<JobWorkRunNodeLogPo> getUnclosedRunNodeLogs(String workId, List<JobWorkNodePo> runNodeList) {
        if (CollUtil.isEmpty(runNodeList)) {
            return Collections.emptyList();
        }
        List<JobWorkRunNodeLogPo> result = new ArrayList<>();
        for (JobWorkNodePo runNode : runNodeList) {
            JobWorkRunNodeLogPo latest = jobWorkRunNodeLogMapper.selectOne(Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                    .eq(JobWorkRunNodeLogPo::getWorkId, runNode.getWorkId())
                    .eq(JobWorkRunNodeLogPo::getNodeId, runNode.getNodeId())
                    .eq(runNode.getTurnDate() != null, JobWorkRunNodeLogPo::getTurnDate, runNode.getTurnDate())
                    .isNull(JobWorkRunNodeLogPo::getCallBackTime)
                    .orderByDesc(JobWorkRunNodeLogPo::getCreateTime).last("LIMIT 1"));
            if (latest != null) {
                latest.setWorkId(workId);
                latest.setNodeId(runNode.getNodeId());
                result.add(latest);
            }
        }
        return result;
    }

    /**
     * 重置节点状态
     *
     * @param node     节点
     * @param turnDate 翻牌日期
     * @return 更新数量
     */
    private int resetNode(JobWorkNodePo node, Date turnDate) {
        return jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                .set(JobWorkNodePo::getTurnDate, turnDate)
                .set(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode())
                .set(JobWorkNodePo::getCurrentRetryTimes, node.getRetryTimes())
                .eq(JobWorkNodePo::getNodeId, node.getNodeId())
                .eq(JobWorkNodePo::getNodeRunStatus, node.getNodeRunStatus())
                .notIn(JobWorkNodePo::getNodeRunStatus,
                        FlowRunStatusEnum.DISPATCHED.getCode(), FlowRunStatusEnum.RUNNING.getCode()));
    }

    /**
     * 重置作业状态
     *
     * @param workId 作业ID
     */
    private void resetRunWorkStatus(String workId) {
        jobWorkMapper.update(null, Wrappers.lambdaUpdate(JobWorkPo.class)
                .set(JobWorkPo::getRunWorkStatus, FlowRunStatusEnum.WAIT.getCode())
                .eq(JobWorkPo::getWorkId, workId));
    }

    /**
     * 获取当前作业
     *
     * @param workId 作业ID
     * @return 作业
     */
    private JobWorkPo getCurrentWork(String workId) {
        JobWorkPo work = jobWorkMapper.selectById(workId);
        return work != null && work.getRunWorkStatus() != null ? work : null;
    }

    /**
     * 是否是可重试状态
     * @param status 状态
     * @return 是否是可重试状态
     */
    private boolean isRerunnableStatus(Integer status) {
        return Objects.equals(status, FlowRunStatusEnum.COMPLETE.getCode())
                || Objects.equals(status, FlowRunStatusEnum.SKIPPED.getCode())
                || Objects.equals(status, FlowRunStatusEnum.EXCEPTION.getCode())
                || Objects.equals(status, FlowRunStatusEnum.STOPPED.getCode())
                || Objects.equals(status, FlowRunStatusEnum.UNINITIALIZED.getCode())
                || Objects.equals(status, FlowRunStatusEnum.WAIT.getCode());
    }

    /**
     * 是否是未初始化状态
     * @param node 节点
     * @return 是否是未初始化状态
     */
    private boolean isUninitialized(JobWorkNodePo node) {
        return node != null && Objects.equals(node.getNodeRunStatus(),
                FlowRunStatusEnum.UNINITIALIZED.getCode());
    }

    /**
     * 节点权利是否在客户端
     */
    private boolean isActiveNode(JobWorkNodePo node) {
        return node != null && (Objects.equals(node.getNodeRunStatus(), FlowRunStatusEnum.DISPATCHED.getCode())
                || Objects.equals(node.getNodeRunStatus(), FlowRunStatusEnum.RUNNING.getCode())
                || Objects.equals(node.getNodeRunStatus(), FlowRunStatusEnum.STOPPING.getCode()));
    }

    /**
     * 获取依赖节点ID
     *
     * @param nodeId       节点ID
     * @param nodes        节点列表
     * @param dependencies 依赖列表
     * @return 依赖节点ID
     */
    private Set<String> resolveDependentNodeIds(String nodeId, List<JobWorkNodePo> nodes,
                                                List<JobWorkFlowDependencyPo> dependencies) {
        Map<String, List<JobWorkNodePo>> groupNodes = nodes.stream().filter(node -> StrUtil.isNotBlank(node.getGroupId()))
                .collect(Collectors.groupingBy(JobWorkNodePo::getGroupId));
        Set<String> result = new LinkedHashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        result.add(nodeId);
        pending.add(nodeId);
        while (!pending.isEmpty()) {
            String predecessorNodeId = pending.removeFirst();
            JobWorkNodePo predecessor = nodes.stream().filter(node -> predecessorNodeId.equals(node.getNodeId()))
                    .findFirst().orElse(null);
            if (predecessor == null) {
                continue;
            }
            for (JobWorkFlowDependencyPo dependency : dependencies) {
                // 当前节点与依赖的前置节点是否匹配
                if (!matchesPredecessor(dependency, predecessor)) {
                    continue;
                }
                // 这里为什么是一个list，因为依赖也有可能一个组
                for (String successorNodeId : resolveSuccessorNodeIds(dependency, groupNodes)) {
                    if (result.add(successorNodeId)) {
                        pending.addLast(successorNodeId);
                    }
                }
            }
        }
        return result;
    }

    /**
     * 是否匹配前置节点
     *
     * @param dependency 依赖
     * @param node       节点
     * @return 是否匹配
     */
    private boolean matchesPredecessor(JobWorkFlowDependencyPo dependency, JobWorkNodePo node) {
        // 当前节点与依赖的前置节点
        return (FlowElementTypeEnum.NODE.name().equals(dependency.getPredecessorType())
                && node.getNodeId().equals(dependency.getPredecessorId()))
                || (FlowElementTypeEnum.GROUP.name().equals(dependency.getPredecessorType())
                && dependency.getPredecessorId().equals(node.getGroupId()));
    }

    /**
     * 获取后置节点ID
     *
     * @param dependency 依赖
     * @param groupNodes 分组节点
     * @return 后置节点ID
     */
    private Set<String> resolveSuccessorNodeIds(JobWorkFlowDependencyPo dependency,
                                                Map<String, List<JobWorkNodePo>> groupNodes) {
        if (FlowElementTypeEnum.NODE.name().equals(dependency.getSuccessorType())) {
            return Collections.singleton(dependency.getSuccessorId());
        }
        if (!FlowElementTypeEnum.GROUP.name().equals(dependency.getSuccessorType())) {
            return Collections.emptySet();
        }
        return groupNodes.getOrDefault(dependency.getSuccessorId(), Collections.emptyList()).stream()
                .map(JobWorkNodePo::getNodeId).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 构建恢复阻塞消息
     *
     * @param logPo 运行节点日志
     * @param state 状态
     * @return 消息
     */
    private String buildRecoveryBlockedMessage(JobWorkRunNodeLogPo logPo, String state) {
        String stateText = StrUtil.blankToDefault(state, "UNKNOWN");
        if (RunNodeExecutionStateEnum.STOP_REQUESTED.getValue().equals(state)) {
            return "长任务已收到停止请求但尚未退出，请稍后再恢复，nodeId:" + logPo.getNodeId();
        }
        if (RunNodeExecutionStateEnum.RUNNING.getValue().equals(state)
                || RunNodeExecutionStateEnum.QUEUED.getValue().equals(state)) {
            return "执行器中仍存在旧任务，暂不能恢复，nodeId:" + logPo.getNodeId() + ", state:" + stateText;
        }
        return "执行器未返回旧任务的停止确认，请稍后重试，nodeId:" + logPo.getNodeId() + ", state:" + stateText;
    }

    /**
     * 关闭未关闭的运行节点日志
     *
     * @param runNodeList 运行节点列表
     * @param handleMsg   处理消息
     */
    private void closeUnclosedRunNodeLogs(List<JobWorkNodePo> runNodeList, String handleMsg) {
        for (JobWorkNodePo runNode : runNodeList) {
            JobWorkRunNodeLogPo latest = jobWorkRunNodeLogMapper.selectOne(Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                    .eq(JobWorkRunNodeLogPo::getWorkId, runNode.getWorkId())
                    .eq(JobWorkRunNodeLogPo::getNodeId, runNode.getNodeId())
                    .eq(runNode.getTurnDate() != null, JobWorkRunNodeLogPo::getTurnDate, runNode.getTurnDate())
                    .isNull(JobWorkRunNodeLogPo::getCallBackTime).orderByDesc(JobWorkRunNodeLogPo::getCreateTime).last("LIMIT 1"));
            if (latest != null) {
                jobWorkRunNodeLogMapper.update(null, Wrappers.lambdaUpdate(JobWorkRunNodeLogPo.class)
                        .set(JobWorkRunNodeLogPo::getHandleCode, HandleCodeConstant.HANDLE_CODE_FAIL)
                        .set(JobWorkRunNodeLogPo::getHandleMsg, handleMsg)
                        .set(JobWorkRunNodeLogPo::getLastActiveTime, LocalDateTime.now())
                        .set(JobWorkRunNodeLogPo::getCallBackTime, LocalDateTime.now())
                        .eq(JobWorkRunNodeLogPo::getNodeLogId, latest.getNodeLogId())
                        .isNull(JobWorkRunNodeLogPo::getCallBackTime));
            }
        }
    }
}
