package com.nbatch.job.admin.core.helper;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.*;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeVo;
import com.nbatch.job.admin.mapper.IJobWorkExportFileMapper;
import com.nbatch.job.admin.mapper.IJobWorkImportFileMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.core.biz.model.ExecuteDbToFileParam;
import com.nbatch.job.core.biz.model.ExecuteFileToDbParam;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.WorkTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.nbatch.job.core.enums.NodeTypeEnum.NODE_TYPE_DB_TO_FILE;
import static com.nbatch.job.core.enums.NodeTypeEnum.NODE_TYPE_FILE_TO_DB;

/**
 * 负责可下发节点筛选、下发参数组装及下发状态处理。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RunNodeDispatchHelper {

    private static final String DISPATCHED_HANDLE_MSG = "运行节点已下发";

    private final IJobWorkNodeMapper jobWorkNodeMapper;
    private final IJobWorkExportFileMapper jobWorkExportFileMapper;
    private final IJobWorkImportFileMapper jobWorkImportFileMapper;
    private final FlowDependencyResolver flowDependencyResolver;
    private final RunNodeLogHelper runNodeLogHelper;
    private final RunNodeStateHelper runNodeStateHelper;

    /**
     * 创建下发日志，并把节点从等待状态更新为已下发。
     */
    public void markDispatched(ExecuteWorkParam executeWorkParam) {
        updateNodeStatus(executeWorkParam, FlowRunStatusEnum.DISPATCHED.getCode());
    }

    /**
     * 按批次日期、重试退避和流程依赖筛选可下发节点。
     */
    public ExecuteWorkParam findEnabled(JobWorkPo work) {
        ExecuteWorkParam executeWorkParam = buildExecuteWorkParam(work);
        if (executeWorkParam == null || CollUtil.isEmpty(executeWorkParam.getExecuteNodeParamList())) {
            return null;
        }
        Date turnDate = executeWorkParam.getTurnDate();
        // 根据依赖关系分析节点状态
        Map<String, FlowDependencyResolver.DependencyState> dependencyStates =
                flowDependencyResolver.resolveNodeStates(work.getWorkId(),
                        toWorkNodeList(executeWorkParam.getExecuteNodeParamList()));
        // 依赖解析器统一判断节点和流程组依赖，避免下发层重复实现依赖规则。
        List<ExecuteNodeParam> enabledNodes = executeWorkParam.getExecuteNodeParamList().stream()
                .filter(node -> Objects.equals(node.getNodeRunStatus(), FlowRunStatusEnum.WAIT.getCode()))
                .filter(node -> runNodeStateHelper.isReadyToDispatch(node.getNodeId()))
                .filter(node -> isNodeTurnDateMatched(node, turnDate, executeWorkParam.getWorkType()))
                .filter(node -> dependencyStates.getOrDefault(node.getNodeId(),
                        FlowDependencyResolver.DependencyState.READY)
                        == FlowDependencyResolver.DependencyState.READY)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(enabledNodes)) {
            return null;
        }
        enabledNodes.sort(Comparator.comparing(ExecuteNodeParam::getTurnDate,
                Comparator.nullsFirst(Date::compareTo)));
        Date dispatchTurnDate = enabledNodes.get(0).getTurnDate();
        executeWorkParam.setTurnDate(dispatchTurnDate == null ? turnDate : dispatchTurnDate);
        executeWorkParam.setExecuteNodeParamList(enabledNodes.stream()
                .filter(node -> isSameTurnDate(node.getTurnDate(), dispatchTurnDate))
                .collect(Collectors.toList()));
        return executeWorkParam;
    }

    /**
     * 下发结果未知时保持节点为已下发，等待执行器后续事件。
     */
    public void markDispatchUnknown(ExecuteWorkParam executeWorkParam, String handleMsg) {
        if (executeWorkParam == null || CollUtil.isEmpty(executeWorkParam.getExecuteNodeParamList())) {
            return;
        }
        createDispatchLogs(executeWorkParam, StrUtil.blankToDefault(handleMsg, "运行节点下发结果未知，等待执行器确认"));
        List<String> nodeIds = nodeIds(executeWorkParam);
        jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                .set(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.DISPATCHED.getCode())
                .in(JobWorkNodePo::getNodeId, nodeIds)
                .in(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode(),
                        FlowRunStatusEnum.DISPATCHED.getCode()));
        runNodeStateHelper.transitionWorkStatus(executeWorkParam.getWorkId(), FlowRunStatusEnum.DISPATCHED.getCode());
    }

    /**
     * 下发失败时关闭对应日志，并交给状态组件执行失败策略。
     */
    public void markDispatchFailed(ExecuteWorkParam executeWorkParam, String handleMsg) {
        if (executeWorkParam == null || CollUtil.isEmpty(executeWorkParam.getExecuteNodeParamList())) {
            return;
        }
        List<String> nodeIds = nodeIds(executeWorkParam);
        for (ExecuteNodeParam node : executeWorkParam.getExecuteNodeParamList()) {
            JobWorkRunNodeLogPo oldLog = runNodeLogHelper.get(node.getNodeLogId());
            if (oldLog == null) {
                runNodeLogHelper.create(node, executeWorkParam.getExecutorAddress(),
                        HandleCodeConstant.HANDLE_CODE_FAIL, handleMsg, LocalDateTime.now());
            } else {
                runNodeLogHelper.finish(node.getNodeLogId(), HandleCodeConstant.HANDLE_CODE_FAIL, handleMsg);
            }
        }
        // 只处理仍处于等待或已下发的节点，避免覆盖已经被事件线程更新的状态。
        List<JobWorkNodePo> nodes = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .in(JobWorkNodePo::getNodeId, nodeIds)
                .in(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode(),
                        FlowRunStatusEnum.DISPATCHED.getCode()));
        // 处理失败节点
        nodes.forEach(runNodeStateHelper::handleFailure);
    }

    private List<JobWorkNodePo> toWorkNodeList(List<ExecuteNodeParam> nodes) {
        if (CollUtil.isEmpty(nodes)) {
            return Collections.emptyList();
        }
        return nodes.stream().map(node -> {
            JobWorkNodePo workNode = new JobWorkNodePo();
            workNode.setNodeId(node.getNodeId());
            workNode.setGroupId(node.getGroupId());
            workNode.setNodeRunStatus(node.getNodeRunStatus());
            workNode.setTurnDate(node.getTurnDate());
            return workNode;
        }).collect(Collectors.toList());
    }

    /**
     * 将作业节点配置和文件配置组装成执行器所需参数。
     */
    private ExecuteWorkParam buildExecuteWorkParam(JobWorkPo work) {
        if (work == null) {
            return null;
        }
        List<JobWorkNodePo> nodes = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, work.getWorkId()));
        if (CollUtil.isEmpty(nodes)) {
            log.info("作业不存在可执行节点，workId：{}", work.getWorkId());
            return null;
        }
        Map<String, JobWorkNodeVo> nodeMap = nodes.stream().collect(Collectors.toMap(JobWorkNodePo::getNodeId,
                node -> BeanUtil.toBean(node, JobWorkNodeVo.class)));
        Map<String, JobWorkImportFilePo> importFiles = toImportFileMap(nodeMap);
        Map<String, JobWorkExportFilePo> exportFiles = toExportFileMap(nodeMap);
        ExecuteWorkParam result = new ExecuteWorkParam().setWorkId(work.getWorkId()).setTurnDate(work.getTurnDate())
                .setWorkType(work.getWorkType());
        result.setExecuteNodeParamList(nodes.stream().map(node -> buildNodeParam(work, node, nodeMap.get(node.getNodeId()),
                importFiles, exportFiles)).collect(Collectors.toList()));
        return result;
    }

    /**
     * 得到导入文件Map。
     */
    private Map<String, JobWorkImportFilePo> toImportFileMap(Map<String, JobWorkNodeVo> nodeMap) {
        List<JobWorkImportFilePo> files = jobWorkImportFileMapper.selectList(Wrappers.lambdaQuery(JobWorkImportFilePo.class)
                .in(JobWorkImportFilePo::getNodeId, nodeMap.keySet()));
        return CollUtil.isEmpty(files) ? Collections.emptyMap() : files.stream()
                                                                  .collect(Collectors.toMap(JobWorkImportFilePo::getNodeId, file -> file));
    }

    /**
     * 得到导出文件Map。
     */
    private Map<String, JobWorkExportFilePo> toExportFileMap(Map<String, JobWorkNodeVo> nodeMap) {
        List<JobWorkExportFilePo> files = jobWorkExportFileMapper.selectList(Wrappers.lambdaQuery(JobWorkExportFilePo.class)
                .in(JobWorkExportFilePo::getNodeId, nodeMap.keySet()));
        return CollUtil.isEmpty(files) ? Collections.emptyMap() : files.stream()
                                                                  .collect(Collectors.toMap(JobWorkExportFilePo::getNodeId, file -> file));
    }

    private ExecuteNodeParam buildNodeParam(JobWorkPo work, JobWorkNodePo node, JobWorkNodeVo config,
                                            Map<String, JobWorkImportFilePo> importFiles,
                                            Map<String, JobWorkExportFilePo> exportFiles) {
        ExecuteNodeParam result = BeanUtil.toBean(node, ExecuteNodeParam.class);
        result.setWorkId(work.getWorkId());
        result.setNodeId(node.getNodeId());
        result.setNodeType(config.getNodeType());
        result.setDbType(config.getDbType());
        result.setExecuteContent(config.getExecuteContent());
        result.setExecuteContentParam(config.getExecuteContentParam());
        result.setExecuteHandler(config.getExecuteHandler());
        result.setScriptType(config.getScriptType());
        result.setUpdateTime(config.getUpdateTime());
        result.setNodeRunStatus(node.getNodeRunStatus());
        if (StrUtil.equals(config.getNodeType(), NODE_TYPE_FILE_TO_DB.getCode()) && importFiles.containsKey(node.getNodeId())) {
            result.setExecuteFileToDbParam(BeanUtil.toBean(importFiles.get(node.getNodeId()), ExecuteFileToDbParam.class));
        }
        if (StrUtil.equals(config.getNodeType(), NODE_TYPE_DB_TO_FILE.getCode()) && exportFiles.containsKey(node.getNodeId())) {
            result.setExecuteDbToFileParam(BeanUtil.toBean(exportFiles.get(node.getNodeId()), ExecuteDbToFileParam.class));
        }
        return result;
    }

    /**
     * 判断节点是否满足下发条件。
     *
     * @param node     节点
     * @param turnDate 作业下发时间
     * @param workType 作业类型
     * @return 是否满足下发条件
     */
    private boolean isNodeTurnDateMatched(ExecuteNodeParam node, Date turnDate, Integer workType) {
        if (Objects.equals(workType, WorkTypeEnum.TYPE_SEQUENCE.getCode())) {
            return true;
        }
        return workType == WorkTypeEnum.TYPE_TURN.getCode() && node.getTurnDate() != null
                && (turnDate == null || DateUtil.compare(node.getTurnDate(), turnDate) <= 0);
    }

    /**
     * 判断两个日期是否为同一天。
     *
     * @param left  日期1
     * @param right 日期2
     * @return 是否为同一天
     */
    private boolean isSameTurnDate(Date left, Date right) {
        return left == null || right == null ? left == right
                : DateUtil.compare(DateUtil.beginOfDay(left), DateUtil.beginOfDay(right)) == 0;
    }

    /**
     * 根据下发结果更新节点和所属作业状态。
     */
    private void updateNodeStatus(ExecuteWorkParam executeWorkParam, Integer status) {
        if (executeWorkParam == null || CollUtil.isEmpty(executeWorkParam.getExecuteNodeParamList())) {
            return;
        }
        List<String> nodeIds = nodeIds(executeWorkParam);
        // 下发状态为已下发时，只更新等待节点状态为已下发，避免覆盖已经被事件线程更新的状态。
        if (status == FlowRunStatusEnum.DISPATCHED.getCode()) {
            createDispatchLogs(executeWorkParam, DISPATCHED_HANDLE_MSG);
            jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                    .set(JobWorkNodePo::getNodeRunStatus, status).in(JobWorkNodePo::getNodeId, nodeIds)
                    .eq(JobWorkNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode()));
        } else {
            jobWorkNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkNodePo.class)
                    .set(JobWorkNodePo::getNodeRunStatus, status).in(JobWorkNodePo::getNodeId, nodeIds));
        }
        runNodeStateHelper.transitionWorkStatus(executeWorkParam.getWorkId(), status);
    }

    /**
     * 为下发创建日志；已有执行中的日志则复用，等待节点则关闭旧日志后新建。
     */
    private void createDispatchLogs(ExecuteWorkParam executeWorkParam, String handleMsg) {
        List<ExecuteNodeParam> nodes = executeWorkParam.getExecuteNodeParamList();
        List<String> nodeIds = nodes.stream().map(ExecuteNodeParam::getNodeId).collect(Collectors.toList());
        List<String> nodeLogIds = nodes.stream().map(ExecuteNodeParam::getNodeLogId)
                .filter(StrUtil::isNotBlank).collect(Collectors.toList());
        // 循环外批量查询，避免每个节点分别访问节点表和日志表。
        Map<String, JobWorkNodePo> currentNodeMap = jobWorkNodeMapper.selectList(
                        Wrappers.lambdaQuery(JobWorkNodePo.class)
                                .eq(JobWorkNodePo::getWorkId, executeWorkParam.getWorkId())
                                .in(JobWorkNodePo::getNodeId, nodeIds))
                .stream().collect(Collectors.toMap(JobWorkNodePo::getNodeId, node -> node,
                        (first, ignored) -> first));
        Map<String, JobWorkRunNodeLogPo> unclosedLogMap = new HashMap<>();
        List<JobWorkRunNodeLogPo> unclosedList = runNodeLogHelper.findUnclosed(
                executeWorkParam.getWorkId(), executeWorkParam.getTurnDate(), nodeIds);
        for (JobWorkRunNodeLogPo log : unclosedList) {
            // 查询结果按创建时间倒序，只保留每个节点最新的一条日志。
            unclosedLogMap.putIfAbsent(log.getNodeId(), log);
        }
        Map<String, JobWorkRunNodeLogPo> existingLogMap = runNodeLogHelper.getMap(nodeLogIds);
        for (ExecuteNodeParam node : nodes) {
            JobWorkRunNodeLogPo unclosedLog = unclosedLogMap.get(node.getNodeId());
            if (unclosedLog != null) {
                JobWorkNodePo currentNode = currentNodeMap.get(node.getNodeId());
                // 如果当前节点是等待状态，则关闭旧日志，否则刷新日志信息
                if (currentNode != null && Objects.equals(currentNode.getNodeRunStatus(),
                        FlowRunStatusEnum.WAIT.getCode())) {
                    runNodeLogHelper.closeUnclosed(unclosedLog.getNodeLogId(), "运行节点重新下发，关闭旧的未闭环日志");
                } else {
                    node.setNodeLogId(unclosedLog.getNodeLogId());
                    runNodeLogHelper.refreshActiveLogStatus(unclosedLog.getNodeLogId(), 0, handleMsg);
                    continue;
                }
            }
            // 补偿如果node_log_id为空，则创建日志
            if (!existingLogMap.containsKey(node.getNodeLogId())) {
                runNodeLogHelper.create(node, executeWorkParam.getExecutorAddress(), 0, handleMsg, null);
            }
        }
    }

    /**
     * 获取作业节点ID列表。
     */
    private List<String> nodeIds(ExecuteWorkParam executeWorkParam) {
        return executeWorkParam.getExecuteNodeParamList().stream().map(ExecuteNodeParam::getNodeId)
                .collect(Collectors.toList());
    }
}
