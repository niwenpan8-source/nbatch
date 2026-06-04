package com.nbatch.job.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nbatch.job.admin.core.domain.param.JobWorkNodeLogPageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkNodePageParam;
import com.nbatch.job.admin.core.domain.param.JobWorkNodeParam;
import com.nbatch.job.admin.core.domain.param.JobWorkNodeRelationParam;
import com.nbatch.job.admin.core.domain.po.JobWorkExportFilePo;
import com.nbatch.job.admin.core.domain.po.JobWorkImportFilePo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkNodeRelationPo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogDetailPo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogPo;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeRelationVo;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeVo;
import com.nbatch.job.admin.core.domain.vo.JobWorkRunNodeLogVo;
import com.nbatch.job.admin.core.domain.vo.JobWorkRunNodeVo;
import com.nbatch.job.admin.core.enums.NodeTypeEnum;
import com.nbatch.job.admin.mapper.IJobWorkExportFileMapper;
import com.nbatch.job.admin.mapper.IJobWorkImportFileMapper;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeRelationMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeLogDetailMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeLogMapper;
import com.nbatch.job.admin.service.IJobWorkNodeService;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @description: 作业节点执行服务实现类
 * @author: Mr.ni
 * @date: 2025/11/13
 */
@Service
public class JobWorkNodeServiceImpl implements IJobWorkNodeService {

    @Resource
    private IJobWorkNodeMapper jobWorkNodeMapper;

    @Resource
    private IJobWorkMapper jobWorkMapper;

    @Resource
    private IJobWorkNodeRelationMapper jobWorkNodeRelationMapper;

    @Resource
    private IJobWorkRunNodeLogMapper jobWorkRunNodeLogMapper;

    @Resource
    private IJobWorkRunNodeLogDetailMapper jobWorkRunNodeLogDetailMapper;

    @Resource
    private IJobWorkRunNodeMapper jobWorkRunNodeMapper;

    @Resource
    private IJobWorkImportFileMapper jobWorkImportFileMapper;

    @Resource
    private IJobWorkExportFileMapper jobWorkExportFileMapper;

