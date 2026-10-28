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
import com.nbatch.job.admin.core.domain.po.*;
import com.nbatch.job.admin.core.domain.vo.JobWorkNodeVo;
import com.nbatch.job.admin.core.domain.vo.JobWorkRunNodeLogVo;
import com.nbatch.job.admin.core.enums.FlowElementTypeEnum;
import com.nbatch.job.admin.core.helper.FlowGroupHelper;
import com.nbatch.job.admin.mapper.*;
import com.nbatch.job.admin.service.IJobWorkNodeService;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.NodeTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 作业节点执行服务实现类
 */
@Service
public class JobWorkNodeServiceImpl implements IJobWorkNodeService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_SCRIPT_CONTENT_BYTES = 60_000;

    @Resource
    private IJobWorkNodeMapper jobWorkNodeMapper;

    @Resource
    private IJobWorkMapper jobWorkMapper;

    @Resource
    private IJobWorkFlowDependencyMapper jobWorkFlowDependencyMapper;

    @Resource
    private IJobWorkRunNodeLogMapper jobWorkRunNodeLogMapper;

    @Resource
    private IJobWorkRunNodeLogDetailMapper jobWorkRunNodeLogDetailMapper;

    @Resource
    private IJobWorkImportFileMapper jobWorkImportFileMapper;

    @Resource
    private IJobWorkExportFileMapper jobWorkExportFileMapper;

    @Resource
    private FlowGroupHelper flowGroupHelper;

    /**
     * 分页列表
     */
    @Override
    public Map<String, Object> pageList(JobWorkNodePageParam param) {
        Page<JobWorkNodePo> page = jobWorkNodeMapper.selectPage(new Page<>((param.getStart() / param.getLength()) + 1, param.getLength()),
                Wrappers.lambdaQuery(JobWorkNodePo.class)
                        .eq(StrUtil.isNotBlank(param.getWorkId()), JobWorkNodePo::getWorkId, param.getWorkId())
                        .eq(StrUtil.isNotBlank(param.getNodeType()), JobWorkNodePo::getNodeType, param.getNodeType())
                        .eq(param.getNodeRunStatus() != null, JobWorkNodePo::getNodeRunStatus, param.getNodeRunStatus())
                        .apply(StrUtil.isNotBlank(param.getNodeName()), "LOWER(node_name) LIKE CONCAT('%', LOWER({0}), '%')", param.getNodeName()));
        //.like(StrUtil.isNotBlank(param.getNodeName()), JobWorkNodePo::getNodeName, param.getNodeName()));

        List<JobWorkPo> jobWorkPos = jobWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkPo.class));
        Map<String, String> workMap = new HashMap<>();
        // 作业类型map
        Map<String, Integer> workTypeMap = new HashMap<>();
        if (CollUtil.isNotEmpty(jobWorkPos)) {
            workMap = jobWorkPos.stream()
                    .collect(Collectors.toMap(JobWorkPo::getWorkId, JobWorkPo::getWorkName));
            workTypeMap = jobWorkPos.stream()
                    .filter(work -> work.getWorkType() != null)
                    .collect(Collectors.toMap(JobWorkPo::getWorkId, JobWorkPo::getWorkType,
                            (oldValue, newValue) -> oldValue));
        }
        // 封装返回结果
        Map<String, String> finalWorkMap = workMap;
        Map<String, Integer> finalWorkTypeMap = workTypeMap;
        page.convert(jobWorkNodePo -> {
            JobWorkNodeVo jobWorkNodeVo = BeanUtil.toBean(jobWorkNodePo, JobWorkNodeVo.class);
            jobWorkNodeVo.setNodeTypeName(NodeTypeEnum.getValueByCode(jobWorkNodePo.getNodeType()));
            jobWorkNodeVo.setWorkName(finalWorkMap.get(jobWorkNodePo.getWorkId()));
            jobWorkNodeVo.setWorkType(finalWorkTypeMap.get(jobWorkNodePo.getWorkId()));
            fillWorkNodeVo(jobWorkNodeVo, jobWorkNodePo);
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
        normalizeScriptContent(param);
        param.setUpdateTime(DateUtil.date());
        JobWorkNodePo jobWorkNodePo = BeanUtil.toBean(param, JobWorkNodePo.class);
        // 如果没有组则归属于默认组
        jobWorkNodePo.setGroupId(flowGroupHelper.resolveGroupId(param.getWorkId(), param.getGroupId()));
        // 设置重试次数
        jobWorkNodePo.setRetryTimes(normalizeRetryTimes(param.getRetryCount()));
        jobWorkNodePo.setNodeRunStatus(FlowRunStatusEnum.UNINITIALIZED.getCode());
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
        JobWorkNodePo currentNode = jobWorkNodeMapper.selectById(param.getNodeId());
        if (currentNode == null) {
            return 0;
        }
        param.setNodeType(StrUtil.blankToDefault(param.getNodeType(), currentNode.getNodeType()));
        normalizeScriptContent(param);
        String workId = StrUtil.blankToDefault(param.getWorkId(), currentNode.getWorkId());
        param.setWorkId(workId);
        if (StrUtil.isBlank(param.getGroupId())) {
            param.setGroupId(StrUtil.blankToDefault(currentNode.getGroupId(),
                    flowGroupHelper.resolveGroupId(workId, null)));
        } else {
            param.setGroupId(flowGroupHelper.resolveGroupId(workId, param.getGroupId()));
        }
        JobWorkNodePo jobWorkNodePo = BeanUtil.toBean(param, JobWorkNodePo.class);
        jobWorkNodePo.setRetryTimes(normalizeRetryTimes(param.getRetryCount()));
        int count = jobWorkNodeMapper.updateById(jobWorkNodePo);
        saveFileConfig(param);
        return count;
    }

    /**
     * 正常化重试次数
     */
    private Integer normalizeRetryTimes(Integer retryTimes) {
        if (retryTimes == null) {
            return 0;
        }
        return retryTimes < -1 ? -1 : retryTimes;
    }

    /**
     * 脚本统一以 LF 保存，避免不同操作系统的换行符在回显和执行时产生差异。
     */
    private void normalizeScriptContent(JobWorkNodeParam param) {
        if (!StrUtil.equals(NodeTypeEnum.NODE_TYPE_SCRIPT.getCode(), param.getNodeType())
                || param.getExecuteContent() == null) {
            return;
        }
        String content = param.getExecuteContent();
        if (content.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("脚本内容不能包含 NUL 字符");
        }
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.getBytes(StandardCharsets.UTF_8).length > MAX_SCRIPT_CONTENT_BYTES) {
            throw new IllegalArgumentException("脚本内容不能超过 60000 个 UTF-8 字节");
        }
        param.setExecuteContent(normalized);
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
     * 详情
     */
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
        // 得到各个节点执行日志的详情
        List<Map<String, Object>> runList = buildNodeRunHistoryFromLogs(nodeId, nodePo);
        if (CollUtil.isEmpty(runList) && nodePo.getNodeRunStatus() != null) {
            runList = Collections.singletonList(buildCurrentNodeRunItem(nodePo));
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("nodeId", nodePo.getNodeId());
        detail.put("nodeName", nodePo.getNodeName());
        detail.put("nodeDesc", nodePo.getNodeDesc());
        detail.put("workId", nodePo.getWorkId());
        detail.put("workName", workPo == null ? null : workPo.getWorkName());
        detail.put("nodeTypeName", NodeTypeEnum.getValueByCode(nodePo.getNodeType()));
        detail.put("dbType", nodePo.getDbType());
        detail.put("executeHandler", nodePo.getExecuteHandler());
        detail.put("scriptType", nodePo.getScriptType());
        detail.put("errorStrategy", nodePo.getErrorStrategy());
        detail.put("retryTimes", nodePo.getRetryTimes());
        detail.put("executeContent", nodePo.getExecuteContent());
        detail.put("executeContentParam", nodePo.getExecuteContentParam());
        detail.put("runCount", runList.size());
        detail.put("runList", runList);
        return ReturnT.success(detail);
    }

    /**
     * 构建当前节点运行项
     *
     * @param runNodePo 运行节点
     * @return 当前节点运行项
     */
    private Map<String, Object> buildCurrentNodeRunItem(JobWorkNodePo runNodePo) {
        Map<String, Object> item = new HashMap<>();
        item.put("nodeId", runNodePo.getNodeId());
        item.put("workId", runNodePo.getWorkId());
        item.put("turnDate", runNodePo.getTurnDate() == null ? null : DateUtil.formatDate(runNodePo.getTurnDate()));
        item.put("nodeRunStatus", runNodePo.getNodeRunStatus());
        item.put("nodeRunStatusName", runNodePo.getNodeRunStatus() == null
                ? null : FlowRunStatusEnum.getValueByCode(runNodePo.getNodeRunStatus()));
        item.put("retryTimes", runNodePo.getCurrentRetryTimes());
        fillRunLogTimes(item, findLatestRunNodeLog(runNodePo));
        return item;
    }

    /**
     * 查询最新运行节点日志
     *
     * @param runNodePo 运行节点
     * @return 最新运行节点日志
     */
    private JobWorkRunNodeLogPo findLatestRunNodeLog(JobWorkNodePo runNodePo) {
        return jobWorkRunNodeLogMapper.selectOne(Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                .eq(JobWorkRunNodeLogPo::getWorkId, runNodePo.getWorkId())
                .eq(JobWorkRunNodeLogPo::getNodeId, runNodePo.getNodeId())
                .eq(runNodePo.getTurnDate() != null, JobWorkRunNodeLogPo::getTurnDate, runNodePo.getTurnDate())
                .orderByDesc(JobWorkRunNodeLogPo::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 填充运行日志时间
     *
     * @param item  运行项
     * @param logPo 运行日志
     */
    private void fillRunLogTimes(Map<String, Object> item, JobWorkRunNodeLogPo logPo) {
        if (logPo == null) {
            item.put("createTime", null);
            item.put("executeStartTime", null);
            item.put("executeEndTime", null);
            item.put("callBackTime", null);
            return;
        }
        item.put("createTime", logPo.getCreateTime() == null ? null : DATE_TIME_FORMATTER.format(logPo.getCreateTime()));
        item.put("executeStartTime", logPo.getExecuteStartTime() == null ? null
                : DATE_TIME_FORMATTER.format(logPo.getExecuteStartTime()));
        item.put("executeEndTime", logPo.getExecuteEndTime() == null ? null
                : DATE_TIME_FORMATTER.format(logPo.getExecuteEndTime()));
        item.put("callBackTime", logPo.getCallBackTime() == null ? null
                : DATE_TIME_FORMATTER.format(logPo.getCallBackTime()));
    }

    /**
     * 构建节点运行历史
     *
     * @param nodeId         节点ID
     * @param currentRunNode 当前运行节点
     * @return 节点运行历史
     */
    private List<Map<String, Object>> buildNodeRunHistoryFromLogs(String nodeId,
                                                                  JobWorkNodePo currentRunNode) {
        // 得到所有执行日志
        List<JobWorkRunNodeLogPo> logList = jobWorkRunNodeLogMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                .eq(JobWorkRunNodeLogPo::getNodeId, nodeId)
                .orderByDesc(JobWorkRunNodeLogPo::getTurnDate)
                .orderByDesc(JobWorkRunNodeLogPo::getCreateTime));
        if (CollUtil.isEmpty(logList)) {
            return Collections.emptyList();
        }
        // 得到翻牌节点当中最新的那一条日志记录
        Map<String, JobWorkRunNodeLogPo> latestTurnLogMap = new LinkedHashMap<>();
        for (JobWorkRunNodeLogPo logPo : logList) {
            String turnDate = logPo.getTurnDate() == null ? "未记录" : DateUtil.formatDate(logPo.getTurnDate());
            // 如果key已经存在，那么不会再put而且返回旧值
            latestTurnLogMap.putIfAbsent(turnDate, logPo);
        }
        List<Map<String, Object>> runList = new ArrayList<>();
        for (Map.Entry<String, JobWorkRunNodeLogPo> entry : latestTurnLogMap.entrySet()) {
            JobWorkRunNodeLogPo logPo = entry.getValue();
            // 该日志是否是当前执行节点的日志
            boolean currentTurn = currentRunNode != null
                    && StrUtil.equals(formatTurnDate(currentRunNode.getTurnDate()), entry.getKey());
            Map<String, Object> item = new HashMap<>();
            item.put("nodeId", logPo.getNodeId());
            item.put("workId", logPo.getWorkId());
            item.put("turnDate", entry.getKey());
            // 如果该日志是当前执行节点的日志，则从当前执行节点中获取数据，否则从日志中获取数据
            if (currentTurn) {
                item.put("nodeRunStatus", currentRunNode.getNodeRunStatus());
                item.put("nodeRunStatusName", currentRunNode.getNodeRunStatus() == null ? null : FlowRunStatusEnum.getValueByCode(currentRunNode.getNodeRunStatus()));
                item.put("retryTimes", currentRunNode.getCurrentRetryTimes());
                fillRunLogTimes(item, logPo);
            } else {
                item.put("nodeRunStatus", null);
                item.put("nodeRunStatusName", logPo.getHandleCode() == null ? null
                        : (logPo.getHandleCode() == HandleCodeConstant.HANDLE_CODE_SUCCESS ? "执行完毕"
                           : (logPo.getHandleCode() == 0 ? "已下发" : "执行异常")));
                item.put("retryTimes", null);
                fillRunLogTimes(item, logPo);
            }
            runList.add(item);
        }
        return runList;
    }

    private String formatTurnDate(Date turnDate) {
        return turnDate == null ? "未记录" : DateUtil.formatDate(turnDate);
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
        // 删除依赖节点信息
        jobWorkFlowDependencyMapper.delete(Wrappers.lambdaQuery(JobWorkFlowDependencyPo.class)
                .eq(JobWorkFlowDependencyPo::getWorkId, jobWorkPo.getWorkId())
                .and(wrapper -> wrapper
                        .eq(JobWorkFlowDependencyPo::getPredecessorType, FlowElementTypeEnum.NODE.name())
                        .eq(JobWorkFlowDependencyPo::getPredecessorId, id)
                        .or()
                        .eq(JobWorkFlowDependencyPo::getSuccessorType, FlowElementTypeEnum.NODE.name())
                        .eq(JobWorkFlowDependencyPo::getSuccessorId, id)));
        // 删除与节点相关联的导入文件信息
        jobWorkImportFileMapper.delete(Wrappers.lambdaQuery(JobWorkImportFilePo.class)
                .eq(JobWorkImportFilePo::getNodeId, id));
        // 删除与节点相关联的导出文件信息
        jobWorkExportFileMapper.delete(Wrappers.lambdaQuery(JobWorkExportFilePo.class)
                .eq(JobWorkExportFilePo::getNodeId, id));
        return jobWorkNodeMapper.deleteById(id);
    }

    /**
     * 获取所有启用的作业
     */
    @Override
    public List<JobWorkPo> getAllWorkList() {
        return jobWorkMapper.selectList(Wrappers.lambdaQuery(JobWorkPo.class));

    }

    /**
     * 获取作业节点关系
     */
    @Override
    public IPage<JobWorkRunNodeLogVo> logPageList(JobWorkNodeLogPageParam param) {
        // 得到节点运行日志
        Date startTime = parseLogStartTime(param.getStartTime());
        Date endTime = parseLogEndTime(param.getEndTime());
        List<String> groupNodeIds = findLogScopeNodeIds(param);
        if (StrUtil.isNotBlank(param.getGroupId()) && CollUtil.isEmpty(groupNodeIds)) {
            return new Page<>(param.getStart(), param.getLength());
        }
        IPage<JobWorkRunNodeLogPo> page = jobWorkRunNodeLogMapper
                .selectPage(new Page<>(param.getStart(), param.getLength()),
                        Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                                .eq(StrUtil.isNotBlank(param.getWorkId()), JobWorkRunNodeLogPo::getWorkId, param.getWorkId())
                                .eq(StrUtil.isNotBlank(param.getNodeId()), JobWorkRunNodeLogPo::getNodeId, param.getNodeId())
                                .in(CollUtil.isNotEmpty(groupNodeIds), JobWorkRunNodeLogPo::getNodeId, groupNodeIds)
                                .eq(StrUtil.isNotBlank(param.getNodeLogId()), JobWorkRunNodeLogPo::getNodeLogId, param.getNodeLogId())
                                .ge(startTime != null, JobWorkRunNodeLogPo::getCreateTime, startTime)
                                .le(endTime != null, JobWorkRunNodeLogPo::getCreateTime, endTime)
                                .orderByDesc(JobWorkRunNodeLogPo::getCreateTime)
                );
        if (CollUtil.isEmpty(page.getRecords())) {
            return page.convert(x -> {
                JobWorkRunNodeLogVo vo = BeanUtil.toBean(x, JobWorkRunNodeLogVo.class);
                vo.setExecuteStartTime(x.getExecuteStartTime() == null ? x.getCreateTime() : x.getExecuteStartTime());
                vo.setExecuteEndTime(x.getExecuteEndTime() == null ? x.getCallBackTime() : x.getExecuteEndTime());
                return vo;
            });
        }
        Map<String, String> nodeNameMap = findNodeNameMap(page.getRecords().stream()
                .map(JobWorkRunNodeLogPo::getNodeId)
                .collect(Collectors.toList()));
        List<String> nodeLogIdList = page.getRecords().stream().map(JobWorkRunNodeLogPo::getNodeLogId)
                .collect(Collectors.toList());
        // 得到节点详细日志
        List<JobWorkRunNodeLogDetailPo> jobWorkRunNodeLogDetailList =
                jobWorkRunNodeLogDetailMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodeLogDetailPo.class)
                        .in(JobWorkRunNodeLogDetailPo::getNodeLogId, nodeLogIdList));
        Map<String, List<JobWorkRunNodeLogDetailPo>> detailMap = new HashMap<>();
        // key = nodeLogId:turnDate,value = JobWorkRunNodeLogDetailPo列表
        if (CollUtil.isNotEmpty(jobWorkRunNodeLogDetailList)) {
            detailMap = jobWorkRunNodeLogDetailList.stream()
                    .collect(Collectors.groupingBy(x -> buildRunNodeLogDetailKey(x.getNodeLogId(), x.getTurnDate())));
        }
        Map<String, List<JobWorkRunNodeLogDetailPo>> finalDetailMap = detailMap;
        return page.convert(x -> {
            JobWorkRunNodeLogVo vo = BeanUtil.toBean(x, JobWorkRunNodeLogVo.class);
            vo.setNodeName(nodeNameMap.getOrDefault(x.getNodeId(), x.getNodeId()));
            vo.setExecuteStartTime(x.getExecuteStartTime() == null ? x.getCreateTime() : x.getExecuteStartTime());
            vo.setExecuteEndTime(x.getExecuteEndTime() == null ? x.getCallBackTime() : x.getExecuteEndTime());
            List<JobWorkRunNodeLogDetailPo> jobWorkRunNodeLogDetailPos = finalDetailMap.get(buildRunNodeLogDetailKey(x.getNodeLogId(), x.getTurnDate()));
            if (CollUtil.isNotEmpty(jobWorkRunNodeLogDetailPos)) {
                String jobDetail = jobWorkRunNodeLogDetailPos.stream().map(JobWorkRunNodeLogDetailPo::getHandleMsg)
                        .collect(Collectors.joining("<br>"));
                vo.setLogDetail(jobDetail);
            }
            return vo;
        });
    }

    /**
     * 构建运行节点日志详情key
     *
     * @param nodeLogId 节点日志ID
     * @param turnDate  翻牌日期
     * @return 运行节点日志详情key
     */
    private String buildRunNodeLogDetailKey(String nodeLogId, Date turnDate) {
        return nodeLogId + ":" + (turnDate == null ? "" : DateUtil.formatDate(turnDate));
    }

    /**
     * 获取作业节点
     */
    @Override
    public JobWorkNodePo getWorkNode(String workNodeId) {
        return jobWorkNodeMapper.selectById(workNodeId);
    }

    /**
     * 日志详情分页列表
     */
    @Override
    public IPage<JobWorkRunNodeLogDetailPo> logDetailPageList(JobWorkNodeLogPageParam param) {
        Date startTime = parseLogStartTime(param.getStartTime());
        Date endTime = parseLogEndTime(param.getEndTime());
        List<String> groupNodeIds = findLogScopeNodeIds(param);
        if (StrUtil.isNotBlank(param.getGroupId()) && CollUtil.isEmpty(groupNodeIds)) {
            return new Page<>(param.getStart(), param.getLength());
        }
        IPage<JobWorkRunNodeLogDetailPo> page = jobWorkRunNodeLogDetailMapper.selectPage(new Page<>(param.getStart(), param.getLength()),
                Wrappers.lambdaQuery(JobWorkRunNodeLogDetailPo.class)
                        .eq(StrUtil.isNotBlank(param.getWorkId()), JobWorkRunNodeLogDetailPo::getWorkId, param.getWorkId())
                        .eq(StrUtil.isNotBlank(param.getNodeId()), JobWorkRunNodeLogDetailPo::getNodeId, param.getNodeId())
                        .in(CollUtil.isNotEmpty(groupNodeIds), JobWorkRunNodeLogDetailPo::getNodeId, groupNodeIds)
                        .eq(StrUtil.isNotBlank(param.getNodeLogId()), JobWorkRunNodeLogDetailPo::getNodeLogId, param.getNodeLogId())
                        .ge(startTime != null, JobWorkRunNodeLogDetailPo::getExecuteTime, startTime)
                        .le(endTime != null, JobWorkRunNodeLogDetailPo::getExecuteTime, endTime)
                        .orderByDesc(JobWorkRunNodeLogDetailPo::getExecuteTime));
        if (CollUtil.isNotEmpty(page.getRecords())) {
            Map<String, String> nodeNameMap = findNodeNameMap(page.getRecords().stream()
                    .map(JobWorkRunNodeLogDetailPo::getNodeId)
                    .collect(Collectors.toList()));
            page.getRecords().forEach(log -> log.setNodeName(nodeNameMap.getOrDefault(log.getNodeId(), log.getNodeId())));
        }
        return page;
    }

    private Map<String, String> findNodeNameMap(Collection<String> nodeIds) {
        if (CollUtil.isEmpty(nodeIds)) {
            return Collections.emptyMap();
        }
        return jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                        .in(JobWorkNodePo::getNodeId, nodeIds))
                .stream()
                .collect(Collectors.toMap(JobWorkNodePo::getNodeId,
                        node -> StrUtil.blankToDefault(node.getNodeName(), node.getNodeId()),
                        (left, right) -> left));
    }

    private List<String> findLogScopeNodeIds(JobWorkNodeLogPageParam param) {
        if (StrUtil.isBlank(param.getGroupId())) {
            return Collections.emptyList();
        }
        return jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                        .eq(StrUtil.isNotBlank(param.getWorkId()), JobWorkNodePo::getWorkId, param.getWorkId())
                        .eq(JobWorkNodePo::getGroupId, param.getGroupId()))
                .stream()
                .map(JobWorkNodePo::getNodeId)
                .collect(Collectors.toList());
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


    private void fillWorkNodeVo(JobWorkNodeVo vo, JobWorkNodePo runNodePo) {
        if (runNodePo == null) {
            return;
        }
        vo.setNodeRunStatusName(FlowRunStatusEnum.getValueByCode(runNodePo.getNodeRunStatus()));
        vo.setTurnDateText(runNodePo.getTurnDate() == null ? null : DateUtil.formatDate(runNodePo.getTurnDate()));
        vo.setRetryTimes(runNodePo.getCurrentRetryTimes());
    }

    /**
     * 填充文件配置
     */
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

    /**
     * 保存文件配置
     */
    private void saveFileConfig(JobWorkNodeParam param) {
        // 文件导入
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
        // 文件导出
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

    /**
     * 保存导入文件配置
     */
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

    /**
     * 保存导出文件配置
     */
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
