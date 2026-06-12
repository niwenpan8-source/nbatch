package com.nbatch.job.admin.core.helper;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobRegistryPo;
import com.nbatch.job.admin.core.domain.po.JobWorkExportFilePo;
import com.nbatch.job.admin.core.domain.po.JobWorkImportFilePo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodeRelationPo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunPo;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeVo;
import com.nbatch.job.admin.mapper.IJobRegistryMapper;
import com.nbatch.job.admin.mapper.IJobWorkExportFileMapper;
import com.nbatch.job.admin.mapper.IJobWorkImportFileMapper;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeRelationMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeLogMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeMapper;
import com.nbatch.job.core.biz.model.ExecuteDbToFileParam;
import com.nbatch.job.core.biz.model.ExecuteFileToDbParam;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.biz.model.RunNodeLogEventParam;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.RegistryConfig;
import com.nbatch.job.core.enums.WorkTypeEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.nbatch.job.core.enums.NodeTypeEnum.NODE_TYPE_DB_TO_FILE;
import static com.nbatch.job.core.enums.NodeTypeEnum.NODE_TYPE_FILE_TO_DB;

/**
 * @description: 作业运行节点帮助类
 * @author: Mr.ni
 * @date: 2025/11/20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RunNodeHelper {

    private static final String DISPATCHED_HANDLE_MSG = "运行节点已下发";

    private static final int DISPATCHED_CONFIRM_TIMEOUT_HOURS = 1;

    private static final int RUNNING_CLOSE_TIMEOUT_HOURS = 6;

    private final IJobWorkRunNodeMapper jobWorkRunNodeMapper;

    private final IJobWorkNodeMapper jobWorkNodeMapper;

    private final IJobWorkNodeRelationMapper jobWorkNodeRelationMapper;

    private final IJobWorkRunMapper jobRunWorkMapper;

    private final IJobWorkExportFileMapper jobWorkExportFileMapper;

    private final IJobWorkImportFileMapper jobWorkImportFileMapper;

    private final IJobWorkRunNodeLogMapper jobWorkRunNodeLogMapper;

    private final IJobWorkMapper jobWorkMapper;

    private final IJobRegistryMapper jobRegistryMapper;

    public void handleNodeStatus(NodeStatusContext context) {
        if (context == null || context.getStrategy() == null) {
            return;
        }
        context.getStrategy().handle(this, context);
    }

    public enum NodeStatusHandleStrategy {

        /**
         * 运行状态
         */
        RUN_STATUS {
            @Override
            void handle(RunNodeHelper helper, NodeStatusContext context) {
                helper.updateNodeRunStatus(context.getExecuteWorkParam(), context.getNodeStatus());
            }
        },

        /**
         * 完成
         */
        COMPLETE {
            @Override
            void handle(RunNodeHelper helper, NodeStatusContext context) {
                helper.updateNodeTurnDate(context.getRunNodeId(), context.getRunWorkId(), context.getWorkType());
            }
        },

        /**
         * 重试失败
         */
        RETRY_FAIL {
            @Override
            void handle(RunNodeHelper helper, NodeStatusContext context) {
                helper.updateNodeStatusWithRetry(context.getRunNodeId());
            }
        };

        abstract void handle(RunNodeHelper helper, NodeStatusContext context);
    }

    @Getter
    public static class NodeStatusContext {
        private NodeStatusHandleStrategy strategy;
        private ExecuteWorkParam executeWorkParam;
        private Integer nodeStatus;
        private String runNodeId;
        private String runWorkId;
        private Integer workType;

        /**
         * 等待中
         */
        public static NodeStatusContext waiting(ExecuteWorkParam executeWorkParam) {
            return runStatus(executeWorkParam, FlowRunStatusEnum.WAIT.getCode());
        }

        /**
         * 已下发
         */
        public static NodeStatusContext dispatched(ExecuteWorkParam executeWorkParam) {
            return runStatus(executeWorkParam, FlowRunStatusEnum.DISPATCHED.getCode());
        }

        /**
         * 运行状态
         */
        public static NodeStatusContext runStatus(ExecuteWorkParam executeWorkParam, Integer nodeStatus) {
            NodeStatusContext context = new NodeStatusContext();
            context.strategy = NodeStatusHandleStrategy.RUN_STATUS;
            context.executeWorkParam = executeWorkParam;
            context.nodeStatus = nodeStatus;
            return context;
        }

        /**
         * 完成
         */
        public static NodeStatusContext complete(String runNodeId, String runWorkId, Integer workType) {
            NodeStatusContext context = new NodeStatusContext();
            context.strategy = NodeStatusHandleStrategy.COMPLETE;
            context.runNodeId = runNodeId;
            context.runWorkId = runWorkId;
            context.workType = workType;
            return context;
        }

        /**
         * 重试失败
         */
        public static NodeStatusContext retryFail(String runNodeId) {
            NodeStatusContext context = new NodeStatusContext();
            context.strategy = NodeStatusHandleStrategy.RETRY_FAIL;
            context.runNodeId = runNodeId;
            return context;
        }

    }

    /**
     * 获取当前运行作业中依赖已满足、可继续下发执行的节点
     *
     * @param jobRunWorkPo 运行作业
     */
    public ExecuteWorkParam getEnableExecuteWork(JobWorkRunPo jobRunWorkPo) {
        ExecuteWorkParam executeWorkParam = buildExecuteWorkParam(jobRunWorkPo);
        if (executeWorkParam == null || CollUtil.isEmpty(executeWorkParam.getExecuteNodeParamList())) {
            return null;
        }

        Date turnDate = executeWorkParam.getTurnDate();
        Integer workType = executeWorkParam.getWorkType();
        List<String> completeNodeIdList = getExecuteCompleteNodeIdByTypeList(
                executeWorkParam.getExecuteNodeParamList(), turnDate, workType);
        Set<String> completeNodeIdSet = CollUtil.isEmpty(completeNodeIdList)
                ? Collections.emptySet()
                : new HashSet<>(completeNodeIdList);

        List<ExecuteNodeParam> enableExecuteNodeList = executeWorkParam.getExecuteNodeParamList().stream()
                .filter(x -> x.getNodeRunStatus() == FlowRunStatusEnum.WAIT.getCode())
                .filter(x -> isNodeTurnDateMatched(x, turnDate, workType))
                .filter(x -> {
                    if (CollUtil.isEmpty(x.getNodeRelationIdList())) {
                        return true;
                    }
                    if (completeNodeIdSet.isEmpty()) {
                        return false;
                    }
                    return completeNodeIdSet.containsAll(x.getNodeRelationIdList());
                }).collect(Collectors.toList());

        if (CollUtil.isEmpty(enableExecuteNodeList)) {
            return null;
        }
        executeWorkParam.setExecuteNodeParamList(enableExecuteNodeList);
        return executeWorkParam;
    }

    /**
     * 构建执行作业参数
     *
     * @param jobRunWorkPo 运行作业
     */
    private ExecuteWorkParam buildExecuteWorkParam(JobWorkRunPo jobRunWorkPo) {
        if (jobRunWorkPo == null) {
            return null;
        }

        String workId = jobRunWorkPo.getWorkId();
        JobWorkPo jobWorkPo = jobWorkMapper.selectById(workId);
        if (jobWorkPo == null) {
            log.info("workId:{},作业不存在！", workId);
            return null;
        }

        ExecuteWorkParam executeWorkParam = new ExecuteWorkParam();
        executeWorkParam.setWorkId(workId);
        executeWorkParam.setRunWorkId(jobRunWorkPo.getRunWorkId());
        executeWorkParam.setTurnDate(jobRunWorkPo.getTurnDate());
        executeWorkParam.setWorkType(jobWorkPo.getWorkType());
        executeWorkParam.setContextJson(jobRunWorkPo.getContextJson());

        // 得到该次运行作业的所有运行节点
        List<JobWorkRunNodePo> jobWorkRunNodePos = jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                .eq(JobWorkRunNodePo::getRunWorkId, jobRunWorkPo.getRunWorkId()));
        if (CollUtil.isEmpty(jobWorkRunNodePos)) {
            log.info("workId:{},不存在运行作业节点！", workId);
            return null;
        }

        // 查找作业当中所有的节点
        List<JobWorkNodePo> workAllNodeList = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, workId));
        if (CollUtil.isEmpty(workAllNodeList)) {
            log.info("workId:{},不存在作业节点！", workId);
            return null;
        }

        Map<String, JobWorkNodeVo> jobWorkNodeMap = workAllNodeList.stream().collect(Collectors
                .toMap(JobWorkNodePo::getNodeId, x -> BeanUtil.toBean(x, JobWorkNodeVo.class)));

        List<JobWorkNodeRelationPo> jobWorkNodeRelationPos = jobWorkNodeRelationMapper.selectList(Wrappers.lambdaQuery(JobWorkNodeRelationPo.class)
                .eq(JobWorkNodeRelationPo::getWorkId, workId));

        Map<String, List<String>> relationMap = new HashMap<>();
        if (CollUtil.isNotEmpty(jobWorkNodeRelationPos)) {
            relationMap = jobWorkNodeRelationPos.stream()
                    .collect(Collectors.groupingBy(JobWorkNodeRelationPo::getNodeId1
                            , Collectors.mapping(JobWorkNodeRelationPo::getNodeId2, Collectors.toList())));
        }

        // 得到导入文件配置
        List<JobWorkImportFilePo> jobWorkImportFilePos = jobWorkImportFileMapper.selectList(Wrappers.lambdaQuery(JobWorkImportFilePo.class)
                .in(JobWorkImportFilePo::getNodeId, jobWorkNodeMap.keySet()));
        // 得到导出文件配置
        List<JobWorkExportFilePo> jobWorkExportFilePos = jobWorkExportFileMapper.selectList(Wrappers.lambdaQuery(JobWorkExportFilePo.class)
                .in(JobWorkExportFilePo::getNodeId, jobWorkNodeMap.keySet()));

        Map<String, JobWorkImportFilePo> jobWorkImportFileMap;
        Map<String, JobWorkExportFilePo> jobWorkExportFileMap;
        if (CollUtil.isNotEmpty(jobWorkImportFilePos)) {
            jobWorkImportFileMap = jobWorkImportFilePos.stream().collect(Collectors.toMap(JobWorkImportFilePo::getNodeId, x -> x));
        } else {
            jobWorkImportFileMap = null;
        }
        if (CollUtil.isNotEmpty(jobWorkExportFilePos)) {
            jobWorkExportFileMap = jobWorkExportFilePos.stream().collect(Collectors.toMap(JobWorkExportFilePo::getNodeId, x -> x));
        } else {
            jobWorkExportFileMap = null;
        }
        // 查找作业当中没有运行同时翻牌时间必须和作业时间相同的节点,对于节点只有运行状态为等待中的节点才需要执行
        Map<String, List<String>> finalRelationMap = relationMap;
        List<ExecuteNodeParam> executeNodeParamList = jobWorkRunNodePos.stream()
                .filter(x -> jobWorkNodeMap.containsKey(x.getNodeId()))
                .map(x -> {
                    JobWorkNodeVo jobWorkNodeVo = jobWorkNodeMap.get(x.getNodeId());
                    // 获取该节点的关联节点id list
                    List<String> nodeRelationIdList = finalRelationMap.get(jobWorkNodeVo.getNodeId());
                    ExecuteNodeParam executeNodeParam = BeanUtil.toBean(x, ExecuteNodeParam.class);
                    executeNodeParam.setNodeType(jobWorkNodeVo.getNodeType());
                    executeNodeParam.setDbType(jobWorkNodeVo.getDbType());
                    executeNodeParam.setExecuteContent(jobWorkNodeVo.getExecuteContent());
                    executeNodeParam.setExecuteContentParam(jobWorkNodeVo.getExecuteContentParam());
                    executeNodeParam.setExecuteHandler(jobWorkNodeVo.getExecuteHandler());
                    executeNodeParam.setScriptType(jobWorkNodeVo.getScriptType());
                    executeNodeParam.setUpdateTime(jobWorkNodeVo.getUpdateTime());
                    executeNodeParam.setNodeRelationIdList(nodeRelationIdList);
                    executeNodeParam.setNodeRunStatus(x.getNodeRunStatus());
                    if (StrUtil.equals(jobWorkNodeVo.getNodeType(), NODE_TYPE_FILE_TO_DB.getCode())) {
                        if (jobWorkImportFileMap != null && jobWorkImportFileMap.containsKey(jobWorkNodeVo.getNodeId())) {
                            executeNodeParam.setExecuteFileToDbParam(BeanUtil.toBean(jobWorkImportFileMap.get(jobWorkNodeVo.getNodeId()), ExecuteFileToDbParam.class));
                        }
                    }
                    if (StrUtil.equals(jobWorkNodeVo.getNodeType(), NODE_TYPE_DB_TO_FILE.getCode())) {
                        if (jobWorkExportFileMap != null && jobWorkExportFileMap.containsKey(jobWorkNodeVo.getNodeId())) {
                            executeNodeParam.setExecuteDbToFileParam(BeanUtil.toBean(jobWorkExportFileMap.get(jobWorkNodeVo.getNodeId()), ExecuteDbToFileParam.class));
                        }
                    }
                    return executeNodeParam;
                }).collect(Collectors.toList());
        executeWorkParam.setExecuteNodeParamList(executeNodeParamList);
        return executeWorkParam;
    }

    private boolean isNodeTurnDateMatched(ExecuteNodeParam executeNodeParam, Date turnDate, Integer workType) {
        if (workType == WorkTypeEnum.TYPE_SEQUENCE.getCode()) {
            return true;
        }
        return workType == WorkTypeEnum.TYPE_TURN.getCode()
                && executeNodeParam.getTurnDate() != null
                && turnDate != null
                && DateUtil.compare(executeNodeParam.getTurnDate(), turnDate) == 0;
    }

    private List<String> getExecuteCompleteNodeIdByTypeList(List<ExecuteNodeParam> executeNodeParamList,
                                                            Date turnDate, Integer workType) {
        if (workType == WorkTypeEnum.TYPE_TURN.getCode()) {
            return executeNodeParamList.stream()
                    .filter(x -> {
                        boolean flag = x.getNodeRunStatus() == FlowRunStatusEnum.COMPLETE.getCode();
                        if (flag && x.getTurnDate() != null) {
                            flag = turnDate != null && DateUtil.compare(x.getTurnDate(), turnDate) == 0;
                        }
                        return flag;
                    })
                    .map(ExecuteNodeParam::getNodeId)
                    .collect(Collectors.toList());
        } else if (workType == WorkTypeEnum.TYPE_SEQUENCE.getCode()) {
            return executeNodeParamList.stream()
                    .filter(x -> x.getNodeRunStatus() == FlowRunStatusEnum.COMPLETE.getCode())
                    .map(ExecuteNodeParam::getNodeId)
                    .collect(Collectors.toList());
        }
        return null;
    }

    /**
     * 更新节点状态
     *
     * @param executeWorkParam 执行作业参数
     * @param nodeStatus       节点状态
     */
    private void updateNodeRunStatus(ExecuteWorkParam executeWorkParam, Integer nodeStatus) {
        if (executeWorkParam == null || CollUtil.isEmpty(executeWorkParam.getExecuteNodeParamList())) {
            return;
        }
        List<String> runNodeIdList = executeWorkParam.getExecuteNodeParamList().stream()
                .map(ExecuteNodeParam::getRunNodeId)
                .collect(Collectors.toList());
        JobWorkRunNodePo jobWorkRunNodePo = new JobWorkRunNodePo();
        jobWorkRunNodePo.setNodeRunStatus(nodeStatus);
        if (Objects.equals(nodeStatus, FlowRunStatusEnum.DISPATCHED.getCode())) {
            insertRunNodeLogs(executeWorkParam);
            jobWorkRunNodeMapper.update(jobWorkRunNodePo, Wrappers.lambdaUpdate(JobWorkRunNodePo.class)
                    .in(JobWorkRunNodePo::getRunNodeId, runNodeIdList)
                    .eq(JobWorkRunNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode()));
        } else {
            jobWorkRunNodeMapper.update(jobWorkRunNodePo, Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                    .in(JobWorkRunNodePo::getRunNodeId, runNodeIdList));
        }
        updateRunWorkStatus(executeWorkParam.getRunWorkId(), nodeStatus);
    }

    private void updateRunWorkStatus(String runWorkId, Integer runStatus) {
        if (StrUtil.isBlank(runWorkId) || runStatus == null) {
            return;
        }
        if (runStatus == FlowRunStatusEnum.DISPATCHED.getCode()) {
            jobRunWorkMapper.update(null, Wrappers.lambdaUpdate(JobWorkRunPo.class)
                    .set(JobWorkRunPo::getRunWorkStatus, runStatus)
                    .eq(JobWorkRunPo::getRunWorkId, runWorkId)
                    .eq(JobWorkRunPo::getRunWorkStatus, FlowRunStatusEnum.WAIT.getCode()));
            return;
        }
        if (runStatus == FlowRunStatusEnum.RUNNING.getCode()
                || runStatus == FlowRunStatusEnum.WAIT.getCode()
                || runStatus == FlowRunStatusEnum.EXCEPTION.getCode()) {
            JobWorkRunPo jobRunWorkPo = new JobWorkRunPo();
            jobRunWorkPo.setRunWorkId(runWorkId);
            jobRunWorkPo.setRunWorkStatus(runStatus);
            jobRunWorkMapper.updateById(jobRunWorkPo);
        }
    }

    private void insertRunNodeLogs(ExecuteWorkParam executeWorkParam) {
        for (ExecuteNodeParam executeNodeParam : executeWorkParam.getExecuteNodeParamList()) {
            if (getRunNodeLog(executeNodeParam.getNodeLogId()) != null) {
                continue;
            }
            jobWorkRunNodeLogMapper.insert(buildRunNodeLog(executeNodeParam, executeWorkParam.getExecutorAddress(),
                    0, DISPATCHED_HANDLE_MSG, null));
        }
    }

    public void markRunNodesDispatchFailed(ExecuteWorkParam executeWorkParam, String handleMsg) {
        if (executeWorkParam == null || CollUtil.isEmpty(executeWorkParam.getExecuteNodeParamList())) {
            return;
        }
        List<String> runNodeIdList = executeWorkParam.getExecuteNodeParamList().stream()
                .map(ExecuteNodeParam::getRunNodeId)
                .collect(Collectors.toList());
        for (ExecuteNodeParam executeNodeParam : executeWorkParam.getExecuteNodeParamList()) {
            JobWorkRunNodeLogPo oldLog = getRunNodeLog(executeNodeParam.getNodeLogId());
            if (oldLog == null) {
                jobWorkRunNodeLogMapper.insert(buildRunNodeLog(executeNodeParam, executeWorkParam.getExecutorAddress(),
                        HandleCodeConstant.HANDLE_CODE_FAIL, handleMsg, DateUtil.date()));
            } else {
                updateCallBackRunNodeLog(executeNodeParam.getNodeLogId(), HandleCodeConstant.HANDLE_CODE_FAIL, handleMsg);
            }
        }
        JobWorkRunNodePo updateRunNodePo = new JobWorkRunNodePo();
        updateRunNodePo.setNodeRunStatus(FlowRunStatusEnum.EXCEPTION.getCode());
        jobWorkRunNodeMapper.update(updateRunNodePo, Wrappers.lambdaUpdate(JobWorkRunNodePo.class)
                .in(JobWorkRunNodePo::getRunNodeId, runNodeIdList)
                .in(JobWorkRunNodePo::getNodeRunStatus,
                        FlowRunStatusEnum.WAIT.getCode(),
                        FlowRunStatusEnum.DISPATCHED.getCode()));

        JobWorkRunPo jobRunWorkPo = new JobWorkRunPo();
        jobRunWorkPo.setRunWorkId(executeWorkParam.getRunWorkId());
        jobRunWorkPo.setRunWorkStatus(FlowRunStatusEnum.EXCEPTION.getCode());
        jobRunWorkMapper.updateById(jobRunWorkPo);
    }

    /**
     * 更新节点翻牌日期
     *
     * @param runNodeId 节点id
     */
    private void updateNodeTurnDate(String runNodeId, String runWorkId, Integer workType) {
        JobWorkRunNodePo jobWorkRunNodePo = jobWorkRunNodeMapper.selectById(runNodeId);
        JobWorkRunPo jobRunWorkPo = jobRunWorkMapper.selectById(runWorkId);
        if (jobWorkRunNodePo == null || jobRunWorkPo == null) {
            return;
        }
        boolean canComplete = workType == WorkTypeEnum.TYPE_SEQUENCE.getCode()
                || (jobWorkRunNodePo.getTurnDate() != null && jobRunWorkPo.getTurnDate() != null
                && DateUtil.compare(jobWorkRunNodePo.getTurnDate(), jobRunWorkPo.getTurnDate()) == 0);
        if (canComplete) {
            jobWorkRunNodePo.setRunWorkId(runWorkId);
            // 如果节点已经完成初始化结束时间
            jobWorkRunNodePo.setEndTime(LocalDateTime.now());
            jobWorkRunNodePo.setNodeRunStatus(FlowRunStatusEnum.COMPLETE.getCode());
            jobWorkRunNodeMapper.updateById(jobWorkRunNodePo);
        }
    }


    /**
     * 节点异常停止
     * 需要将节点状态改为异常，然后将运行节点状态改为失败
     *
     * @param nodeRunId 运行节点id
     */
    public void exceptionStopNode(String nodeRunId) {
        JobWorkRunNodePo jobWorkRunNodePo = jobWorkRunNodeMapper.selectById(nodeRunId);
        if (jobWorkRunNodePo == null) {
            return;
        }
        JobWorkRunNodePo updateRunNodePo = new JobWorkRunNodePo();
        updateRunNodePo.setRunNodeId(nodeRunId);
        updateRunNodePo.setNodeRunStatus(FlowRunStatusEnum.EXCEPTION.getCode());

        jobWorkRunNodeMapper.updateById(updateRunNodePo);
    }

    /**
     * 根据剩余重试次数更新节点失败状态。
     */
    private void updateNodeStatusWithRetry(String nodeRunId) {
        JobWorkRunNodePo oldRunNodePo = jobWorkRunNodeMapper.selectById(nodeRunId);
        if (oldRunNodePo == null) {
            return;
        }

        Integer retryTimes = oldRunNodePo.getRetryTimes();
        if (retryTimes != null && retryTimes > 0) {
            JobWorkRunNodePo jobWorkRunNodePo = new JobWorkRunNodePo();
            jobWorkRunNodePo.setRunNodeId(nodeRunId);
            jobWorkRunNodePo.setNodeRunStatus(FlowRunStatusEnum.WAIT.getCode());
            jobWorkRunNodePo.setRetryTimes(retryTimes - 1);
            jobWorkRunNodeMapper.updateById(jobWorkRunNodePo);

            JobWorkRunPo jobRunWorkPo = new JobWorkRunPo();
            jobRunWorkPo.setRunWorkId(oldRunNodePo.getRunWorkId());
            jobRunWorkPo.setRunWorkStatus(FlowRunStatusEnum.WAIT.getCode());
            jobRunWorkMapper.updateById(jobRunWorkPo);
        } else {
            // 如果剩余重试次数为0，则修改运行节点状态为失败
            exceptionStopNode(nodeRunId);
        }
    }

    /**
     * 修改运行节点日志状态
     */
    public void updateCallBackRunNodeLog(String nodeLogId,
                                         Integer handleCode,
                                         String handleMsg) {
        JobWorkRunNodeLogPo jobWorkRunNodeLogPo = new JobWorkRunNodeLogPo();

        jobWorkRunNodeLogPo.setNodeLogId(nodeLogId)
                .setHandleCode(handleCode).setHandleMsg(handleMsg).setCallBackTime(DateUtil.date());
        jobWorkRunNodeLogMapper.updateById(jobWorkRunNodeLogPo);
    }

    /**
     * 修改运行节点日志状态
     */
    public void updateRunNodeLogHandle(String nodeLogId,
                                        Integer handleCode,
                                        String handleMsg) {
        JobWorkRunNodeLogPo jobWorkRunNodeLogPo = new JobWorkRunNodeLogPo();
        jobWorkRunNodeLogPo.setNodeLogId(nodeLogId)
                .setHandleCode(handleCode).setHandleMsg(handleMsg);
        jobWorkRunNodeLogMapper.updateById(jobWorkRunNodeLogPo);
    }

    /**
     * 执行器已拉取并开始执行节点后，按单个运行节点精确置为运行中。
     */
    public void markRunNodeStarted(RunNodeLogEventParam eventParam, String handleMsg) {
        if (eventParam == null || StrUtil.isBlank(eventParam.getRunNodeId()) || StrUtil.isBlank(eventParam.getNodeLogId())) {
            return;
        }
        JobWorkRunNodePo updateRunNodePo = new JobWorkRunNodePo();
        updateRunNodePo.setNodeRunStatus(FlowRunStatusEnum.RUNNING.getCode());
        updateRunNodePo.setStartTime(LocalDateTime.now());
        int updateCount = jobWorkRunNodeMapper.update(updateRunNodePo, Wrappers.lambdaUpdate(JobWorkRunNodePo.class)
                .eq(JobWorkRunNodePo::getRunNodeId, eventParam.getRunNodeId())
                .in(JobWorkRunNodePo::getNodeRunStatus,
                        FlowRunStatusEnum.DISPATCHED.getCode(),
                        FlowRunStatusEnum.WAIT.getCode()));

        if (updateCount <= 0 && getRunNodeLog(eventParam.getNodeLogId()) != null) {
            updateRunNodeLogHandle(eventParam.getNodeLogId(), 0, handleMsg);
            return;
        }

        JobWorkRunNodeLogPo oldLog = getRunNodeLog(eventParam.getNodeLogId());
        if (oldLog == null) {
            jobWorkRunNodeLogMapper.insert(buildRunNodeLog(eventParam, handleMsg));
        } else {
            updateRunNodeLogHandle(eventParam.getNodeLogId(), 0, handleMsg);
        }

        updateRunWorkStatus(eventParam.getRunWorkId(), FlowRunStatusEnum.RUNNING.getCode());
    }

    private JobWorkRunNodeLogPo buildRunNodeLog(ExecuteNodeParam executeNodeParam,
                                                String executorAddress,
                                                Integer handleCode,
                                                String handleMsg,
                                                Date callBackTime) {
        JobWorkRunNodeLogPo jobWorkRunNodeLogPo = BeanUtil.toBean(executeNodeParam, JobWorkRunNodeLogPo.class);
        jobWorkRunNodeLogPo.setWorkId(executeNodeParam.getWorkId());
        jobWorkRunNodeLogPo.setRunWorkId(executeNodeParam.getRunWorkId());
        jobWorkRunNodeLogPo.setNodeId(executeNodeParam.getNodeId());
        jobWorkRunNodeLogPo.setRunNodeId(executeNodeParam.getRunNodeId());
        jobWorkRunNodeLogPo.setExecutorAddress(executorAddress);
        jobWorkRunNodeLogPo.setHandleCode(handleCode);
        jobWorkRunNodeLogPo.setHandleMsg(handleMsg);
        jobWorkRunNodeLogPo.setCreateTime(LocalDateTime.now());
        jobWorkRunNodeLogPo.setCallBackTime(callBackTime);
        return jobWorkRunNodeLogPo;
    }

    private JobWorkRunNodeLogPo buildRunNodeLog(RunNodeLogEventParam eventParam, String handleMsg) {
        JobWorkRunNodeLogPo jobWorkRunNodeLogPo = new JobWorkRunNodeLogPo();
        jobWorkRunNodeLogPo.setNodeLogId(eventParam.getNodeLogId());
        jobWorkRunNodeLogPo.setWorkId(eventParam.getWorkId());
        jobWorkRunNodeLogPo.setRunWorkId(eventParam.getRunWorkId());
        jobWorkRunNodeLogPo.setNodeId(eventParam.getNodeId());
        jobWorkRunNodeLogPo.setRunNodeId(eventParam.getRunNodeId());
        jobWorkRunNodeLogPo.setHandleCode(0);
        jobWorkRunNodeLogPo.setHandleMsg(handleMsg);
        jobWorkRunNodeLogPo.setCreateTime(LocalDateTime.now());
        jobWorkRunNodeLogPo.setCallBackTime(null);
        return jobWorkRunNodeLogPo;
    }

    public JobWorkRunNodeLogPo getRunNodeLog(String nodeLogId) {
        return jobWorkRunNodeLogMapper.selectById(nodeLogId);
    }

    /**
     * 运行节点执行器失联或超过固定时间仍未完成闭环时标记为异常。
     */
    public void updateTimeOutRunNodeStatus() {

        List<JobWorkRunNodePo> jobWorkRunNodePos = jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                .in(JobWorkRunNodePo::getNodeRunStatus,
                        FlowRunStatusEnum.DISPATCHED.getCode(),
                        FlowRunStatusEnum.RUNNING.getCode()));
        for (JobWorkRunNodePo jobWorkRunNodePo : jobWorkRunNodePos) {
            JobWorkRunNodeLogPo runNodeLogPo = getUnclosedRunNodeLog(jobWorkRunNodePo.getRunNodeId());
            // 通过地址检查到服务已经离线了，那么就会将该地址的所有任务标记为异常
            if (runNodeLogPo != null && isExecutorOffline(runNodeLogPo.getExecutorAddress())) {
                markUnclosedRunNodeException(jobWorkRunNodePo, runNodeLogPo, HandleCodeConstant.HANDLE_CODE_TIMEOUT,
                        "运行节点执行器失联，未完成闭环");
            } else if (jobWorkRunNodePo.getNodeRunStatus() == FlowRunStatusEnum.DISPATCHED.getCode()
                    && runNodeLogPo != null
                    && runNodeLogPo.getCreateTime() != null
                    && !runNodeLogPo.getCreateTime().isAfter(LocalDateTime.now().minusHours(DISPATCHED_CONFIRM_TIMEOUT_HOURS))) {
                markUnclosedRunNodeException(jobWorkRunNodePo, runNodeLogPo, HandleCodeConstant.HANDLE_CODE_TIMEOUT,
                        "运行节点已下发但执行器未确认开始");
            } else if (jobWorkRunNodePo.getStartTime() != null
                    && !jobWorkRunNodePo.getStartTime().isAfter(LocalDateTime.now().minusHours(RUNNING_CLOSE_TIMEOUT_HOURS))) {
                markUnclosedRunNodeException(jobWorkRunNodePo, runNodeLogPo, HandleCodeConstant.HANDLE_CODE_TIMEOUT,
                        "运行节点超时未完成闭环");
            }
        }
    }

    private JobWorkRunNodeLogPo getUnclosedRunNodeLog(String runNodeId) {
        return jobWorkRunNodeLogMapper.selectOne(Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                .eq(JobWorkRunNodeLogPo::getRunNodeId, runNodeId)
                .isNull(JobWorkRunNodeLogPo::getCallBackTime)
                .orderByDesc(JobWorkRunNodeLogPo::getCreateTime)
                .last("limit 1"));
    }

    private boolean isExecutorOffline(String executorAddress) {
        if (StrUtil.isBlank(executorAddress)) {
            return false;
        }
        List<JobRegistryPo> registryList = jobRegistryMapper.selectList(Wrappers.lambdaQuery(JobRegistryPo.class)
                .eq(JobRegistryPo::getRegistryGroup, RegistryConfig.RegistType.EXECUTOR.name())
                .ge(JobRegistryPo::getUpdateTime, DateUtil.offsetSecond(DateUtil.date(), -RegistryConfig.DEAD_TIMEOUT)));
        if (CollUtil.isEmpty(registryList)) {
            return true;
        }
        String targetAddress = executorAddress.trim();
        for (JobRegistryPo registryPo : registryList) {
            if (StrUtil.isBlank(registryPo.getRegistryValue())) {
                continue;
            }
            for (String address : registryPo.getRegistryValue().split(StrPool.COMMA)) {
                if (StrUtil.equals(targetAddress, StrUtil.trim(address))) {
                    return false;
                }
            }
        }
        return true;
    }

    private void markUnclosedRunNodeException(JobWorkRunNodePo jobWorkRunNodePo,
                                              JobWorkRunNodeLogPo runNodeLogPo,
                                              Integer handleCode,
                                              String handleMsg) {
        jobWorkRunNodePo.setNodeRunStatus(FlowRunStatusEnum.EXCEPTION.getCode());
        jobWorkRunNodeMapper.updateById(jobWorkRunNodePo);
        if (runNodeLogPo != null) {
            updateCallBackRunNodeLog(runNodeLogPo.getNodeLogId(), handleCode, handleMsg);
        }
    }


}
