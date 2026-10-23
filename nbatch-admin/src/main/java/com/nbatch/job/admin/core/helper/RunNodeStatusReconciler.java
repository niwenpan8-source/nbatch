package com.nbatch.job.admin.core.helper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobRegistryPo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogPo;
import com.nbatch.job.admin.core.executor.ExecutorBizProxy;
import com.nbatch.job.admin.mapper.IJobRegistryMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.RunNodeStatusQueryParam;
import com.nbatch.job.core.biz.model.RunNodeStatusQueryResult;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.RegistryConfig;
import com.nbatch.job.core.enums.RunNodeExecutionStateEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 核对长时间未收到事件的节点，并根据执行器真实状态修正数据库记录。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RunNodeStatusReconciler {

    /**
     * 节点超过x分钟无事件后，开始向执行器查询真实状态
     */
    private static final int NODE_RECONCILE_TIMEOUT_MINUTES = 4;

    /**
     * 执行器未注册或状态查询不可用时，超过x分钟才允许累计离线确认并最终闭环
     */
    private static final int OFFLINE_PROTECT_MINUTES = 10;

    /**
     * 节点不存在确认次数
     */
    private static final int ABSENT_CONFIRM_TIMES = 3;

    /**
     * 执行器离线确认次数
     */
    private static final int OFFLINE_CONFIRM_TIMES = 3;

    private static final int STATUS_QUERY_RETRY_TIMES = 3;

    private static final long STATUS_QUERY_RETRY_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(1);

    private final IJobWorkNodeMapper jobWorkNodeMapper;

    private final IJobRegistryMapper jobRegistryMapper;

    private final RunNodeLogHelper runNodeLogHelper;

    private final RunNodeStateHelper runNodeStateHelper;

    private final RunNodeStopCommandHelper runNodeStopCommandHelper;

    /**
     * 节点缺席次数
     */
    private final ConcurrentMap<String, Integer> absentCountMap = new ConcurrentHashMap<>();

    /**
     * 执行器离线确认次数
     */
    private final ConcurrentMap<String, Integer> offlineCountMap = new ConcurrentHashMap<>();

    /**
     * 核对超时节点，按执行器地址分组后批量核对真实运行状态。
     */
    public void reconcileTimeoutNodes() {
        // 停止中的节点必须持续核对，直到执行器明确确认任务已经退出。
        List<JobWorkNodePo> nodes = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .in(JobWorkNodePo::getNodeRunStatus,
                        FlowRunStatusEnum.DISPATCHED.getCode(),
                        FlowRunStatusEnum.RUNNING.getCode(),
                        FlowRunStatusEnum.STOPPING.getCode()));
        if (CollUtil.isEmpty(nodes)) {
            return;
        }
        Map<String, List<Candidate>> candidatesByAddress = new HashMap<>();
        Map<String, JobWorkRunNodeLogPo> unclosedMap = runNodeLogHelper.findUnclosedMap(nodes);
        LocalDateTime now = LocalDateTime.now();
        for (JobWorkNodePo node : nodes) {
            // 得到所有未闭环的日志
            String nodeKey = RunNodeLogHelper.buildWorkNodeTurnKey(
                    node.getWorkId(), node.getNodeId(), node.getTurnDate());
            JobWorkRunNodeLogPo logPo = unclosedMap.get(nodeKey);
            // 如果说节点超过x分钟没有收到事件则进入候选节点向客户端询问其状态
            if (logPo != null && needsReconcile(node, logPo, now) && StrUtil.isNotBlank(logPo.getExecutorAddress())) {
                candidatesByAddress.computeIfAbsent(logPo.getExecutorAddress(), key -> new ArrayList<>())
                        .add(new Candidate(node, logPo));
            }
        }
        // 同一执行器的节点合并查询，减少管理端到执行器的 RPC 次数。
        Set<String> activeAddresses = getActiveExecutorAddresses();
        for (Map.Entry<String, List<Candidate>> entry : candidatesByAddress.entrySet()) {
            reconcileExecutor(entry.getKey(), entry.getValue(), activeAddresses.contains(entry.getKey().trim()));
        }
    }

    /**
     * 收到当前执行尝试事件后，清除离线和不存在确认次数。
     */
    public void clearConfirmCount(String nodeLogId) {
        if (StrUtil.isBlank(nodeLogId)) {
            return;
        }
        absentCountMap.remove(nodeLogId);
        offlineCountMap.remove(nodeLogId);
    }

    /**
     * 根据执行器实时状态修正节点；停止请求未确认时批量补发停止命令。
     */
    private void reconcileExecutor(String executorAddress, List<Candidate> candidates, boolean registered) {
        // 对无效的候选节点进行过滤
        List<Candidate> validCandidates = candidates.stream().filter(Objects::nonNull)
                .filter(candidate -> candidate.node != null && candidate.log != null)
                .filter(candidate -> StrUtil.isNotBlank(candidate.log.getNodeLogId()))
                .collect(Collectors.toList());
        // 如果执行器地址为空或没有有效候选节点，则清除离线和不存在确认次数。
        if (StrUtil.isBlank(executorAddress) || CollUtil.isEmpty(validCandidates)) {
            clearAbsentCount(candidates);
            return;
        }
        List<String> nodeLogIds = validCandidates.stream().map(candidate -> candidate.log.getNodeLogId())
                .distinct().collect(Collectors.toList());
        // 根据日志的 ids 查询节点执行的状态
        ReturnT<RunNodeStatusQueryResult> result = queryStatus(executorAddress,
                new RunNodeStatusQueryParam().setNodeLogIdList(nodeLogIds));
        // 判断下发的节点是否完整,如果不完整则代表请求有问题或者返回的结果有问题，执行判断里面的内容
        if (!isCompleteStatusResult(result, nodeLogIds)) {
            clearAbsentCount(validCandidates);
            // 如果已经注册，代表通信没有问题，这里需要对停止中的节点补发停止命令
            if (registered) {
                resendStopCommands(validCandidates.stream()
                        .filter(candidate -> isStopping(candidate.node))
                        .map(candidate -> candidate.log).collect(Collectors.toList()));
            }
            handleUnavailableExecutor(executorAddress, validCandidates, registered);
            return;
        }
        // 如果判断通过清理离线次数
        clearOfflineCount(validCandidates);
        RunNodeStatusQueryResult statusResult = result.getContent();
        List<JobWorkRunNodeLogPo> retryStopLogs = new ArrayList<>();
        // 会根据状态完成对应的业务
        for (Candidate candidate : validCandidates) {
            String nodeLogId = candidate.log.getNodeLogId();
            String state = statusResult.getNodeStateMap().get(nodeLogId);
            // 执行器明确返回的状态优先于管理端的超时推断。
            if (RunNodeExecutionStateEnum.RUNNING.getValue().equals(state)) {
                if (isStopping(candidate.node)) {
                    runNodeLogHelper.refreshActiveLogStatus(nodeLogId, 0,
                            "执行器仍在运行，停止请求尚未确认");
                    retryStopLogs.add(candidate.log);
                } else {
                    markRunning(candidate);
                }
                absentCountMap.remove(nodeLogId);
            } else if (RunNodeExecutionStateEnum.STOPPED.getValue().equals(state)) {
                markStopped(candidate);
                clearConfirmCount(nodeLogId);
            } else if (RunNodeExecutionStateEnum.QUEUED.getValue().equals(state)) {
                if (isStopping(candidate.node)) {
                    runNodeLogHelper.refreshActiveLogStatus(nodeLogId, 0,
                            "执行器任务仍在排队，停止请求尚未确认");
                    retryStopLogs.add(candidate.log);
                } else {
                    runNodeLogHelper.refreshActiveLogStatus(nodeLogId, 0, "执行器状态确认：QUEUED");
                }
                absentCountMap.remove(nodeLogId);
            } else if (RunNodeExecutionStateEnum.STOP_REQUESTED.getValue().equals(state)) {
                runNodeLogHelper.refreshActiveLogStatus(nodeLogId, 0,
                        "执行器已确认停止请求，等待任务退出");
                absentCountMap.remove(nodeLogId);
            } else if (RunNodeExecutionStateEnum.ABSENT.getValue().equals(state)) {
                if (isStopping(candidate.node)) {
                    runNodeLogHelper.refreshActiveLogStatus(nodeLogId, 0,
                            "执行器未找到任务且没有停止确认，正在补发停止请求");
                    retryStopLogs.add(candidate.log);
                    continue;
                }
                // 连续确认用于容忍下发请求与状态查询之间的短暂乱序。
                int count = absentCountMap.merge(nodeLogId, 1, Integer::sum);
                if (count >= ABSENT_CONFIRM_TIMES) {
                    closeAsFailed(candidate, HandleCodeConstant.HANDLE_CODE_TIMEOUT,
                            "执行器连续确认运行节点不存在，未完成闭环");
                    absentCountMap.remove(nodeLogId);
                }
            } else {
                absentCountMap.remove(nodeLogId);
            }
        }
        resendStopCommands(retryStopLogs);
    }

    /**
     * 同一轮核对中按执行器和线程池合并补发，避免逐节点远程调用。
     */
    private void resendStopCommands(List<JobWorkRunNodeLogPo> retryStopLogs) {
        if (CollUtil.isEmpty(retryStopLogs)) {
            return;
        }
        List<JobWorkRunNodeLogPo> distinctLogs = new ArrayList<>(retryStopLogs.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(JobWorkRunNodeLogPo::getNodeLogId, log -> log,
                        (first, ignored) -> first, LinkedHashMap::new)).values());
        ReturnT<String> result = runNodeStopCommandHelper.send(distinctLogs);
        if (result == null || !result.isSuccess()) {
            log.warn("停止中节点补发停止命令失败：{}", result == null ? "无响应" : result.getMsg());
        }
    }

    /**
     * 执行器未注册或查询失败时，连续确认到达阀值后强制关闭。
     */
    private void handleUnavailableExecutor(String executorAddress, List<Candidate> candidates, boolean registered) {
        if (registered) {
            clearOfflineCount(candidates);
            return;
        }
        for (Candidate candidate : candidates) {
            String nodeLogId = candidate.log.getNodeLogId();
            if (isStopping(candidate.node)) {
                offlineCountMap.remove(nodeLogId);
                continue;
            }
            // 判断是否失效
            if (!isOfflineProtectionExpired(candidate.log)) {
                offlineCountMap.remove(nodeLogId);
                continue;
            }
            // 合并得到离线确认次数
            int count = offlineCountMap.merge(nodeLogId, 1, Integer::sum);
            if (count < OFFLINE_CONFIRM_TIMES) {
                continue;
            }
            forceCloseOffline(candidate, executorAddress);
            clearConfirmCount(nodeLogId);
        }
    }

    /**
     * 执行器明确返回节点不存在时，按正常失败策略处理节点。
     */
    private void closeAsFailed(Candidate candidate, Integer handleCode, String handleMsg) {
        // 如果没有未闭环的日志直接结束
        if (!runNodeLogHelper.isCurrentUnclosed(candidate.node.getNodeId(), candidate.log.getNodeLogId())) {
            return;
        }
        JobWorkNodePo currentNode = jobWorkNodeMapper.selectById(candidate.node.getNodeId());
        if (!isActive(currentNode)) {
            return;
        }
        if (runNodeStateHelper.stopIfRequested(currentNode)) {
            runNodeLogHelper.finish(candidate.log.getNodeLogId(), HandleCodeConstant.HANDLE_CODE_FAIL, "运行节点已停止");
            return;
        }
        runNodeStateHelper.handleFailure(currentNode);
        runNodeLogHelper.finish(candidate.log.getNodeLogId(), handleCode, handleMsg);
    }

    /**
     * 执行器持续离线且超过保护阈值时，强制结束节点并关闭日志。
     */
    private void forceCloseOffline(Candidate candidate, String executorAddress) {
        // 如果没有未闭环的日志直接结束
        if (!runNodeLogHelper.isCurrentUnclosed(candidate.node.getNodeId(), candidate.log.getNodeLogId())) {
            return;
        }
        JobWorkNodePo currentNode = jobWorkNodeMapper.selectById(candidate.node.getNodeId());
        if (!isActive(currentNode)) {
            return;
        }
        // 候选快照生成后可能收到停止请求，停止中的节点只能由执行器明确确认退出。
        if (isStopping(currentNode)) {
            return;
        }
        // 停止作业以及节点状态
        if (runNodeStateHelper.stopIfRequested(currentNode)) {
            // 结束日志
            runNodeLogHelper.finish(candidate.log.getNodeLogId(), HandleCodeConstant.HANDLE_CODE_FAIL, "运行节点已停止");
            return;
        }
        // 否则将日志标记为异常
        if (!runNodeStateHelper.markException(currentNode.getNodeId(), currentNode.getWorkId())) {
            return;
        }
        runNodeLogHelper.finish(candidate.log.getNodeLogId(), HandleCodeConstant.HANDLE_CODE_TIMEOUT,
                "执行器持续离线，运行结果无法确认，系统强制关闭本次执行日志");
        log.error("执行器不可用后强制关闭运行节点，执行器：{}，nodeId：{}，nodeLogId：{}",
                executorAddress, currentNode.getNodeId(), candidate.log.getNodeLogId());
    }

    /**
     * 停止中的节点只有在执行器明确确认退出后才可进入已停止。
     */
    private void markStopped(Candidate candidate) {
        int updated = jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                .set(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.STOPPED.getCode())
                .eq(JobWorkNodePo::getNodeId, candidate.node.getNodeId())
                .eq(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.STOPPING.getCode()));
        if (updated != 1) {
            return;
        }
        runNodeLogHelper.finish(candidate.log.getNodeLogId(), HandleCodeConstant.HANDLE_CODE_FAIL,
                "执行器状态确认：STOPPED");
        runNodeStateHelper.transitionWorkStatus(candidate.node.getWorkId(), FlowRunStatusEnum.STOPPED.getCode());
    }

    private void markRunning(Candidate candidate) {
        jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                .set(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.RUNNING.getCode())
                .eq(JobWorkNodePo::getNodeId, candidate.node.getNodeId())
                .in(JobWorkNodePo::getNodeRunStatus,
                        FlowRunStatusEnum.DISPATCHED.getCode(), FlowRunStatusEnum.RUNNING.getCode()));
        runNodeLogHelper.refreshActiveLogStatus(candidate.log.getNodeLogId(), 0, "执行器状态确认：RUNNING");
        runNodeStateHelper.transitionWorkStatus(candidate.node.getWorkId(), FlowRunStatusEnum.RUNNING.getCode());
    }

    /**
     * 判断已下发或运行中节点是否超过统一的无事件核对时间。
     */
    private boolean needsReconcile(JobWorkNodePo node, JobWorkRunNodeLogPo logPo, LocalDateTime now) {
        if (!isActive(node) || now == null) {
            return false;
        }
        // 得到节点最新心跳时间
        LocalDateTime activeTime = getActiveTime(logPo);
        // 核对时间 ==> 如果节点处于停止中或者活跃时间在询问状态时间之前,返回true（已经过期）
        return isStopping(node) || (activeTime != null
                && !activeTime.isAfter(now.minusMinutes(NODE_RECONCILE_TIMEOUT_MINUTES)));
    }

    /**
     * 判断是否超处理节点最大失效时间
     */
    private boolean isOfflineProtectionExpired(JobWorkRunNodeLogPo logPo) {
        LocalDateTime activeTime = getActiveTime(logPo);
        // 核对时间 ==> 如果节点处于停止中或者活跃时间在强制闭环时间之前,返回true（已经过期）
        return activeTime != null && !activeTime.isAfter(LocalDateTime.now().minusMinutes(OFFLINE_PROTECT_MINUTES));
    }

    /**
     * 得到最后活跃时间，兼容历史日志缺少活跃时间的情况。
     */
    private LocalDateTime getActiveTime(JobWorkRunNodeLogPo logPo) {
        if (logPo == null) {
            return null;
        }
        return logPo.getLastActiveTime() == null ? logPo.getCreateTime() : logPo.getLastActiveTime();
    }

    /**
     * 判断节点是否已经向发送到客户端（还有一种可能，就是状态已经修改过了，但是还没有向客户端发送）
     */
    private boolean isActive(JobWorkNodePo node) {
        return node != null && (Objects.equals(node.getNodeRunStatus(), FlowRunStatusEnum.DISPATCHED.getCode())
                || Objects.equals(node.getNodeRunStatus(), FlowRunStatusEnum.RUNNING.getCode())
                || isStopping(node));
    }

    private boolean isStopping(JobWorkNodePo node) {
        return node != null && Objects.equals(node.getNodeRunStatus(), FlowRunStatusEnum.STOPPING.getCode());
    }

    private Set<String> getActiveExecutorAddresses() {
        List<JobRegistryPo> registryList = jobRegistryMapper.selectList(Wrappers.lambdaQuery(JobRegistryPo.class)
                .eq(JobRegistryPo::getRegistryGroup, RegistryConfig.RegistType.EXECUTOR.name())
                .ge(JobRegistryPo::getUpdateTime, DateUtil.offsetSecond(new Date(), -RegistryConfig.DEAD_TIMEOUT)));
        if (CollUtil.isEmpty(registryList)) {
            return Collections.emptySet();
        }
        Set<String> addresses = new HashSet<>();
        for (JobRegistryPo registry : registryList) {
            if (registry != null && StrUtil.isNotBlank(registry.getRegistryValue())) {
                for (String address : registry.getRegistryValue().split(",")) {
                    if (StrUtil.isNotBlank(address)) {
                        addresses.add(address.trim());
                    }
                }
            }
        }
        return addresses;
    }

    /**
     * 清理节点缺席次数
     */
    private void clearAbsentCount(List<Candidate> candidates) {
        for (Candidate candidate : candidates) {
            if (candidate != null && candidate.log != null) {
                absentCountMap.remove(candidate.log.getNodeLogId());
            }
        }
    }

    /**
     * 清理离线次数
     */
    private void clearOfflineCount(List<Candidate> candidates) {
        for (Candidate candidate : candidates) {
            if (candidate != null && candidate.log != null) {
                offlineCountMap.remove(candidate.log.getNodeLogId());
            }
        }
    }

    /**
     * 查询执行器节点状态，响应不完整时按固定间隔重试。
     */
    private ReturnT<RunNodeStatusQueryResult> queryStatus(String executorAddress,
                                                          RunNodeStatusQueryParam queryParam) {
        if (StrUtil.isBlank(executorAddress) || queryParam == null
                || CollUtil.isEmpty(queryParam.getNodeLogIdList())) {
            return null;
        }
        ReturnT<RunNodeStatusQueryResult> result = null;
        for (int attempt = 1; attempt <= STATUS_QUERY_RETRY_TIMES; attempt++) {
            if (attempt > 1 && !waitQueryRetryInterval()) {
                return result;
            }
            result = ExecutorBizProxy.queryRunNodeStatus(executorAddress, queryParam);
            if (isCompleteStatusResult(result, queryParam.getNodeLogIdList())) {
                return result;
            }
        }
        return result;
    }

    /**
     * 判断执行器节点状态查询响应是否完整。
     */
    private boolean isCompleteStatusResult(ReturnT<RunNodeStatusQueryResult> result,
                                           List<String> nodeLogIds) {
        return result != null && result.isSuccess() && result.getContent() != null
                && CollUtil.isNotEmpty(nodeLogIds)
                && CollUtil.isNotEmpty(result.getContent().getNodeStateMap())
                && result.getContent().getNodeStateMap().keySet().containsAll(nodeLogIds);
    }

    /**
     * 等待查询重试间隔。
     */
    private boolean waitQueryRetryInterval() {
        try {
            TimeUnit.MILLISECONDS.sleep(STATUS_QUERY_RETRY_INTERVAL_MILLIS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 候选快照
     */
    private static final class Candidate {
        private final JobWorkNodePo node;
        private final JobWorkRunNodeLogPo log;

        private Candidate(JobWorkNodePo node, JobWorkRunNodeLogPo log) {
            this.node = node;
            this.log = log;
        }
    }
}
