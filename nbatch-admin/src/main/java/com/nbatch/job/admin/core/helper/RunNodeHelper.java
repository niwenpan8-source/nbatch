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
import com.nbatch.job.admin.core.domain.vo.JobWorkRunNodeVo;
import com.nbatch.job.admin.core.enums.RunWorkStatusEnum;
import com.nbatch.job.admin.core.enums.WorkTypeEnum;
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

import java.util.ArrayList;
import java.util.Date;
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
     * 获取作业运行节点
     *
     * @param workId 作业id
     */
    public ExecuteWorkParam getEnableExecuteNodeList(String workId) {
        ExecuteWorkParam executeWorkParam = new ExecuteWorkParam();
        executeWorkParam.setWorkId(workId);
        JobWorkPo jobWorkPo = jobWorkMapper.selectById(workId);
        if (jobWorkPo == null) {
            return executeWorkParam;
        }
        executeWorkParam.setWorkId(workId).setWorkType(jobWorkPo.getWorkType());
        List<JobWorkRunNodeVo> allNeedRunNode = this.getAllNeedRunNode(workId, jobWorkPo.getWorkType());
        if (CollUtil.isEmpty(allNeedRunNode)) {
            return executeWorkParam;
        }
        executeWorkParam.setRunWorkId(allNeedRunNode.get(0).getRunWorkId());

        List<String> allNeedRunNodeIds = allNeedRunNode.stream().map(JobWorkRunNodeVo::getNodeId)
                .collect(Collectors.toList());
        List<JobWorkImportFilePo> jobWorkImportFilePos = jobWorkImportFileMapper.selectList(Wrappers.lambdaQuery(JobWorkImportFilePo.class)
                .in(JobWorkImportFilePo::getNodeId, allNeedRunNodeIds));

        List<JobWorkExportFilePo> jobWorkExportFilePos = jobWorkExportFileMapper.selectList(Wrappers.lambdaQuery(JobWorkExportFilePo.class)
                .in(JobWorkExportFilePo::getNodeId, allNeedRunNodeIds));

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

        List<ExecuteNodeParam> executeNodeParamList = new ArrayList<>();
        for (JobWorkRunNodeVo jobWorkRunNodeVo : allNeedRunNode) {
            ExecuteNodeParam nodeParam = BeanUtil.toBean(jobWorkRunNodeVo, ExecuteNodeParam.class);
            nodeParam.setWorkId(workId);
            if (StrUtil.equals(jobWorkRunNodeVo.getNodeType(), NODE_TYPE_FILE_TO_DB.getCode())) {
                if (!(jobWorkImportFileMap != null && jobWorkImportFileMap.containsKey(jobWorkRunNodeVo.getNodeId()))) {
                    continue;
                }
                nodeParam.setExecuteFileToDbParam(BeanUtil.toBean(jobWorkImportFileMap.get(jobWorkRunNodeVo.getNodeId()), ExecuteFileToDbParam.class));
            }
            if (StrUtil.equals(jobWorkRunNodeVo.getNodeType(), NODE_TYPE_DB_TO_FILE.getCode())) {
                if (!(jobWorkExportFileMap != null && jobWorkExportFileMap.containsKey(jobWorkRunNodeVo.getNodeId()))) {
                    continue;
                }
                nodeParam.setExecuteDbToFileParam(BeanUtil.toBean(jobWorkExportFileMap.get(jobWorkRunNodeVo.getNodeId()), ExecuteDbToFileParam.class));
            }
            executeNodeParamList.add(nodeParam);
        }
        executeWorkParam.setExecuteNodeParamList(executeNodeParamList);
        return executeWorkParam;

    }

    /**
     * 所有需要运行的节点
     *
     * @param workId 作业id
     */
    private List<JobWorkRunNodeVo> getAllNeedRunNode(String workId, Integer workType) {
        List<JobRunWorkPo> jobRunWorkPoList = jobRunWorkMapper.selectList(Wrappers.lambdaQuery(JobRunWorkPo.class)
                .eq(JobRunWorkPo::getWorkId, workId)
                .in(JobRunWorkPo::getRunWorkStatus, RunWorkStatusEnum.RUNNING, RunWorkStatusEnum.WAIT)
                .orderByDesc(JobRunWorkPo::getCreateTime));
        if (CollUtil.isEmpty(jobRunWorkPoList)) {
            log.info("workId:{},不存在运行作业！", workId);
            return null;
        }

        JobRunWorkPo jobRunWorkPo = jobRunWorkPoList.get(0);
        // 对于作业来说等待中以及待运行的作业都可以再次进入
        if (jobRunWorkPo.getRunWorkStatus() == RunWorkStatusEnum.COMPLETE.getCode()) {
            log.info("workId:{},目前最新运行作业已经完成！", workId);
            return null;
        }
        List<JobWorkRunNodePo> jobWorkRunNodePos = jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                .eq(JobWorkRunNodePo::getRunWorkId, jobRunWorkPo.getRunWorkId()));
        if (CollUtil.isEmpty(jobWorkRunNodePos)) {
            log.info("workId:{},不存在运行作业节点！", workId);
            return null;
        }
        // 当前作业翻牌日期
        Date turnDate = jobRunWorkPo.getTurnDate();
        List<String> nodeIdList = jobWorkRunNodePos.stream().map(JobWorkRunNodePo::getNodeId)
                .collect(Collectors.toList());

        // 查找作业当中所有的节点
        List<JobWorkNodePo> workAllNodeList = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .in(JobWorkNodePo::getNodeId, nodeIdList));

        Map<String, JobWorkNodeVo> jobWorkNodeMap = workAllNodeList.stream().collect(Collectors
                .toMap(JobWorkNodePo::getNodeId, x -> BeanUtil.toBean(x, JobWorkNodeVo.class)));

        // 查找作业当中没有运行同时翻牌时间必须和作业时间相同的节点,对于节点只有运行状态为等待中的节点才需要执行
        List<JobWorkRunNodeVo> enableExecuteNode = jobWorkRunNodePos.stream()
                .filter(x -> x.getNodeRunStatus() == RunWorkStatusEnum.WAIT.getCode()
                        && DateUtil.compare(x.getTurnDate(), turnDate) == 0
                        && jobWorkNodeMap.containsKey(x.getNodeId()))
                .map(x -> {
                    JobWorkNodeVo jobWorkNodeVo = jobWorkNodeMap.get(x.getNodeId());
                    JobWorkRunNodeVo runNodeVo = BeanUtil.toBean(x, JobWorkRunNodeVo.class);
                    runNodeVo.setNodeType(jobWorkNodeVo.getNodeType());
                    runNodeVo.setDbType(jobWorkNodeVo.getDbType());
                    runNodeVo.setExecuteContent(jobWorkNodeVo.getExecuteContent());
                    runNodeVo.setExecuteContentParam(jobWorkNodeVo.getExecuteContentParam());
                    runNodeVo.setExecuteHandler(jobWorkNodeVo.getExecuteHandler());
                    runNodeVo.setScriptType(jobWorkNodeVo.getScriptType());
                    runNodeVo.setUpdateTime(jobWorkNodeVo.getUpdateTime());
                    return runNodeVo;
                }).collect(Collectors.toList());

        List<String> allRunNodeIdByTypeList = getAllRunNodeIdByTypeList(jobWorkRunNodePos, turnDate, workType);


        List<JobWorkNodeRelationPo> jobWorkNodeRelationPos = jobWorkNodeRelationMapper.selectList(Wrappers.lambdaQuery(JobWorkNodeRelationPo.class)
                .eq(JobWorkNodeRelationPo::getWorkId, workId));


        if (CollUtil.isEmpty(jobWorkNodeRelationPos)) {
            return enableExecuteNode;
        }
        Map<String, List<String>> relationMap = jobWorkNodeRelationPos.stream()
                .collect(Collectors.groupingBy(JobWorkNodeRelationPo::getNodeId1
                , Collectors.mapping(JobWorkNodeRelationPo::getNodeId2, Collectors.toList())));

        // 根据关联节点对不可运行的节点进行过滤
        return enableExecuteNode.stream().filter(x -> {
            if (!relationMap.containsKey(x.getNodeId())) {
                return true;
            }
            if (CollUtil.isEmpty(allRunNodeIdByTypeList)) {
                return false;
            }
            return new HashSet<>(allRunNodeIdByTypeList).containsAll(relationMap.get(x.getNodeId()));
        }).collect(Collectors.toList());
    }

    /**
     * 获取所有运行节点
     *
     * @param jobWorkRunNodePos 作业运行节点
     * @param workType         作业类型
     */
    public List<String> getAllRunNodeIdByTypeList(List<JobWorkRunNodePo> jobWorkRunNodePos,
                                                  Date turnDate, Integer workType) {
        if (workType == WorkTypeEnum.TYPE_TURN.getCode()) {
            return jobWorkRunNodePos.stream()
                    .filter(x -> {
                        boolean flag = x.getNodeRunStatus() == RunWorkStatusEnum.COMPLETE.getCode();
                        // 这里由于当作业类型为顺序类型时翻牌时间为空，不判断翻牌时间
                        if (flag && x.getTurnDate() != null) {
                            flag = DateUtil.compare(x.getTurnDate(), DateUtil.offset(turnDate, DateField.DAY_OF_MONTH, 1)) == 0;
                        }
                        return flag;
                    })
                    .map(JobWorkRunNodePo::getNodeId)
                    .collect(Collectors.toList());
        } else if (workType == WorkTypeEnum.TYPE_SEQUENCE.getCode()) {
            return jobWorkRunNodePos.stream()
                    .filter(x -> x.getNodeRunStatus() == RunWorkStatusEnum.COMPLETE.getCode())
                    .map(JobWorkRunNodePo::getNodeId)
                    .collect(Collectors.toList());
        } else {
            return null;
        }
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
    }

    /**
     * 更新节点翻牌日期
     *
     * @param runNodeId 节点id
     */
    public void updateNodeTurnDate(String runNodeId, String runWorkId, Integer workType) {
        JobWorkRunNodePo jobWorkRunNodePo = jobWorkRunNodeMapper.selectById(runNodeId);
        JobRunWorkPo jobRunWorkPo = jobRunWorkMapper.selectById(runWorkId);
        if (DateUtil.compare(jobWorkRunNodePo.getTurnDate(), jobRunWorkPo.getTurnDate()) == 0) {
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
                .in(JobRunWorkPo::getRunWorkStatus, RunWorkStatusEnum.RUNNING, RunWorkStatusEnum.WAIT));
        for (JobRunWorkPo jobRunWorkPo : jobRunWorkList) {
            List<JobWorkRunNodePo> jobWorkRunNodePos = jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                    .eq(JobWorkRunNodePo::getRunWorkId, jobRunWorkPo.getRunWorkId()));

            if (CollUtil.isNotEmpty(jobWorkRunNodePos)) {
                DateTime offsetTurnDate = DateUtil.offset(jobRunWorkPo.getTurnDate(), DateField.DAY_OF_MONTH, 1);
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
                    JobRunWorkPo updateJobWork = new JobRunWorkPo().setTurnDate(offsetTurnDate)
                            .setRunWorkStatus(RunWorkStatusEnum.COMPLETE.getCode())
                            .setWorkId(jobRunWorkPo.getWorkId())
                            .setRunWorkId(jobRunWorkPo.getRunWorkId());
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
        JobWorkRunNodePo jobWorkRunNodePo = new JobWorkRunNodePo();
        jobWorkRunNodePo.setRunNodeId(nodeRunId);
        jobWorkRunNodePo.setNodeRunStatus(nodeStatus);
        jobWorkRunNodeMapper.updateById(jobWorkRunNodePo);
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


}
