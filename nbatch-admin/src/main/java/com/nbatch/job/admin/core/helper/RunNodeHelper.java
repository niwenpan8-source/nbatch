package com.nbatch.job.admin.core.helper;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobWorkExportFilePo;
import com.nbatch.job.admin.core.domain.po.JobWorkImportFilePo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodeRelationPo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodePo;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeVo;
import com.nbatch.job.admin.core.enums.WorkStatusEnum;
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
@Component
@RequiredArgsConstructor
public class RunNodeHelper {

    private final IJobWorkRunNodeMapper jobWorkRunNodeMapper;

    private final IJobWorkNodeMapper jobWorkNodeMapper;

    private final IJobWorkNodeRelationMapper jobWorkNodeRelationMapper;

    private final IJobWorkMapper jobWorkMapper;

    private final IJobWorkExportFileMapper jobWorkExportFileMapper;

    private final IJobWorkImportFileMapper jobWorkImportFileMapper;

    private final IJobWorkRunNodeLogMapper jobWorkRunNodeLogMapper;


    /**
     * 获取作业运行节点
     *
     * @param workId 作业id
     */
    public ExecuteWorkParam getEnableExecuteNodeList(String workId) {
        ExecuteWorkParam executeWorkParam = new ExecuteWorkParam();
        List<JobWorkNodeVo> allNeedRunNode = this.getAllNeedRunNode(workId);
        if (CollUtil.isEmpty(allNeedRunNode)) {
            return executeWorkParam;
        }
        executeWorkParam.setWorkId(workId);
        List<String> allNeedRunNodeIds = allNeedRunNode.stream().map(JobWorkNodeVo::getNodeId)
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
        for (JobWorkNodeVo jobWorkNodeVo : allNeedRunNode) {
            ExecuteNodeParam nodeParam = BeanUtil.toBean(jobWorkNodeVo, ExecuteNodeParam.class);
            if (StrUtil.equals(jobWorkNodeVo.getNodeType(), NODE_TYPE_FILE_TO_DB.getCode())) {
                if (!(jobWorkImportFileMap != null && jobWorkImportFileMap.containsKey(jobWorkNodeVo.getNodeId()))) {
                    continue;
                }
                nodeParam.setExecuteFileToDbParam(BeanUtil.toBean(jobWorkImportFileMap.get(jobWorkNodeVo.getNodeId()), ExecuteFileToDbParam.class));
            }
            if (StrUtil.equals(jobWorkNodeVo.getNodeType(), NODE_TYPE_DB_TO_FILE.getCode())) {
                if (!(jobWorkExportFileMap != null && jobWorkExportFileMap.containsKey(jobWorkNodeVo.getNodeId()))) {
                    continue;
                }
                nodeParam.setExecuteDbToFileParam(BeanUtil.toBean(jobWorkExportFileMap.get(jobWorkNodeVo.getNodeId()), ExecuteDbToFileParam.class));
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
    private List<JobWorkNodeVo> getAllNeedRunNode(String workId) {
        JobWorkPo jobWorkPo = jobWorkMapper.selectById(workId);
        List<JobWorkRunNodePo> jobWorkRunNodePos = jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                .eq(JobWorkRunNodePo::getWorkId, workId));
        if (CollUtil.isEmpty(jobWorkRunNodePos)) {
            return null;
        }
        // 当前作业翻牌日期
        Date turnDate = jobWorkPo.getTurnDate();
        List<String> nodeIdList = jobWorkRunNodePos.stream().map(JobWorkRunNodePo::getNodeId)
                .collect(Collectors.toList());

        // 查找作业当中所有的节点
        List<JobWorkNodePo> workAllNodeList = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .in(JobWorkNodePo::getNodeId, nodeIdList));

        Map<String, JobWorkNodeVo> jobWorkNodeMap = workAllNodeList.stream().collect(Collectors
                .toMap(JobWorkNodePo::getNodeId, x -> BeanUtil.toBean(x, JobWorkNodeVo.class)));

        // 查找作业当中没有运行同时翻牌时间必须和作业时间相同的节点
        List<JobWorkNodeVo> enableExecuteNode = jobWorkRunNodePos.stream()
                .filter(x -> x.getNodeRunStatus() == 0
                        && DateUtil.compare(x.getTurnDate(), turnDate) == 0
                        && jobWorkNodeMap.containsKey(x.getNodeId()))
                .map(x -> {
                    JobWorkNodeVo jobWorkNodeVo = jobWorkNodeMap.get(x.getNodeId());
                    jobWorkNodeVo.setNodeRunStatus(x.getNodeRunStatus()).setTurnDate(x.getTurnDate());
                    return jobWorkNodeVo;
                }).collect(Collectors.toList());

        List<String> workAllNodeTurnDateList = jobWorkRunNodePos.stream()
                .filter(x -> DateUtil.compare(x.getTurnDate(), DateUtil.offset(turnDate, DateField.DAY_OF_MONTH, 1)) == 0)
                .map(JobWorkRunNodePo::getNodeId)
                .collect(Collectors.toList());


        List<JobWorkNodeRelationPo> jobWorkNodeRelationPos = jobWorkNodeRelationMapper.selectList(Wrappers.lambdaQuery(JobWorkNodeRelationPo.class)
                .eq(JobWorkNodeRelationPo::getWorkId, workId));


        if (CollUtil.isEmpty(jobWorkNodeRelationPos)) {
            return enableExecuteNode;
        }
        Map<String, List<String>> relationMap = jobWorkNodeRelationPos.stream().collect(Collectors.groupingBy(JobWorkNodeRelationPo::getNodeId1
                , Collectors.mapping(JobWorkNodeRelationPo::getNodeId2, Collectors.toList())));

        // 根据关联节点对不可运行的节点进行过滤
        return enableExecuteNode.stream().filter(x -> {
            if (!relationMap.containsKey(x.getNodeId())) {
                return true;
            }
            if (CollUtil.isEmpty(workAllNodeTurnDateList)) {
                return false;
            }
            return new HashSet<>(workAllNodeTurnDateList).containsAll(relationMap.get(x.getNodeId()));
        }).collect(Collectors.toList());
    }

    /**
     * 更新节点状态
     *
     * @param executeWorkParam 执行作业参数
     * @param nodeStatus 节点状态
     */
    public int updateNodeRunStatus(ExecuteWorkParam executeWorkParam, Integer nodeStatus) {
        if (executeWorkParam == null || CollUtil.isEmpty(executeWorkParam.getExecuteNodeParamList())) {
            return 0;
        }
        List<String> nodeIdList = executeWorkParam.getExecuteNodeParamList().stream().map(ExecuteNodeParam::getNodeId)
                .collect(Collectors.toList());
        JobWorkRunNodePo jobWorkRunNodePo = new JobWorkRunNodePo();
        jobWorkRunNodePo.setNodeRunStatus(nodeStatus);

        List<JobWorkRunNodeLogPo> nodeLogList = executeWorkParam.getExecuteNodeParamList().stream()
                .map(x -> {
                    JobWorkRunNodeLogPo jobWorkRunNodeLogPo = BeanUtil.toBean(x, JobWorkRunNodeLogPo.class);
                    jobWorkRunNodeLogPo.setWorkId(executeWorkParam.getWorkId());
                    jobWorkRunNodeLogPo.setHandleCode(0);
                    jobWorkRunNodeLogPo.setCreateTime(DateUtil.date());
                    return jobWorkRunNodeLogPo;
                }).collect(Collectors.toList());
        for (JobWorkRunNodeLogPo jobWorkRunNodeLogPo : nodeLogList) {
            jobWorkRunNodeLogMapper.insert(jobWorkRunNodeLogPo);
        }
        return jobWorkRunNodeMapper.update(jobWorkRunNodePo, Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                .in(JobWorkRunNodePo::getNodeId, nodeIdList));
    }

    /**
     * 更新节点翻牌日期
     *
     * @param nodeId 节点id
     */
    public int updateNodeTurnDate(String nodeId, String workId) {
        JobWorkRunNodePo jobWorkRunNodePo = jobWorkRunNodeMapper.selectOne(Wrappers
                .lambdaQuery(JobWorkRunNodePo.class)
                .eq(JobWorkRunNodePo::getNodeId, nodeId)
                .eq(JobWorkRunNodePo::getWorkId, workId));
        JobWorkPo jobWorkPo = jobWorkMapper.selectById(workId);
        if (DateUtil.compare(jobWorkRunNodePo.getTurnDate(), jobWorkPo.getTurnDate()) == 0) {
            jobWorkRunNodePo.setTurnDate(DateUtil.offset(jobWorkPo.getTurnDate(), DateField.DAY_OF_MONTH, 1));
            jobWorkRunNodePo.setNodeRunStatus(WorkStatusEnum.STOP.getCode());
            return jobWorkRunNodeMapper.update(jobWorkRunNodePo, Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                    .eq(JobWorkRunNodePo::getNodeId, nodeId)
                    .eq(JobWorkRunNodePo::getWorkId, workId));
        }
        return 0;
    }

    /**
     * 修改作业翻牌时间
     */
    public void updateWorkTurnDate() {
        List<JobWorkPo> jobWorkList = jobWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkPo.class));
        for (JobWorkPo jobWorkPo : jobWorkList) {
            List<JobWorkRunNodePo> jobWorkRunNodePos = jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                    .eq(JobWorkRunNodePo::getWorkId, jobWorkPo.getWorkId()));

