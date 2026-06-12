package com.nbatch.job.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
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
import com.nbatch.job.admin.core.domain.po.JobWorkRunPo;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeRelationVo;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeVo;
import com.nbatch.job.admin.core.domain.vo.JobWorkRunNodeLogVo;
import com.nbatch.job.admin.core.domain.vo.JobWorkRunNodeVo;
import com.nbatch.job.core.enums.NodeTypeEnum;
import com.nbatch.job.admin.mapper.IJobWorkExportFileMapper;
import com.nbatch.job.admin.mapper.IJobWorkImportFileMapper;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeRelationMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeLogDetailMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeLogMapper;
import com.nbatch.job.admin.service.IJobWorkNodeService;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @description: 作业节点执行服务实现类
 * @author: Mr.ni
 * @date: 2025/11/13
 */
@Service
public class JobWorkNodeServiceImpl implements IJobWorkNodeService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
    private IJobWorkRunMapper jobWorkRunMapper;

    @Resource
    private IJobWorkImportFileMapper jobWorkImportFileMapper;

    @Resource
    private IJobWorkExportFileMapper jobWorkExportFileMapper;

    /**
     * 分页列表
     */
    @Override
    public Map<String, Object> pageList(JobWorkNodePageParam param) {
        Set<String> runStatusNodeIdSet = getLatestRunStatusNodeIdSet(param.getNodeRunStatus());
        Page<JobWorkNodePo> page = jobWorkNodeMapper.selectPage(new Page<>((param.getStart() / param.getLength()) + 1, param.getLength()),
                Wrappers.lambdaQuery(JobWorkNodePo.class)
                        .eq(StrUtil.isNotBlank(param.getWorkId()), JobWorkNodePo::getWorkId, param.getWorkId())
                        .eq(StrUtil.isNotBlank(param.getNodeType()), JobWorkNodePo::getNodeType, param.getNodeType())
                        .in(param.getNodeRunStatus() != null, JobWorkNodePo::getNodeId, runStatusNodeIdSet)
                        .like(StrUtil.isNotBlank(param.getNodeName()), JobWorkNodePo::getNodeName, param.getNodeName()));

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
            jobWorkNodeVo.setNodeTypeName(NodeTypeEnum.getValueByCode(jobWorkNodePo.getNodeType()));
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

    private Set<String> getLatestRunStatusNodeIdSet(Integer nodeRunStatus) {
        if (nodeRunStatus == null) {
            return Collections.emptySet();
        }
        List<JobWorkRunNodePo> runNodeList = jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                .orderByDesc(JobWorkRunNodePo::getCreateTime));
        if (CollUtil.isEmpty(runNodeList)) {
            return Collections.singleton("__none__");
        }
        Map<String, JobWorkRunNodePo> latestRunNodeMap = new LinkedHashMap<>();
        for (JobWorkRunNodePo runNodePo : runNodeList) {
            latestRunNodeMap.putIfAbsent(runNodePo.getNodeId(), runNodePo);
        }
        Set<String> nodeIdSet = latestRunNodeMap.values().stream()
                .filter(runNodePo -> runNodePo.getNodeRunStatus() != null
                        && runNodePo.getNodeRunStatus().equals(nodeRunStatus))
                .map(JobWorkRunNodePo::getNodeId)
                .collect(Collectors.toSet());
        return nodeIdSet.isEmpty() ? Collections.singleton("__none__") : nodeIdSet;
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

    @Override
    public ReturnT<Map<String, Object>> detail(String nodeId) {
        if (StrUtil.isBlank(nodeId)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "节点ID不能为空");
        }
        JobWorkNodePo nodePo = jobWorkNodeMapper.selectById(nodeId);
        if (nodePo == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "节点不存在");
        }
        JobWorkPo workPo = jobWorkMapper.selectById(nodePo.getWorkId());
        List<JobWorkRunNodePo> runNodeList = jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                .eq(JobWorkRunNodePo::getNodeId, nodeId)
                .orderByDesc(JobWorkRunNodePo::getCreateTime));

        List<Map<String, Object>> runList = runNodeList.stream().map(runNodePo -> {
            Map<String, Object> item = new HashMap<>();
            item.put("runNodeId", runNodePo.getRunNodeId());
            item.put("runWorkId", runNodePo.getRunWorkId());
            item.put("turnDate", runNodePo.getTurnDate() == null ? null : DateUtil.formatDate(runNodePo.getTurnDate()));
            item.put("nodeRunStatus", runNodePo.getNodeRunStatus());
            item.put("nodeRunStatusName", runNodePo.getNodeRunStatus() == null ? null : FlowRunStatusEnum.getValueByCode(runNodePo.getNodeRunStatus()));
            item.put("retryTimes", runNodePo.getRetryTimes());
            item.put("createTime", runNodePo.getCreateTime() == null ? null : DateUtil.formatDateTime(runNodePo.getCreateTime()));
            item.put("startTime", runNodePo.getStartTime() == null ? null : DATE_TIME_FORMATTER.format(runNodePo.getStartTime()));
            item.put("endTime", runNodePo.getEndTime() == null ? null : DATE_TIME_FORMATTER.format(runNodePo.getEndTime()));
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> detail = new HashMap<>();
        detail.put("nodeId", nodePo.getNodeId());
        detail.put("nodeName", nodePo.getNodeName());
        detail.put("nodeDesc", nodePo.getNodeDesc());
        detail.put("workId", nodePo.getWorkId());
        detail.put("workName", workPo == null ? null : workPo.getWorkName());
        detail.put("nodeTypeName", NodeTypeEnum.getValueByCode(nodePo.getNodeType()));
        detail.put("dbType", nodePo.getDbType());
        detail.put("nodeStatusName", nodePo.getNodeStatus() != null && nodePo.getNodeStatus() == 1 ? "启用" : "停用");
        detail.put("executeHandler", nodePo.getExecuteHandler());
        detail.put("scriptType", nodePo.getScriptType());
        detail.put("errorStrategy", nodePo.getErrorStrategy());
        detail.put("retryTimes", nodePo.getRetryTimes());
        detail.put("executeContent", nodePo.getExecuteContent());
        detail.put("executeContentParam", nodePo.getExecuteContentParam());
        detail.put("runCount", runNodeList.size());
        detail.put("runList", runList);
        return ReturnT.success(detail);
    }

    /**
     * 删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int delete(String id) {
        JobWorkNodePo jobWorkPo = jobWorkNodeMapper.selectById(id);
        if (jobWorkPo == null) {
            return 1;
        }
        List<JobWorkRunNodePo> runNodeList = jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                .eq(JobWorkRunNodePo::getNodeId, id));
        if (CollUtil.isNotEmpty(runNodeList)) {
            List<String> runNodeIdList = runNodeList.stream()
                    .map(JobWorkRunNodePo::getRunNodeId)
                    .collect(Collectors.toList());
            jobWorkRunNodeLogDetailMapper.delete(Wrappers.lambdaQuery(JobWorkRunNodeLogDetailPo.class)
                    .in(JobWorkRunNodeLogDetailPo::getRunNodeId, runNodeIdList));
            jobWorkRunNodeLogMapper.delete(Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                    .in(JobWorkRunNodeLogPo::getRunNodeId, runNodeIdList));
            jobWorkRunNodeMapper.delete(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                    .eq(JobWorkRunNodePo::getNodeId, id));
        }
        jobWorkNodeRelationMapper.delete(Wrappers.lambdaQuery(JobWorkNodeRelationPo.class)
                .eq(JobWorkNodeRelationPo::getNodeId1, id)
                .or()
                .eq(JobWorkNodeRelationPo::getNodeId2, id));
        jobWorkImportFileMapper.delete(Wrappers.lambdaQuery(JobWorkImportFilePo.class)
                .eq(JobWorkImportFilePo::getNodeId, id));
        jobWorkExportFileMapper.delete(Wrappers.lambdaQuery(JobWorkExportFilePo.class)
                .eq(JobWorkExportFilePo::getNodeId, id));
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
        Date startTime = parseLogStartTime(param.getStartTime());
        Date endTime = parseLogEndTime(param.getEndTime());
        IPage<JobWorkRunNodeLogPo> page = jobWorkRunNodeLogMapper
                .selectPage(new Page<>(param.getStart(), param.getLength()),
                Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                        .eq(StrUtil.isNotBlank(param.getWorkId()), JobWorkRunNodeLogPo::getWorkId, param.getWorkId())
                        .eq(StrUtil.isNotBlank(param.getNodeId()), JobWorkRunNodeLogPo::getNodeId, param.getNodeId())
                        .eq(StrUtil.isNotBlank(param.getRunNodeId()), JobWorkRunNodeLogPo::getRunNodeId, param.getRunNodeId())
                        .ge(startTime != null, JobWorkRunNodeLogPo::getCreateTime, startTime)
                        .le(endTime != null, JobWorkRunNodeLogPo::getCreateTime, endTime)
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
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> rerunNode(String runNodeId) {
        return rerunNodeInternal(runNodeId, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> rerunNodeFromInitTurnDate(String runNodeId) {
        return rerunNodeInternal(runNodeId, true);
    }

    private ReturnT<String> rerunNodeInternal(String runNodeId, boolean fromInitTurnDate) {
        if (StrUtil.isBlank(runNodeId)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "运行节点ID不能为空");
        }

        JobWorkRunNodePo runNodePo = jobWorkRunNodeMapper.selectById(runNodeId);
        if (runNodePo == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "运行节点不存在");
        }

        JobWorkRunPo runWorkPo = jobWorkRunMapper.selectById(runNodePo.getRunWorkId());
        if (runWorkPo == null) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "运行作业不存在");
        }

        JobWorkRunPo targetRunWorkPo = runWorkPo;
        if (fromInitTurnDate) {
            JobWorkPo jobWorkPo = jobWorkMapper.selectById(runWorkPo.getWorkId());
            if (jobWorkPo == null) {
                return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业不存在");
            }
            java.util.Date initTurnDate = resolveInitTurnDate(jobWorkPo);
            targetRunWorkPo = jobWorkRunMapper.selectOne(Wrappers.lambdaQuery(JobWorkRunPo.class)
                    .eq(JobWorkRunPo::getWorkId, runWorkPo.getWorkId())
                    .eq(JobWorkRunPo::getTurnDate, initTurnDate)
                    .last("LIMIT 1"));
            if (targetRunWorkPo == null) {
                ReturnT<JobWorkRunPo> initResult = initRunWorkForTurnDate(jobWorkPo, initTurnDate);
                if (initResult.getCode() != HandleCodeConstant.HANDLE_CODE_SUCCESS) {
                    return new ReturnT<>(initResult.getCode(), initResult.getMsg());
                }
                targetRunWorkPo = initResult.getContent();
            }
        }

        List<JobWorkNodePo> nodeList = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, runWorkPo.getWorkId()));
        if (CollUtil.isEmpty(nodeList)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "作业节点不存在");
        }

        Map<String, JobWorkNodePo> nodeMap = nodeList.stream()
                .collect(Collectors.toMap(JobWorkNodePo::getNodeId, x -> x, (oldValue, newValue) -> oldValue));
        if (!nodeMap.containsKey(runNodePo.getNodeId())) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "运行节点不属于当前作业");
        }

        List<JobWorkNodeRelationPo> relationList = jobWorkNodeRelationMapper.selectList(Wrappers.lambdaQuery(JobWorkNodeRelationPo.class)
                .eq(JobWorkNodeRelationPo::getWorkId, runWorkPo.getWorkId()));
        Map<String, List<String>> relationMap = buildNextNodeMap(relationList);
        Set<String> rerunNodeIdSet = collectRerunNodeIdSet(runNodePo.getNodeId(), relationMap);

        if (fromInitTurnDate) {
            deleteRunWorkAfterTurnDate(runWorkPo.getWorkId(), targetRunWorkPo.getTurnDate());
        }

        List<JobWorkRunNodePo> runNodeList = jobWorkRunNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                .eq(JobWorkRunNodePo::getRunWorkId, targetRunWorkPo.getRunWorkId())
                .in(JobWorkRunNodePo::getNodeId, rerunNodeIdSet));
        if (CollUtil.isEmpty(runNodeList)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "当前运行作业没有可重跑节点");
        }

        for (JobWorkRunNodePo runNode : runNodeList) {
            JobWorkNodePo workNodePo = nodeMap.get(runNode.getNodeId());
            jobWorkRunNodeMapper.update(null, Wrappers.lambdaUpdate(JobWorkRunNodePo.class)
                    .set(JobWorkRunNodePo::getNodeRunStatus, FlowRunStatusEnum.WAIT.getCode())
                    .set(JobWorkRunNodePo::getStartTime, null)
                    .set(JobWorkRunNodePo::getEndTime, null)
                    .set(JobWorkRunNodePo::getRetryTimes, workNodePo == null ? runNode.getRetryTimes() : workNodePo.getRetryTimes())
                    .eq(JobWorkRunNodePo::getRunNodeId, runNode.getRunNodeId()));
        }

        JobWorkRunPo updateRunWorkPo = new JobWorkRunPo();
        updateRunWorkPo.setRunWorkId(targetRunWorkPo.getRunWorkId());
        updateRunWorkPo.setRunWorkStatus(FlowRunStatusEnum.WAIT.getCode());
        jobWorkRunMapper.updateById(updateRunWorkPo);

        return ReturnT.SUCCESS;
    }

    private java.util.Date resolveInitTurnDate(JobWorkPo jobWorkPo) {
        java.util.Date initTurnDate = jobWorkPo.getInitTurnDate() == null
                ? DateUtil.parseDate(DateUtil.today())
                : DateUtil.parseDate(DateUtil.formatDate(jobWorkPo.getInitTurnDate()));
        if (jobWorkPo.getInitTurnDate() == null) {
            JobWorkPo updateJobWorkPo = new JobWorkPo();
            updateJobWorkPo.setWorkId(jobWorkPo.getWorkId());
            updateJobWorkPo.setInitTurnDate(initTurnDate);
            jobWorkMapper.updateById(updateJobWorkPo);
            jobWorkPo.setInitTurnDate(initTurnDate);
        }
        return initTurnDate;
    }

    private ReturnT<JobWorkRunPo> initRunWorkForTurnDate(JobWorkPo jobWorkPo, java.util.Date turnDate) {
        List<JobWorkNodePo> nodeList = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, jobWorkPo.getWorkId()));
        if (CollUtil.isEmpty(nodeList)) {
            return new ReturnT<>(HandleCodeConstant.HANDLE_CODE_FAIL, "该运行作业没有运行节点");
        }
        JobWorkRunPo jobWorkRunPo = new JobWorkRunPo();
        jobWorkRunPo.setRunWorkId(IdUtil.getSnowflakeNextIdStr());
        jobWorkRunPo.setWorkId(jobWorkPo.getWorkId());
        jobWorkRunPo.setRunWorkStatus(FlowRunStatusEnum.WAIT.getCode());
        jobWorkRunPo.setWorkType(jobWorkPo.getWorkType());
        jobWorkRunPo.setCreateTime(DateUtil.date());
        jobWorkRunPo.setTurnDate(turnDate);
        jobWorkRunMapper.insert(jobWorkRunPo);

        for (JobWorkNodePo nodePo : nodeList) {
            JobWorkRunNodePo runNodePo = new JobWorkRunNodePo();
            runNodePo.setRunNodeId(IdUtil.getSnowflakeNextIdStr());
            runNodePo.setRunWorkId(jobWorkRunPo.getRunWorkId());
            runNodePo.setWorkId(jobWorkPo.getWorkId());
            runNodePo.setNodeId(nodePo.getNodeId());
            runNodePo.setNodeRunStatus(FlowRunStatusEnum.WAIT.getCode());
            runNodePo.setTurnDate(turnDate);
            runNodePo.setCreateTime(DateUtil.date());
            runNodePo.setErrorStrategy(nodePo.getErrorStrategy());
            runNodePo.setRetryTimes(nodePo.getRetryTimes());
            jobWorkRunNodeMapper.insert(runNodePo);
        }
        return ReturnT.success(jobWorkRunPo);
    }

    @Override
    public IPage<JobWorkRunNodeLogDetailPo> logDetailPageList(JobWorkNodeLogPageParam param) {
        Date startTime = parseLogStartTime(param.getStartTime());
        Date endTime = parseLogEndTime(param.getEndTime());
        return jobWorkRunNodeLogDetailMapper.selectPage(new Page<>(param.getStart(), param.getLength()),
                Wrappers.lambdaQuery(JobWorkRunNodeLogDetailPo.class)
                        .eq(StrUtil.isNotBlank(param.getWorkId()), JobWorkRunNodeLogDetailPo::getWorkId, param.getWorkId())
                        .eq(StrUtil.isNotBlank(param.getNodeId()), JobWorkRunNodeLogDetailPo::getNodeId, param.getNodeId())
                        .eq(StrUtil.isNotBlank(param.getRunNodeId()), JobWorkRunNodeLogDetailPo::getRunNodeId, param.getRunNodeId())
                        .ge(startTime != null, JobWorkRunNodeLogDetailPo::getExecuteTime, startTime)
                        .le(endTime != null, JobWorkRunNodeLogDetailPo::getExecuteTime, endTime)
                        .orderByDesc(JobWorkRunNodeLogDetailPo::getExecuteTime));
    }

    private Date parseLogStartTime(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return value.length() <= 10 ? DateUtil.beginOfDay(DateUtil.parseDate(value)) : DateUtil.parseDateTime(value);
    }

    private Date parseLogEndTime(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        Date parsed = value.length() <= 10 ? DateUtil.parseDate(value) : DateUtil.parseDateTime(value);
        if (value.length() <= 10 || value.endsWith("00:00:00")) {
            return DateUtil.endOfDay(parsed);
        }
        return parsed;
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
        vo.setStartTimeText(runNodePo.getStartTime() == null ? null : DATE_TIME_FORMATTER.format(runNodePo.getStartTime()));
        vo.setEndTimeText(runNodePo.getEndTime() == null ? null : DATE_TIME_FORMATTER.format(runNodePo.getEndTime()));
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

    private Map<String, List<String>> buildNextNodeMap(List<JobWorkNodeRelationPo> relationList) {
        Map<String, List<String>> relationMap = new HashMap<>();
        if (CollUtil.isEmpty(relationList)) {
            return relationMap;
        }
        return relationList.stream()
                .collect(Collectors.groupingBy(JobWorkNodeRelationPo::getNodeId2,
                        Collectors.mapping(JobWorkNodeRelationPo::getNodeId1, Collectors.toList())));
    }

    private Set<String> collectRerunNodeIdSet(String startNodeId, Map<String, List<String>> relationMap) {
        Set<String> rerunNodeIdSet = new HashSet<>();
        Deque<String> nodeQueue = new ArrayDeque<>();
        nodeQueue.add(startNodeId);
        while (!nodeQueue.isEmpty()) {
            String currentNodeId = nodeQueue.pollFirst();
            if (!rerunNodeIdSet.add(currentNodeId)) {
                continue;
            }
            List<String> nextNodeIdList = relationMap.get(currentNodeId);
            if (CollUtil.isNotEmpty(nextNodeIdList)) {
                nodeQueue.addAll(nextNodeIdList);
            }
        }
        return rerunNodeIdSet;
    }

    private void deleteRunWorkAfterTurnDate(String workId, java.util.Date turnDate) {
        List<JobWorkRunPo> deleteRunWorkList = jobWorkRunMapper.selectList(Wrappers.lambdaQuery(JobWorkRunPo.class)
                .eq(JobWorkRunPo::getWorkId, workId)
                .gt(JobWorkRunPo::getTurnDate, turnDate));
        if (CollUtil.isEmpty(deleteRunWorkList)) {
            return;
        }
        List<String> runWorkIdList = deleteRunWorkList.stream()
                .map(JobWorkRunPo::getRunWorkId)
                .collect(Collectors.toList());
        jobWorkRunNodeLogDetailMapper.delete(Wrappers.lambdaQuery(JobWorkRunNodeLogDetailPo.class)
                .in(JobWorkRunNodeLogDetailPo::getRunWorkId, runWorkIdList));
        jobWorkRunNodeLogMapper.delete(Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                .in(JobWorkRunNodeLogPo::getRunWorkId, runWorkIdList));
        jobWorkRunNodeMapper.delete(Wrappers.lambdaQuery(JobWorkRunNodePo.class)
                .in(JobWorkRunNodePo::getRunWorkId, runWorkIdList));
        jobWorkRunMapper.delete(Wrappers.lambdaQuery(JobWorkRunPo.class)
                .in(JobWorkRunPo::getRunWorkId, runWorkIdList));
    }


}