    /**
     * 分页列表
     */
    @Override
    public Map<String, Object> pageList(JobWorkNodePageParam param) {
        Page<JobWorkNodePo> page = jobWorkNodeMapper.selectPage(new Page<>((param.getStart() / param.getLength()) + 1, param.getLength()),
                Wrappers.lambdaQuery(JobWorkNodePo.class)
                        .eq(StrUtil.isNotBlank(param.getWorkId()), JobWorkNodePo::getWorkId, param.getWorkId())
                        .eq(StrUtil.isNotBlank(param.getNodeType()), JobWorkNodePo::getNodeType, param.getNodeType()));

        List<JobWorkPo> jobWorkPos = jobWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkPo.class));
        Map<String, String> workMap = new HashMap<>();
        if (CollUtil.isNotEmpty(jobWorkPos)) {
            workMap = jobWorkPos.stream()
                    .collect(Collectors.toMap(JobWorkPo::getWorkId, JobWorkPo::getWorkName));
        }
        // package result
        Map<String, String> finalWorkMap = workMap;
        List<String> nodeIdList = page.getRecords().stream().map(JobWorkNodePo::getNodeId).collect(Collectors.toList());
        List<JobWorkRunNodePo> runNodeList = CollUtil.isEmpty(nodeIdList) ? Collections.emptyList()
                : jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                .in(JobWorkRunNodePo::getNodeId, nodeIdList)
                .orderByDesc(JobWorkRunNodePo::getCreateTime));
        Map<String, JobWorkRunNodePo> runNodeMap = runNodeList.stream()
                .collect(Collectors.toMap(JobWorkRunNodePo::getNodeId, x -> x, (old, value) -> old));
        page.convert(jobWorkNodePo -> {
            JobWorkRunNodeVo jobWorkNodeVo = BeanUtil.toBean(jobWorkNodePo, JobWorkRunNodeVo.class);
            jobWorkNodeVo.setNodeTypeName(NodeTypeEnum.getValue(jobWorkNodePo.getNodeType()));
            jobWorkNodeVo.setWorkName(finalWorkMap.get(jobWorkNodePo.getWorkId()));
            JobWorkRunNodePo runNodePo = runNodeMap.get(jobWorkNodePo.getNodeId());
            fillRunNodeInfo(jobWorkNodeVo, runNodePo);
            return jobWorkNodeVo;
        });
        Map<String, Object> maps = new HashMap<>();
        // 总记录数
        maps.put("recordsTotal", page.getTotal());
        // 过滤后的总记录数
        maps.put("recordsFiltered", page.getTotal());
        // 分页列表
        maps.put("data", page.getRecords());
        return maps;
    }

    /**
     * 插入
     */
    @Override
    public int insert(JobWorkNodeParam param) {
        param.setUpdateTime(DateUtil.date());
        JobWorkNodePo jobWorkNodePo = BeanUtil.toBean(param, JobWorkNodePo.class);
        jobWorkNodePo.setRetryTimes(param.getRetryCount());
        int count = jobWorkNodeMapper.insert(jobWorkNodePo);
        param.setNodeId(jobWorkNodePo.getNodeId());
        saveFileConfig(param);
        return count;
    }

    /**
     * 修改
     */
    @Override
    public int update(JobWorkNodeParam param) {
        param.setUpdateTime(DateUtil.date());
        JobWorkNodePo jobWorkNodePo = BeanUtil.toBean(param, JobWorkNodePo.class);
        jobWorkNodePo.setRetryTimes(param.getRetryCount());
        int count = jobWorkNodeMapper.updateById(jobWorkNodePo);
        saveFileConfig(param);
        return count;
    }

    /**
     * 通过得到id得到对象
     */
    @Override
    public JobWorkNodeVo getModel(String id) {
        JobWorkNodePo jobWorkNodePo = jobWorkNodeMapper.selectById(id);
        if (jobWorkNodePo == null) {
            return null;
        }
        JobWorkNodeVo vo = BeanUtil.toBean(jobWorkNodePo, JobWorkNodeVo.class);
        vo.setRetryCount(jobWorkNodePo.getRetryTimes());
        fillFileConfig(vo, id);
        return vo;
    }

    /**
     * 删除
     */
    @Override
    public int delete(String id) {
        JobWorkNodePo jobWorkPo = jobWorkNodeMapper.selectById(id);
        if (jobWorkPo == null) {
            return 1;
        }
        return jobWorkNodeMapper.deleteById(id);
    }

    /**
     * 得到所有的发布的
     */
    @Override
    public List<JobWorkNodePo> getWorkNodeList(String workId) {
        return jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getNodeStatus, 1).eq(JobWorkNodePo::getWorkId, workId));
    }

    /**
     * 获取所有启用的作业
     */
    @Override
    public List<JobWorkPo> getAllWorkList() {
        return jobWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkPo.class));

    }

    /**
     * 获得所有作业节点关系
     */
    @Override
    public List<JobWorkNodeRelationVo> getWorkNodeRelationByWorkId(String workId) {
        List<JobWorkNodeRelationPo> relationList = jobWorkNodeRelationMapper.selectList(Wrappers.lambdaQuery(JobWorkNodeRelationPo.class)
                .eq(JobWorkNodeRelationPo::getWorkId, workId)
                .orderByDesc(JobWorkNodeRelationPo::getNodeId1));
        List<JobWorkNodePo> jobWorkNodePos = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, workId));
        Map<String, String> nodeMap = new HashMap<>();
        if (CollUtil.isNotEmpty(jobWorkNodePos)) {
            nodeMap = jobWorkNodePos.stream()
                    .collect(Collectors.toMap(JobWorkNodePo::getNodeId, JobWorkNodePo::getNodeName));
        }
        Map<String, String> finalNodeMap = nodeMap;
        return relationList.stream().map(x -> {
            JobWorkNodeRelationVo bean = BeanUtil.toBean(x, JobWorkNodeRelationVo.class);
            bean.setNodeName1(finalNodeMap.get(x.getNodeId1()));
            bean.setNodeName2(finalNodeMap.get(x.getNodeId2()));
            return bean;
        }).collect(Collectors.toList());
    }

    /**
     * 批量插入作业节点关系
     */
    @Override
    public int updateWorkNodeRelation(JobWorkNodeRelationParam param) {
        int insertCount = 0;
        jobWorkNodeRelationMapper.delete(Wrappers.lambdaQuery(JobWorkNodeRelationPo.class)
                .eq(JobWorkNodeRelationPo::getWorkId, param.getWorkId()));
        if (CollUtil.isEmpty(param.getNodeRelationList())) {
            return insertCount;
        }
        for (JobWorkNodeRelationParam.NodeRelation relation : param.getNodeRelationList()) {
            JobWorkNodeRelationPo bean = BeanUtil.toBean(relation, JobWorkNodeRelationPo.class);
            bean.setWorkId(param.getWorkId());
            insertCount += jobWorkNodeRelationMapper.insert(bean);
        }
        return insertCount;
    }


    /**
     * 获取作业节点关系
     */
    @Override
    public IPage<JobWorkRunNodeLogVo> logPageList(JobWorkNodeLogPageParam param) {
        IPage<JobWorkRunNodeLogPo> page = jobWorkRunNodeLogMapper
                .selectPage(new Page<>(param.getStart(), param.getLength()),
                Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                        .eq(StrUtil.isNotBlank(param.getWorkId()), JobWorkRunNodeLogPo::getWorkId, param.getWorkId())
                        .eq(StrUtil.isNotBlank(param.getNodeId()), JobWorkRunNodeLogPo::getNodeId, param.getNodeId())
                        .eq(StrUtil.isNotBlank(param.getRunNodeId()), JobWorkRunNodeLogPo::getRunNodeId, param.getRunNodeId())
                        .ge(param.getStartTime() != null, JobWorkRunNodeLogPo::getCreateTime, param.getStartTime())
                        .le(param.getEndTime() != null, JobWorkRunNodeLogPo::getCreateTime, param.getEndTime())
                        .orderByDesc(JobWorkRunNodeLogPo::getCreateTime)
                );
        if (CollUtil.isEmpty(page.getRecords())) {
            return page.convert(x -> BeanUtil.toBean(x, JobWorkRunNodeLogVo.class));
        }
        List<String> runNodeIdList = page.getRecords().stream().map(JobWorkRunNodeLogPo::getRunNodeId)
                .collect(Collectors.toList());
        List<JobWorkRunNodeLogDetailPo> jobWorkRunNodeLogDetailList =
                jobWorkRunNodeLogDetailMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodeLogDetailPo.class)
                .in(JobWorkRunNodeLogDetailPo::getRunNodeId, runNodeIdList));
        Map<String, List<JobWorkRunNodeLogDetailPo>> detailMap = new HashMap<>();
        if (CollUtil.isNotEmpty(jobWorkRunNodeLogDetailList)) {
            detailMap = jobWorkRunNodeLogDetailList.stream()
                    .collect(Collectors.groupingBy(JobWorkRunNodeLogDetailPo::getRunNodeId));
        }
        Map<String, List<JobWorkRunNodeLogDetailPo>> finalDetailMap = detailMap;
        return page.convert(x -> {
            JobWorkRunNodeLogVo vo = BeanUtil.toBean(x, JobWorkRunNodeLogVo.class);
            List<JobWorkRunNodeLogDetailPo> jobWorkRunNodeLogDetailPos = finalDetailMap.get(x.getRunNodeId());
            if (CollUtil.isNotEmpty(jobWorkRunNodeLogDetailPos)) {
                String jobDetail = jobWorkRunNodeLogDetailPos.stream().map(JobWorkRunNodeLogDetailPo::getHandleMsg)
                        .collect(Collectors.joining("<br>"));
                vo.setLogDetail(jobDetail);
            }
            return vo;
        });
    }

    /**
     * 获取作业节点
     */
    @Override
    public JobWorkNodePo getWorkNode(String workNodeId) {
        return jobWorkNodeMapper.selectById(workNodeId);
    }

    @Override
    public IPage<JobWorkRunNodeLogDetailPo> logDetailPageList(JobWorkNodeLogPageParam param) {
        return jobWorkRunNodeLogDetailMapper.selectPage(new Page<>(param.getStart(), param.getLength()),
                Wrappers.lambdaQuery(JobWorkRunNodeLogDetailPo.class)
                        .eq(StrUtil.isNotBlank(param.getWorkId()), JobWorkRunNodeLogDetailPo::getWorkId, param.getWorkId())
                        .eq(StrUtil.isNotBlank(param.getNodeId()), JobWorkRunNodeLogDetailPo::getNodeId, param.getNodeId())
                        .eq(StrUtil.isNotBlank(param.getRunNodeId()), JobWorkRunNodeLogDetailPo::getRunNodeId, param.getRunNodeId())
                        .ge(param.getStartTime() != null, JobWorkRunNodeLogDetailPo::getExecuteTime, param.getStartTime())
                        .le(param.getEndTime() != null, JobWorkRunNodeLogDetailPo::getExecuteTime, param.getEndTime())
                        .orderByDesc(JobWorkRunNodeLogDetailPo::getExecuteTime));
    }

    private void fillRunNodeInfo(JobWorkRunNodeVo vo, JobWorkRunNodePo runNodePo) {
        if (runNodePo == null) {
            return;
        }
        vo.setRunNodeId(runNodePo.getRunNodeId());
        vo.setRunWorkId(runNodePo.getRunWorkId());
        vo.setNodeRunStatus(runNodePo.getNodeRunStatus());
        vo.setNodeRunStatusName(FlowRunStatusEnum.getValueByCode(runNodePo.getNodeRunStatus()));
        vo.setTurnDate(runNodePo.getTurnDate());
        vo.setTurnDateText(runNodePo.getTurnDate() == null ? null : DateUtil.formatDate(runNodePo.getTurnDate()));
        vo.setRunNodeCreateTime(runNodePo.getCreateTime() == null ? null : DateUtil.formatDateTime(runNodePo.getCreateTime()));
        vo.setStartTime(runNodePo.getStartTime());
        vo.setEndTime(runNodePo.getEndTime());
        vo.setStartTimeText(runNodePo.getStartTime() == null ? null : runNodePo.getStartTime().toString());
        vo.setEndTimeText(runNodePo.getEndTime() == null ? null : runNodePo.getEndTime().toString());
        vo.setRetryTimes(runNodePo.getRetryTimes());
    }

    private void fillFileConfig(JobWorkNodeVo vo, String nodeId) {
        JobWorkImportFilePo importFilePo = jobWorkImportFileMapper.selectOne(Wrappers.lambdaQuery(JobWorkImportFilePo.class)
                .eq(JobWorkImportFilePo::getNodeId, nodeId)
                .last("limit 1"));
        if (importFilePo != null) {
            vo.setImportFileId(importFilePo.getImportFileId());
            vo.setImportFileName(importFilePo.getFileName());
            vo.setImportTableName(importFilePo.getImportTableName());
            vo.setImportTableFiled(importFilePo.getImportTableFiled());
            vo.setImportTableCondition(importFilePo.getImportTableCondition());
            vo.setImportFileCode(importFilePo.getFileCode());
            vo.setImportSep(importFilePo.getSep());
            vo.setImportAllUpdate(importFilePo.getAllUpdate());
            vo.setImportIsGzip(importFilePo.getIsGzip());
            vo.setImportFileNameParam(importFilePo.getFileNameParam());
        }

        JobWorkExportFilePo exportFilePo = jobWorkExportFileMapper.selectOne(Wrappers.lambdaQuery(JobWorkExportFilePo.class)
                .eq(JobWorkExportFilePo::getNodeId, nodeId)
                .last("limit 1"));
        if (exportFilePo != null) {
            vo.setExportFileId(exportFilePo.getExportFileId());
            vo.setExportFileName(exportFilePo.getFileName());
            vo.setExportTableName(exportFilePo.getExportTableName());
            vo.setExportTableFiled(exportFilePo.getExportTableFiled());
            vo.setExportTableCondition(exportFilePo.getExportTableCondition());
            vo.setExportFileCode(exportFilePo.getFileCode());
            vo.setExportSep(exportFilePo.getSep());
            vo.setExportAllUpdate(exportFilePo.getAllUpdate());
            vo.setExportIsGzip(exportFilePo.getIsGzip());
            vo.setExportFileNameParam(exportFilePo.getFileNameParam());
        }
    }

    private void saveFileConfig(JobWorkNodeParam param) {
        if (StrUtil.equals(param.getNodeType(), NodeTypeEnum.NODE_TYPE_FILE_TO_DB.getCode())) {
            JobWorkImportFilePo importFilePo = new JobWorkImportFilePo();
            importFilePo.setImportFileId(param.getImportFileId());
            importFilePo.setNodeId(param.getNodeId());
            importFilePo.setFileName(param.getImportFileName());
            importFilePo.setImportTableName(param.getImportTableName());
            importFilePo.setImportTableFiled(param.getImportTableFiled());
            importFilePo.setImportTableCondition(param.getImportTableCondition());
            importFilePo.setFileCode(param.getImportFileCode());
            importFilePo.setSep(param.getImportSep());
            importFilePo.setAllUpdate(param.getImportAllUpdate());
            importFilePo.setIsGzip(param.getImportIsGzip());
            importFilePo.setFileNameParam(param.getImportFileNameParam());
            saveImportFileConfig(importFilePo);
        }
        if (StrUtil.equals(param.getNodeType(), NodeTypeEnum.NODE_TYPE_DB_TO_FILE.getCode())) {
            JobWorkExportFilePo exportFilePo = new JobWorkExportFilePo();
            exportFilePo.setExportFileId(param.getExportFileId());
            exportFilePo.setNodeId(param.getNodeId());
            exportFilePo.setFileName(param.getExportFileName());
            exportFilePo.setExportTableName(param.getExportTableName());
            exportFilePo.setExportTableFiled(param.getExportTableFiled());
            exportFilePo.setExportTableCondition(param.getExportTableCondition());
            exportFilePo.setFileCode(param.getExportFileCode());
            exportFilePo.setSep(param.getExportSep());
            exportFilePo.setAllUpdate(param.getExportAllUpdate());
            exportFilePo.setIsGzip(param.getExportIsGzip());
            exportFilePo.setFileNameParam(param.getExportFileNameParam());
            saveExportFileConfig(exportFilePo);
        }
    }

    private void saveImportFileConfig(JobWorkImportFilePo importFilePo) {
        JobWorkImportFilePo oldPo = jobWorkImportFileMapper.selectOne(Wrappers.lambdaQuery(JobWorkImportFilePo.class)
                .eq(JobWorkImportFilePo::getNodeId, importFilePo.getNodeId())
                .last("limit 1"));
        if (oldPo == null) {
            jobWorkImportFileMapper.insert(importFilePo);
        } else {
            importFilePo.setImportFileId(oldPo.getImportFileId());
            jobWorkImportFileMapper.updateById(importFilePo);
        }
    }

    private void saveExportFileConfig(JobWorkExportFilePo exportFilePo) {
        JobWorkExportFilePo oldPo = jobWorkExportFileMapper.selectOne(Wrappers.lambdaQuery(JobWorkExportFilePo.class)
                .eq(JobWorkExportFilePo::getNodeId, exportFilePo.getNodeId())
                .last("limit 1"));
        if (oldPo == null) {
            jobWorkExportFileMapper.insert(exportFilePo);
        } else {
            exportFilePo.setExportFileId(oldPo.getExportFileId());
            jobWorkExportFileMapper.updateById(exportFilePo);
        }
    }


}
