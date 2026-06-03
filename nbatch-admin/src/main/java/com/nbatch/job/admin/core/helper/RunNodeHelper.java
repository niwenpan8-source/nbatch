package com.nbatch.job.admin.core.helper;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobRunWorkPo;
import com.nbatch.job.admin.core.domain.po.JobWorkExportFilePo;
import com.nbatch.job.admin.core.domain.po.JobWorkImportFilePo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodeRelationPo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodePo;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeVo;
import com.nbatch.job.core.enums.RunWorkStatusEnum;
import com.nbatch.job.core.enums.WorkTypeEnum;
import com.nbatch.job.admin.mapper.IJobRunWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkExportFileMapper;
import com.nbatch.job.admin.mapper.IJobWorkImportFileMapper;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeRelationMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeLogMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeMapper;
import com.nbatch.job.core.biz.model.ExecuteDbToFileParam;
import com.nbatch.job.core.biz.model.ExecuteFileToDbParam;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.nbatch.job.admin.core.enums.NodeTypeEnum.NODE_TYPE_DB_TO_FILE;
import static com.nbatch.job.admin.core.enums.NodeTypeEnum.NODE_TYPE_FILE_TO_DB;

/**
 * @description: 作业运行节点帮助类
 * @author: Mr.ni
 * @date: 2025/11/20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RunNodeHelper {

    private final IJobWorkRunNodeMapper jobWorkRunNodeMapper;

    private final IJobWorkNodeMapper jobWorkNodeMapper;

    private final IJobWorkNodeRelationMapper jobWorkNodeRelationMapper;

    private final IJobRunWorkMapper jobRunWorkMapper;

    private final IJobWorkExportFileMapper jobWorkExportFileMapper;

    private final IJobWorkImportFileMapper jobWorkImportFileMapper;

    private final IJobWorkRunNodeLogMapper jobWorkRunNodeLogMapper;

    private final IJobWorkMapper jobWorkMapper;

    /**
     * 获取所有运行的节点
     *
     * @param workId 作业id
     */
    public ExecuteWorkParam getExecuteWorkObj(String workId) {
        List<JobRunWorkPo> jobRunWorkPoList = jobRunWorkMapper.selectList(Wrappers.lambdaQuery(JobRunWorkPo.class)
                .eq(JobRunWorkPo::getWorkId, workId)
                .in(JobRunWorkPo::getRunWorkStatus, RunWorkStatusEnum.RUNNING.getCode(), RunWorkStatusEnum.WAIT.getCode())
                .orderByDesc(JobRunWorkPo::getCreateTime));
        if (CollUtil.isEmpty(jobRunWorkPoList)) {
            log.info("workId:{},不存在运行作业！", workId);
            return null;
        }
        JobRunWorkPo jobRunWorkPo = jobRunWorkPoList.get(0);
        // 对于作业来说等待中以及待运行的作业都可以再次进入
        if (jobRunWorkPo.getRunWorkStatus() != RunWorkStatusEnum.WAIT.getCode()) {
            log.info("workId:{},workStatus:{},只有等待中的节点才可以执行！", workId,
                    RunWorkStatusEnum.getValueByCode(jobRunWorkPo.getRunWorkStatus()));
            return null;
        }
        return buildExecuteWorkParam(jobRunWorkPo);
    }

    /**
     * 获取当前运行作业中依赖已满足、可继续下发执行的节点
     *
     * @param jobRunWorkPo 运行作业
     */
    public ExecuteWorkParam getEnableExecuteWork(JobRunWorkPo jobRunWorkPo) {
        ExecuteWorkParam executeWorkParam = buildExecuteWorkParam(jobRunWorkPo);
        if (executeWorkParam == null || CollUtil.isEmpty(executeWorkParam.getExecuteNodeParamList())) {
            return null;
        }

        Date turnDate = executeWorkParam.getTurnDate();
        Integer workType = executeWorkParam.getWorkType();
        List<String> completeNodeIdList = getExecuteCompleteNodeIdByTypeList(
                executeWorkParam.getExecuteNodeParamList(), turnDate, workType);

        List<ExecuteNodeParam> enableExecuteNodeList = executeWorkParam.getExecuteNodeParamList().stream()
                .filter(x -> x.getNodeRunStatus() == RunWorkStatusEnum.WAIT.getCode())
                .filter(x -> isNodeTurnDateMatched(x, turnDate, workType))
                .filter(x -> {
                    if (CollUtil.isEmpty(x.getNodeRelationIdList())) {
                        return true;
                    }
                    if (CollUtil.isEmpty(completeNodeIdList)) {
                        return false;
                    }
                    return new HashSet<>(completeNodeIdList).containsAll(x.getNodeRelationIdList());
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
    private ExecuteWorkParam buildExecuteWorkParam(JobRunWorkPo jobRunWorkPo) {
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
        List<ExecuteNodeParam> executeNodeParamList = jobWorkRunNodePos.stream().filter(x -> jobWorkNodeMap.containsKey(x.getNodeId())).map(x -> {
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
            Date nextTurnDate = turnDate == null ? null : DateUtil.offset(turnDate, DateField.DAY_OF_MONTH, 1);
            return executeNodeParamList.stream()
                    .filter(x -> {
                        boolean flag = x.getNodeRunStatus() == RunWorkStatusEnum.COMPLETE.getCode();
                        if (flag && x.getTurnDate() != null) {
                            flag = nextTurnDate != null && DateUtil.compare(x.getTurnDate(), nextTurnDate) == 0;
                        }
                        return flag;
                    })
                    .map(ExecuteNodeParam::getNodeId)
                    .collect(Collectors.toList());
        } else if (workType == WorkTypeEnum.TYPE_SEQUENCE.getCode()) {
            return executeNodeParamList.stream()
                    .filter(x -> x.getNodeRunStatus() == RunWorkStatusEnum.COMPLETE.getCode())
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
    public void updateNodeRunStatus(ExecuteWorkParam executeWorkParam, Integer nodeStatus) {
        if (executeWorkParam == null || CollUtil.isEmpty(executeWorkParam.getExecuteNodeParamList())) {
            return;
        }
        List<String> runNodeIdList = executeWorkParam.getExecuteNodeParamList().stream()
                .map(ExecuteNodeParam::getRunNodeId)
                .collect(Collectors.toList());
        JobWorkRunNodePo jobWorkRunNodePo = new JobWorkRunNodePo();
        jobWorkRunNodePo.setNodeRunStatus(nodeStatus);

        List<JobWorkRunNodeLogPo> nodeLogList = executeWorkParam.getExecuteNodeParamList().stream()
                .map(x -> {
                    JobWorkRunNodeLogPo jobWorkRunNodeLogPo = BeanUtil.toBean(x, JobWorkRunNodeLogPo.class);
                    jobWorkRunNodeLogPo.setWorkId(x.getWorkId());
                    jobWorkRunNodeLogPo.setRunWorkId(x.getRunWorkId());
                    jobWorkRunNodeLogPo.setNodeId(x.getNodeId());
                    jobWorkRunNodeLogPo.setRunNodeId(x.getRunNodeId());
                    jobWorkRunNodeLogPo.setHandleCode(0);
                    jobWorkRunNodeLogPo.setCreateTime(DateUtil.date());
                    return jobWorkRunNodeLogPo;
                }).collect(Collectors.toList());
        for (JobWorkRunNodeLogPo jobWorkRunNodeLogPo : nodeLogList) {
            jobWorkRunNodeLogMapper.insert(jobWorkRunNodeLogPo);
        }
        jobWorkRunNodeMapper.update(jobWorkRunNodePo, Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                .in(JobWorkRunNodePo::getRunNodeId, runNodeIdList));
        if (nodeStatus == RunWorkStatusEnum.RUNNING.getCode()
                || nodeStatus == RunWorkStatusEnum.WAIT.getCode()
                || nodeStatus == RunWorkStatusEnum.FAIL.getCode()) {
            JobRunWorkPo jobRunWorkPo = new JobRunWorkPo();
            jobRunWorkPo.setRunWorkId(executeWorkParam.getRunWorkId());
            jobRunWorkPo.setRunWorkStatus(nodeStatus);
            jobRunWorkMapper.updateById(jobRunWorkPo);
        }
    }

    /**
     * 更新节点翻牌日期
     *
     * @param runNodeId 节点id
     */
    public void updateNodeTurnDate(String runNodeId, String runWorkId, Integer workType) {
        JobWorkRunNodePo jobWorkRunNodePo = jobWorkRunNodeMapper.selectById(runNodeId);
        JobRunWorkPo jobRunWorkPo = jobRunWorkMapper.selectById(runWorkId);
        if (jobWorkRunNodePo == null || jobRunWorkPo == null) {
            return;
        }
        boolean canComplete = workType == WorkTypeEnum.TYPE_SEQUENCE.getCode()
                || (jobWorkRunNodePo.getTurnDate() != null && jobRunWorkPo.getTurnDate() != null
                && DateUtil.compare(jobWorkRunNodePo.getTurnDate(), jobRunWorkPo.getTurnDate()) == 0);
        if (canComplete) {
            jobWorkRunNodePo.setRunWorkId(runWorkId);
            // 只有翻牌节点类型才会设置翻牌时间
            if (workType == WorkTypeEnum.TYPE_TURN.getCode()) {
                jobWorkRunNodePo.setTurnDate(DateUtil.offset(jobWorkRunNodePo.getTurnDate(), DateField.DAY_OF_MONTH, 1));
            }
            jobWorkRunNodePo.setNodeRunStatus(RunWorkStatusEnum.COMPLETE.getCode());
            jobWorkRunNodeMapper.updateById(jobWorkRunNodePo);
        }
    }

    /**
     * 修改作业翻牌时间
     */
    public void updateWorkTurnDate() {
        List<JobRunWorkPo> jobRunWorkList = jobRunWorkMapper.selectList(Wrappers.lambdaQuery(JobRunWorkPo.class)
                .in(JobRunWorkPo::getRunWorkStatus, RunWorkStatusEnum.RUNNING.getCode(), RunWorkStatusEnum.WAIT.getCode()));
        for (JobRunWorkPo jobRunWorkPo : jobRunWorkList) {
            List<JobWorkRunNodePo> jobWorkRunNodePos = jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                    .eq(JobWorkRunNodePo::getRunWorkId, jobRunWorkPo.getRunWorkId()));

            if (CollUtil.isNotEmpty(jobWorkRunNodePos)) {
                DateTime offsetTurnDate = jobRunWorkPo.getTurnDate() == null ? null : DateUtil.offset(jobRunWorkPo.getTurnDate(), DateField.DAY_OF_MONTH, 1);
                long count = jobWorkRunNodePos.stream()
                        .filter(x -> {
                            boolean flag = x.getNodeRunStatus() == RunWorkStatusEnum.COMPLETE.getCode();
                            // 这里由于当作业类型为顺序类型时翻牌时间为空，不判断翻牌时间
                            if (flag && x.getTurnDate() != null) {
                                flag = DateUtil.compare(x.getTurnDate(), offsetTurnDate) == 0;
                            }
                            return flag;
                        })
                        .count();
                if (count == jobWorkRunNodePos.size()) {
                    JobRunWorkPo updateJobWork = new JobRunWorkPo()
                            .setRunWorkStatus(RunWorkStatusEnum.COMPLETE.getCode())
                            .setWorkId(jobRunWorkPo.getWorkId())
                            .setRunWorkId(jobRunWorkPo.getRunWorkId());
                    if (offsetTurnDate != null) {
                        updateJobWork.setTurnDate(offsetTurnDate);
                    }
                    jobRunWorkMapper.updateById(updateJobWork);
                }
            }
        }
    }

    /**
     * 更新节点状态
     *
     * @param nodeRunId  节点id
     * @param nodeStatus 节点状态
     */
    public void updateNodeStatusById(String nodeRunId, Integer nodeStatus) {
        JobWorkRunNodePo oldRunNodePo = jobWorkRunNodeMapper.selectById(nodeRunId);
        JobWorkRunNodePo jobWorkRunNodePo = new JobWorkRunNodePo();
        jobWorkRunNodePo.setRunNodeId(nodeRunId);
        jobWorkRunNodePo.setNodeRunStatus(nodeStatus);
        jobWorkRunNodeMapper.updateById(jobWorkRunNodePo);
        if (oldRunNodePo != null && nodeStatus == RunWorkStatusEnum.FAIL.getCode()) {
            JobRunWorkPo jobRunWorkPo = new JobRunWorkPo();
            jobRunWorkPo.setRunWorkId(oldRunNodePo.getRunWorkId());
            jobRunWorkPo.setRunWorkStatus(RunWorkStatusEnum.FAIL.getCode());
            jobRunWorkMapper.updateById(jobRunWorkPo);
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
     * 修改超过固定时间，任务状态仍然为1的节点任务修改为0
     */
    public void updateTimeOutRunNodeStatus() {

        List<JobWorkRunNodePo> jobWorkRunNodePos = jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                .eq(JobWorkRunNodePo::getNodeRunStatus, RunWorkStatusEnum.RUNNING.getCode())
                .le(JobWorkRunNodePo::getCreateTime, DateUtil.offsetHour(DateUtil.date(), -6)));
        for (JobWorkRunNodePo jobWorkRunNodePo : jobWorkRunNodePos) {
            jobWorkRunNodePo.setNodeRunStatus(RunWorkStatusEnum.WAIT.getCode());
            jobWorkRunNodeMapper.updateById(jobWorkRunNodePo);
        }
    }


}