            if (CollUtil.isNotEmpty(jobWorkRunNodePos)) {
                DateTime offsetTurnDate = DateUtil.offset(jobWorkPo.getTurnDate(), DateField.DAY_OF_MONTH, 1);
                long count = jobWorkRunNodePos.stream().filter(x -> DateUtil.compare(x.getTurnDate(), offsetTurnDate) == 0).count();
                if (count == jobWorkRunNodePos.size()) {
                    JobWorkPo updateJobWork = new JobWorkPo().setTurnDate(offsetTurnDate)
                            .setWorkId(jobWorkPo.getWorkId());
                    jobWorkMapper.updateById(updateJobWork);
                }
            }
        }
    }

    /**
     * 更新节点状态
     *
     * @param nodeId 节点id
     * @param nodeStatus 节点状态
     */
    public int updateNodeStatusById(String workId, String nodeId, Integer nodeStatus) {
        JobWorkRunNodePo jobWorkRunNodePo = new JobWorkRunNodePo();
        jobWorkRunNodePo.setNodeRunStatus(nodeStatus);
        return jobWorkRunNodeMapper.update(jobWorkRunNodePo, Wrappers
                .lambdaQuery(JobWorkRunNodePo.class)
                .eq(JobWorkRunNodePo::getNodeId, nodeId)
                .eq(JobWorkRunNodePo::getWorkId, workId));
    }

    /**
     * 修改运行节点日志状态
     */
    public int updateCallBackRunNodeLog(String nodeLogId,
                                        Integer handleCode,
                                        String handleMsg) {
        JobWorkRunNodeLogPo jobWorkRunNodeLogPo = new JobWorkRunNodeLogPo();

        jobWorkRunNodeLogPo.setNodeLogId(nodeLogId)
                .setHandleCode(handleCode).setHandleMsg(handleMsg).setCallBackTime(DateUtil.date());
        return jobWorkRunNodeLogMapper.updateById(jobWorkRunNodeLogPo);
    }


}
