package com.nbatch.job.admin.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.excel.JobWorkFlowGroupExcelRow;
import com.nbatch.job.admin.core.domain.excel.JobWorkNodeDependExcelRow;
import com.nbatch.job.admin.core.domain.excel.JobWorkNodeExcelRow;
import com.nbatch.job.admin.core.domain.po.*;
import com.nbatch.job.admin.core.enums.FlowElementTypeEnum;
import com.nbatch.job.admin.core.enums.FlowGroupTypeEnum;
import com.nbatch.job.admin.core.helper.FlowGroupHelper;
import com.nbatch.job.admin.mapper.*;
import com.nbatch.job.admin.service.IJobWorkFlowDependService;
import com.nbatch.job.admin.service.IJobWorkNodeService;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.NodeTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 作业流程配置服务，处理组、节点和依赖关系的工作簿导入导出。
 */
@Component
@RequiredArgsConstructor
public class JobWorkFlowDependServiceImpl implements IJobWorkFlowDependService {

    private static final String GROUP_SHEET = "组";
    private static final String DEPENDENCY_SHEET = "依赖关系";
    private static final String EXAMPLE_PREFIX = "EXAMPLE_";
    private static final List<NodeExcelColumn> COMMON_NODE_COLUMNS = Arrays.asList(
            nodeColumn("节点标识", JobWorkNodeExcelRow::getSourceNodeId),
            nodeColumn("所属组标识", JobWorkNodeExcelRow::getSourceGroupId),
            nodeColumn("节点名称", JobWorkNodeExcelRow::getNodeName),
            nodeColumn("节点说明", JobWorkNodeExcelRow::getNodeDesc),
            nodeColumn("失败策略", JobWorkNodeExcelRow::getErrorStrategy),
            nodeColumn("重试次数", JobWorkNodeExcelRow::getRetryTimes));
    // 类型 -> 列
    private static final Map<NodeTypeEnum, List<NodeExcelColumn>> NODE_COLUMNS = createNodeColumns();

    private final IJobWorkMapper jobWorkMapper;
    private final IJobWorkNodeMapper jobWorkNodeMapper;
    private final IJobWorkFlowGroupMapper jobWorkFlowGroupMapper;
    private final IJobWorkFlowDependencyMapper jobWorkFlowDependencyMapper;
    private final IJobWorkImportFileMapper jobWorkImportFileMapper;
    private final IJobWorkExportFileMapper jobWorkExportFileMapper;
    private final FlowGroupHelper flowGroupHelper;
    private final IJobWorkNodeService jobWorkNodeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String save(JobWorkFlowDependencyPo dependency) {
        DependencyData data = loadData(requireWorkId(dependency));
        validate(dependency, data, new HashSet<>());
        dependency.setDependencyId(null);
        dependency.setCreateTime(LocalDateTime.now());
        jobWorkFlowDependencyMapper.insert(dependency);
        return dependency.getDependencyId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteGroup(String workId, String groupId) {
        requireWorkId(workId);
        JobWorkFlowGroupPo group = jobWorkFlowGroupMapper.selectById(groupId);
        if (group == null || !StrUtil.equals(workId, group.getWorkId())) {
            throw new IllegalArgumentException("流程组不存在或不属于当前作业");
        }
        if (FlowGroupTypeEnum.ROOT.name().equals(group.getGroupType())) {
            throw new IllegalArgumentException("默认组不可删除");
        }
        Integer groupStatus = flowGroupHelper.resolveGroupStatusMap(workId).get(groupId);
        if (FlowRunStatusEnum.DISPATCHED.getCode().equals(groupStatus)
                || FlowRunStatusEnum.RUNNING.getCode().equals(groupStatus)) {
            throw new IllegalArgumentException("流程组正在调度或执行，不能删除");
        }
        List<JobWorkNodePo> nodes = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, workId)
                .eq(JobWorkNodePo::getGroupId, groupId));
        boolean hasRunningNode = nodes.stream().anyMatch(node -> FlowRunStatusEnum.DISPATCHED.getCode().equals(node.getNodeRunStatus())
                || FlowRunStatusEnum.RUNNING.getCode().equals(node.getNodeRunStatus()));
        if (hasRunningNode) {
            throw new IllegalArgumentException("流程组包含正在调度或执行的节点，不能删除");
        }
        jobWorkFlowDependencyMapper.delete(Wrappers.lambdaQuery(JobWorkFlowDependencyPo.class)
                .eq(JobWorkFlowDependencyPo::getWorkId, workId)
                .and(wrapper -> wrapper
                        .eq(JobWorkFlowDependencyPo::getPredecessorType, FlowElementTypeEnum.GROUP.name())
                        .eq(JobWorkFlowDependencyPo::getPredecessorId, groupId)
                        .or()
                        .eq(JobWorkFlowDependencyPo::getSuccessorType, FlowElementTypeEnum.GROUP.name())
                        .eq(JobWorkFlowDependencyPo::getSuccessorId, groupId)));
        for (JobWorkNodePo node : nodes) {
            jobWorkNodeService.delete(node.getNodeId());
        }
        return jobWorkFlowGroupMapper.deleteById(groupId);
    }

    @Override
    public void export(String workId, HttpServletResponse response) throws IOException {
        DependencyData data = loadData(workId);
        Map<String, JobWorkImportFilePo> importFiles = loadImportFiles(data.nodes.keySet());
        Map<String, JobWorkExportFilePo> exportFiles = loadExportFiles(data.nodes.keySet());
        prepareResponse(response, "作业流程配置_" + workId);
        try (ExcelWriter writer = EasyExcel.write(response.getOutputStream()).build()) {
            // 向excel中写入组数据
            writer.write(data.groupList.stream().map(this::toGroupRow).collect(Collectors.toList()),
                    EasyExcel.writerSheet(GROUP_SHEET).head(JobWorkFlowGroupExcelRow.class).build());
            for (NodeTypeEnum nodeType : NodeTypeEnum.values()) {
                List<JobWorkNodeExcelRow> rows = data.nodeList.stream()
                        .filter(node -> nodeType.getCode().equals(node.getNodeType()))
                        .map(node -> toNodeRow(node, importFiles.get(node.getNodeId()), exportFiles.get(node.getNodeId())))
                        .collect(Collectors.toList());
                writer.write(toExcelRows(nodeType, rows), EasyExcel.writerSheet(nodeType.getValue()).head(nodeHead(nodeType)).build());
            }
            // 向excel中写入依赖数据
            writer.write(data.dependencies.stream().map(item -> toDependencyRow(item, data)).collect(Collectors.toList()),
                    EasyExcel.writerSheet(DEPENDENCY_SHEET).head(JobWorkNodeDependExcelRow.class).build());
        }
    }

    @Override
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        prepareResponse(response, "作业流程配置导入模板");
        try (ExcelWriter writer = EasyExcel.write(response.getOutputStream()).build()) {
            // 初始化组sheet
            writer.write(Collections.singletonList(exampleGroupRow()),
                    EasyExcel.writerSheet(GROUP_SHEET).head(JobWorkFlowGroupExcelRow.class).build());
            // 初始化节点sheet
            for (NodeTypeEnum nodeType : NodeTypeEnum.values()) {
                writer.write(toExcelRows(nodeType, Collections.singletonList(exampleNodeRow(nodeType))),
                        EasyExcel.writerSheet(nodeType.getValue()).head(nodeHead(nodeType)).build());
            }
            // 初始化依赖关系sheet
            writer.write(Collections.singletonList(exampleDependencyRow()),
                    EasyExcel.writerSheet(DEPENDENCY_SHEET).head(JobWorkNodeDependExcelRow.class).build());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importExcel(String workId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择需要导入的Excel文件");
        }
        requireWorkId(workId);
        // 确保根组存在
        flowGroupHelper.ensureRootGroup(workId);
        // excel中导入group的sourceGroupId -> groupId
        Map<String, String> groupIdMap = importGroups(workId, readSheet(file, GROUP_SHEET, JobWorkFlowGroupExcelRow.class));
        // excel中导入node的sourceNodeId -> nodeId
        Map<String, String> nodeIdMap = new LinkedHashMap<>();
        int nodeCount = 0;
        for (NodeTypeEnum nodeType : NodeTypeEnum.values()) {
            nodeCount += importNodes(workId, nodeType, readSheet(file, nodeType.getValue(), JobWorkNodeExcelRow.class),
                    groupIdMap, nodeIdMap);
        }
        int dependencyCount = importDependencies(workId, readSheet(file, DEPENDENCY_SHEET, JobWorkNodeDependExcelRow.class),
                groupIdMap, nodeIdMap);
        return groupIdMap.size() + nodeCount + dependencyCount;
    }

    /**
     * 根据节点类型得到对应的个性化列map
     */
    private static Map<NodeTypeEnum, List<NodeExcelColumn>> createNodeColumns() {
        Map<NodeTypeEnum, List<NodeExcelColumn>> columns = new EnumMap<>(NodeTypeEnum.class);
        columns.put(NodeTypeEnum.NODE_TYPE_SCRIPT, withNodeColumns(
                nodeColumn("执行内容", JobWorkNodeExcelRow::getExecuteContent),
                nodeColumn("脚本类型", JobWorkNodeExcelRow::getScriptType),
                nodeColumn("执行参数", JobWorkNodeExcelRow::getExecuteContentParam)));
        columns.put(NodeTypeEnum.NODE_TYPE_STORE_PROCEDURE, withNodeColumns(
                nodeColumn("数据库类型", JobWorkNodeExcelRow::getDbType),
                nodeColumn("执行内容", JobWorkNodeExcelRow::getExecuteContent),
                nodeColumn("执行参数", JobWorkNodeExcelRow::getExecuteContentParam)));
        columns.put(NodeTypeEnum.NODE_TYPE_STORE_PROCEDURE_RETURN_STR, withNodeColumns(
                nodeColumn("数据库类型", JobWorkNodeExcelRow::getDbType),
                nodeColumn("执行内容", JobWorkNodeExcelRow::getExecuteContent),
                nodeColumn("执行参数", JobWorkNodeExcelRow::getExecuteContentParam)));
        columns.put(NodeTypeEnum.NODE_TYPE_EXECUTE_SQL, withNodeColumns(
                nodeColumn("数据库类型", JobWorkNodeExcelRow::getDbType),
                nodeColumn("执行内容", JobWorkNodeExcelRow::getExecuteContent),
                nodeColumn("执行参数", JobWorkNodeExcelRow::getExecuteContentParam)));
        columns.put(NodeTypeEnum.NODE_TYPE_FILE_TO_DB, withNodeColumns(
                nodeColumn("数据库类型", JobWorkNodeExcelRow::getDbType),
                nodeColumn("文件名", JobWorkNodeExcelRow::getFileName),
                nodeColumn("表名", JobWorkNodeExcelRow::getTableName),
                nodeColumn("字段", JobWorkNodeExcelRow::getTableFields),
                nodeColumn("条件", JobWorkNodeExcelRow::getTableCondition),
                nodeColumn("文件编码", JobWorkNodeExcelRow::getFileCode),
                nodeColumn("分隔符", JobWorkNodeExcelRow::getSeparator),
                nodeColumn("全量更新", JobWorkNodeExcelRow::getAllUpdate),
                nodeColumn("Gzip压缩", JobWorkNodeExcelRow::getGzip),
                nodeColumn("文件名参数", JobWorkNodeExcelRow::getFileNameParam)));
        columns.put(NodeTypeEnum.NODE_TYPE_DB_TO_FILE, withNodeColumns(
                nodeColumn("数据库类型", JobWorkNodeExcelRow::getDbType),
                nodeColumn("文件名", JobWorkNodeExcelRow::getFileName),
                nodeColumn("表名", JobWorkNodeExcelRow::getTableName),
                nodeColumn("字段", JobWorkNodeExcelRow::getTableFields),
                nodeColumn("条件", JobWorkNodeExcelRow::getTableCondition),
                nodeColumn("文件编码", JobWorkNodeExcelRow::getFileCode),
                nodeColumn("分隔符", JobWorkNodeExcelRow::getSeparator),
                nodeColumn("全量更新", JobWorkNodeExcelRow::getAllUpdate),
                nodeColumn("Gzip压缩", JobWorkNodeExcelRow::getGzip),
                nodeColumn("文件名参数", JobWorkNodeExcelRow::getFileNameParam)));
        columns.put(NodeTypeEnum.NODE_TYPE_BEAN, withNodeColumns(
                nodeColumn("执行器", JobWorkNodeExcelRow::getExecuteHandler),
                nodeColumn("执行参数", JobWorkNodeExcelRow::getExecuteContentParam)));
        return Collections.unmodifiableMap(columns);
    }

    private static List<NodeExcelColumn> withNodeColumns(NodeExcelColumn... specificColumns) {
        List<NodeExcelColumn> columns = new ArrayList<>(COMMON_NODE_COLUMNS);
        columns.addAll(Arrays.asList(specificColumns));
        return Collections.unmodifiableList(columns);
    }

    private static NodeExcelColumn nodeColumn(String title, Function<JobWorkNodeExcelRow, Object> valueExtractor) {
        return new NodeExcelColumn(title, valueExtractor);
    }

    private List<List<String>> nodeHead(NodeTypeEnum nodeType) {
        return nodeColumns(nodeType).stream()
                .map(column -> Collections.singletonList(column.title))
                .collect(Collectors.toList());
    }

    private List<List<Object>> toExcelRows(NodeTypeEnum nodeType, List<JobWorkNodeExcelRow> rows) {
        List<NodeExcelColumn> columns = nodeColumns(nodeType);
        return rows.stream().map(row -> columns.stream()
                        .map(column -> column.valueExtractor.apply(row))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    private List<NodeExcelColumn> nodeColumns(NodeTypeEnum nodeType) {
        List<NodeExcelColumn> columns = NODE_COLUMNS.get(nodeType);
        if (columns == null) {
            throw new IllegalArgumentException("不支持的节点类型：" + nodeType);
        }
        return columns;
    }

    private Map<String, JobWorkImportFilePo> loadImportFiles(Set<String> nodeIds) {
        if (nodeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return jobWorkImportFileMapper.selectList(Wrappers.lambdaQuery(JobWorkImportFilePo.class).in(JobWorkImportFilePo::getNodeId, nodeIds))
                .stream().collect(Collectors.toMap(JobWorkImportFilePo::getNodeId, item -> item, (left, right) -> left));
    }

    private Map<String, JobWorkExportFilePo> loadExportFiles(Set<String> nodeIds) {
        if (nodeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return jobWorkExportFileMapper.selectList(Wrappers.lambdaQuery(JobWorkExportFilePo.class).in(JobWorkExportFilePo::getNodeId, nodeIds))
                .stream().collect(Collectors.toMap(JobWorkExportFilePo::getNodeId, item -> item, (left, right) -> left));
    }

    /**
     * 导入组信息
     */
    private Map<String, String> importGroups(String workId, List<JobWorkFlowGroupExcelRow> rows) {
        DependencyData data = loadData(workId);
        // 得到当前根目录中的默认组
        JobWorkFlowGroupPo root = data.groupList.stream().filter(group -> FlowGroupTypeEnum.ROOT.name().equals(group.getGroupType()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("默认组不存在"));
        Map<String, String> groupIdMap = new LinkedHashMap<>();
        for (int index = 0; index < rows.size(); index++) {
            JobWorkFlowGroupExcelRow row = rows.get(index);
            // 判断是否为空行或模版示例行
            if (isBlank(row) || isExample(row.getSourceGroupId())) {
                continue;
            }
            // getSourceGroupId这个字段不可为空
            String sourceId = required(StrUtil.trim(row.getSourceGroupId()), GROUP_SHEET, index, "组标识");
            if (groupIdMap.containsKey(sourceId)) {
                throw rowError(GROUP_SHEET, index, "组标识重复");
            }
            // 如果是 root group 则只记录sourceId，而不向表中插入
            if (StrUtil.equals(FlowGroupTypeEnum.ROOT.name(), trimToUpperCase((row.getGroupType())))) {
                groupIdMap.put(sourceId, root.getGroupId());
                continue;
            }
            JobWorkFlowGroupPo group = new JobWorkFlowGroupPo();
            group.setWorkId(workId);
            group.setGroupName(required(StrUtil.trim(row.getGroupName()), GROUP_SHEET, index, "组名称"));
            group.setGroupDesc(StrUtil.trim(row.getGroupDesc()));
            group.setGroupType(FlowGroupTypeEnum.NORMAL.name());
            group.setSortOrder(row.getSortOrder() == null ? 0 : row.getSortOrder());
            group.setVersion(1);
            group.setCreateTime(new Date());
            group.setUpdateTime(new Date());
            jobWorkFlowGroupMapper.insert(group);
            groupIdMap.put(sourceId, group.getGroupId());
        }
        return groupIdMap;
    }

    /**
     * 导入节点信息
     */
    private int importNodes(String workId, NodeTypeEnum nodeType, List<JobWorkNodeExcelRow> rows,
                            Map<String, String> groupIdMap, Map<String, String> nodeIdMap) {
        int count = 0;
        for (int index = 0; index < rows.size(); index++) {
            JobWorkNodeExcelRow row = rows.get(index);
            if (isBlank(row) || isExample(row.getSourceNodeId())) {
                continue;
            }
            // 得到excel中导入的sourceNodeId
            String sourceId = required(StrUtil.trim(row.getSourceNodeId()), nodeType.getValue(), index, "节点标识");
            if (nodeIdMap.containsKey(sourceId)) {
                throw rowError(nodeType.getValue(), index, "节点标识重复");
            }
            String sourceGroupId = StrUtil.trim(row.getSourceGroupId());
            String groupId = StrUtil.isBlank(sourceGroupId) ? flowGroupHelper.resolveGroupId(workId, null) : groupIdMap.get(sourceGroupId);
            if (StrUtil.isBlank(groupId)) {
                throw rowError(nodeType.getValue(), index, "所属组标识不存在，请先在组Sheet中定义");
            }
            JobWorkNodePo node = new JobWorkNodePo();
            node.setWorkId(workId);
            node.setGroupId(groupId);
            node.setNodeName(required(StrUtil.trim(row.getNodeName()), nodeType.getValue(), index, "节点名称"));
            node.setNodeDesc(StrUtil.trim(row.getNodeDesc()));
            node.setNodeType(nodeType.getCode());
            node.setDbType(StrUtil.trim(row.getDbType()));
            node.setExecuteContent(normalizeScriptContent(nodeType, row.getExecuteContent()));
            node.setExecuteContentParam(StrUtil.trim(row.getExecuteContentParam()));
            node.setExecuteHandler(StrUtil.trim(row.getExecuteHandler()));
            node.setScriptType(StrUtil.trim(row.getScriptType()));
            node.setErrorStrategy(StrUtil.blankToDefault(StrUtil.trim(row.getErrorStrategy()), "retry"));
            node.setRetryTimes(normalizeRetryTimes(row.getRetryTimes()));
            node.setNodeRunStatus(FlowRunStatusEnum.UNINITIALIZED.getCode());
            node.setUpdateTime(new Date());
            jobWorkNodeMapper.insert(node);
            nodeIdMap.put(sourceId, node.getNodeId());
            saveFileConfig(nodeType, node.getNodeId(), row);
            count++;
        }
        return count;
    }

    /**
     * 导入依赖信息
     */
    private int importDependencies(String workId, List<JobWorkNodeDependExcelRow> rows, Map<String, String> groupIdMap,
                                   Map<String, String> nodeIdMap) {
        DependencyData data = loadData(workId);
        // 候选依赖
        List<JobWorkFlowDependencyPo> candidates = new ArrayList<>();
        Set<String> importedKeys = new HashSet<>();
        for (int index = 0; index < rows.size(); index++) {
            JobWorkNodeDependExcelRow row = rows.get(index);
            if (isBlank(row) || isExample(row.getPredecessorId()) || isExample(row.getSuccessorId())) {
                continue;
            }
            JobWorkFlowDependencyPo dependency = new JobWorkFlowDependencyPo();
            dependency.setWorkId(workId);
            dependency.setPredecessorType(trimToUpperCase((row.getPredecessorType())));
            dependency.setPredecessorId(resolveEndpointId(dependency.getPredecessorType(), row.getPredecessorId(), groupIdMap, nodeIdMap, index));
            dependency.setSuccessorType(trimToUpperCase((row.getSuccessorType())));
            dependency.setSuccessorId(resolveEndpointId(dependency.getSuccessorType(), row.getSuccessorId(), groupIdMap, nodeIdMap, index));
            try {
                // 校验依赖
                validate(dependency, data, importedKeys);
            } catch (IllegalArgumentException exception) {
                throw rowError(DEPENDENCY_SHEET, index, exception.getMessage());
            }
            candidates.add(dependency);
            importedKeys.add(key(dependency));
        }
        LocalDateTime now = LocalDateTime.now();
        candidates.forEach(item -> {
            item.setCreateTime(now);
            jobWorkFlowDependencyMapper.insert(item);
        });
        return candidates.size();
    }

    /**
     * 根据sourceId得到需要插入数据库的nodeId和groupId
     */
    private String resolveEndpointId(String type, String sourceId,
                                     Map<String, String> groupIdMap, Map<String, String> nodeIdMap, int rowIndex) {
        if (!isType(type)) {
            throw rowError(DEPENDENCY_SHEET, rowIndex, "对象类型只能是NODE或GROUP");
        }
        String result = StrUtil.equals(FlowElementTypeEnum.NODE.name(), type) ?
                nodeIdMap.get(StrUtil.trim(sourceId)) : groupIdMap.get(StrUtil.trim(sourceId));
        if (StrUtil.isBlank(result)) {
            throw rowError(DEPENDENCY_SHEET, rowIndex, "对象标识未在组或节点Sheet中找到");
        }
        return result;
    }

    /**
     * 保存文件配置
     */
    private void saveFileConfig(NodeTypeEnum nodeType, String nodeId, JobWorkNodeExcelRow row) {
        if (NodeTypeEnum.NODE_TYPE_FILE_TO_DB == nodeType && hasFileConfig(row)) {
            JobWorkImportFilePo file = new JobWorkImportFilePo();
            file.setNodeId(nodeId);
            file.setFileName(StrUtil.trim(row.getFileName()));
            file.setImportTableName(StrUtil.trim(row.getTableName()));
            file.setImportTableFiled(StrUtil.trim(row.getTableFields()));
            file.setImportTableCondition(StrUtil.trim(row.getTableCondition()));
            file.setFileCode(StrUtil.trim(row.getFileCode()));
            file.setSep(StrUtil.trim(row.getSeparator()));
            file.setAllUpdate(row.getAllUpdate());
            file.setIsGzip(row.getGzip());
            file.setFileNameParam(StrUtil.trim(row.getFileNameParam()));
            jobWorkImportFileMapper.insert(file);
        } else if (NodeTypeEnum.NODE_TYPE_DB_TO_FILE == nodeType && hasFileConfig(row)) {
            JobWorkExportFilePo file = new JobWorkExportFilePo();
            file.setNodeId(nodeId);
            file.setFileName(StrUtil.trim(row.getFileName()));
            file.setExportTableName(StrUtil.trim(row.getTableName()));
            file.setExportTableFiled(StrUtil.trim(row.getTableFields()));
            file.setExportTableCondition(StrUtil.trim(row.getTableCondition()));
            file.setFileCode(StrUtil.trim(row.getFileCode()));
            file.setSep(StrUtil.trim(row.getSeparator()));
            file.setAllUpdate(row.getAllUpdate());
            file.setIsGzip(row.getGzip());
            file.setFileNameParam(StrUtil.trim(row.getFileNameParam()));
            jobWorkExportFileMapper.insert(file);
        }
    }

    private boolean hasFileConfig(JobWorkNodeExcelRow row) {
        return StrUtil.isNotBlank(row.getFileName()) || StrUtil.isNotBlank(row.getTableName()) || StrUtil.isNotBlank(row.getTableFields())
                || StrUtil.isNotBlank(row.getTableCondition()) || StrUtil.isNotBlank(row.getFileCode()) || StrUtil.isNotBlank(row.getSeparator())
                || row.getAllUpdate() != null || row.getGzip() != null || StrUtil.isNotBlank(row.getFileNameParam());
    }

    /**
     * groupPo转换为组行
     * @param group 组PO
     * @return 组行
     */
    private JobWorkFlowGroupExcelRow toGroupRow(JobWorkFlowGroupPo group) {
        JobWorkFlowGroupExcelRow row = new JobWorkFlowGroupExcelRow();
        row.setSourceGroupId(group.getGroupId());
        row.setGroupName(group.getGroupName());
        row.setGroupDesc(group.getGroupDesc());
        row.setGroupType(group.getGroupType());
        row.setSortOrder(group.getSortOrder());
        return row;
    }

    /**
     * nodePo转换为节点行
     * @param node 节点PO
     * @param importFile 导入文件PO
     * @param exportFile 导出文件PO
     * @return 节点行
     */
    private JobWorkNodeExcelRow toNodeRow(JobWorkNodePo node, JobWorkImportFilePo importFile, JobWorkExportFilePo exportFile) {
        JobWorkNodeExcelRow row = new JobWorkNodeExcelRow();
        row.setSourceNodeId(node.getNodeId());
        row.setSourceGroupId(node.getGroupId());
        row.setNodeName(node.getNodeName());
        row.setNodeDesc(node.getNodeDesc());
        row.setDbType(node.getDbType());
        row.setExecuteContent(node.getExecuteContent());
        row.setExecuteContentParam(node.getExecuteContentParam());
        row.setExecuteHandler(node.getExecuteHandler());
        row.setScriptType(node.getScriptType());
        row.setErrorStrategy(node.getErrorStrategy());
        row.setRetryTimes(node.getRetryTimes());
        if (importFile != null) {
            fillFileRow(row, importFile.getFileName(), importFile.getImportTableName(), importFile.getImportTableFiled(), importFile.getImportTableCondition(), importFile.getFileCode(), importFile.getSep(), importFile.getAllUpdate(), importFile.getIsGzip(), importFile.getFileNameParam());
        } else if (exportFile != null) {
            fillFileRow(row, exportFile.getFileName(), exportFile.getExportTableName(), exportFile.getExportTableFiled(), exportFile.getExportTableCondition(), exportFile.getFileCode(), exportFile.getSep(), exportFile.getAllUpdate(), exportFile.getIsGzip(), exportFile.getFileNameParam());
        }
        return row;
    }

    private void fillFileRow(JobWorkNodeExcelRow row, String fileName, String tableName, String fields, String condition,
                             String fileCode, String separator, Integer allUpdate, Integer gzip, String fileNameParam) {
        row.setFileName(fileName);
        row.setTableName(tableName);
        row.setTableFields(fields);
        row.setTableCondition(condition);
        row.setFileCode(fileCode);
        row.setSeparator(separator);
        row.setAllUpdate(allUpdate);
        row.setGzip(gzip);
        row.setFileNameParam(fileNameParam);
    }

    /**
     * 示例组行
     * @return 示例组行
     */
    private JobWorkFlowGroupExcelRow exampleGroupRow() {
        JobWorkFlowGroupExcelRow row = new JobWorkFlowGroupExcelRow();
        row.setSourceGroupId(EXAMPLE_PREFIX + "GROUP_ETL");
        row.setGroupName("示例流程组（导入时忽略）");
        row.setGroupDesc("以 EXAMPLE_ 开头的示例行仅用于填写参考，导入时会自动忽略");
        row.setGroupType(FlowGroupTypeEnum.NORMAL.name());
        row.setSortOrder(1);
        return row;
    }

    /**
     * 示例节点行
     * @param nodeType 节点类型
     * @return 示例节点行
     */
    private JobWorkNodeExcelRow exampleNodeRow(NodeTypeEnum nodeType) {
        JobWorkNodeExcelRow row = new JobWorkNodeExcelRow();
        row.setSourceNodeId(EXAMPLE_PREFIX + nodeType.getCode().toUpperCase());
        row.setSourceGroupId(EXAMPLE_PREFIX + "GROUP_ETL");
        row.setNodeName("示例" + nodeType.getValue() + "节点（导入时忽略）");
        row.setNodeDesc("将本行替换为实际配置；以 EXAMPLE_ 开头的节点标识会被忽略");
        row.setErrorStrategy("retry");
        row.setRetryTimes(-1);
        switch (nodeType) {
            case NODE_TYPE_SCRIPT:
                row.setExecuteContent("echo hello");
                row.setScriptType("shell");
                break;
            case NODE_TYPE_STORE_PROCEDURE:
            case NODE_TYPE_STORE_PROCEDURE_RETURN_STR:
                row.setDbType("mysql");
                row.setExecuteContent("example_procedure");
                break;
            case NODE_TYPE_EXECUTE_SQL:
                row.setDbType("mysql");
                row.setExecuteContent("select 1");
                break;
            case NODE_TYPE_FILE_TO_DB:
                row.setDbType("mysql");
                row.setFileName("file_to_db.csv");
                row.setTableName("file_to_db_table");
                row.setTableFields("id,name");
                row.setFileCode("UTF-8");
                row.setSeparator(",");
                row.setAllUpdate(1);
                row.setGzip(0);
                break;
            case NODE_TYPE_DB_TO_FILE:
                row.setDbType("mysql");
                row.setFileName("db_To_File.csv");
                row.setTableName("db_To_File_table");
                row.setTableFields("id,name");
                row.setFileCode("UTF-8");
                row.setSeparator(",");
                row.setAllUpdate(1);
                row.setGzip(0);
                break;
            case NODE_TYPE_BEAN:
                row.setExecuteHandler("exampleJobHandler");
                break;
            default:
                break;
        }
        return row;
    }

    /**
     * 示例依赖行
     * @return 示例依赖行
     */
    private JobWorkNodeDependExcelRow exampleDependencyRow() {
        JobWorkNodeDependExcelRow row = new JobWorkNodeDependExcelRow();
        row.setPredecessorType(FlowElementTypeEnum.NODE.name());
        row.setPredecessorId(EXAMPLE_PREFIX + NodeTypeEnum.NODE_TYPE_SCRIPT.getCode().toUpperCase());
        row.setPredecessorName("示例前置节点（导入时忽略）");
        row.setSuccessorType(FlowElementTypeEnum.NODE.name());
        row.setSuccessorId(EXAMPLE_PREFIX + NodeTypeEnum.NODE_TYPE_EXECUTE_SQL.getCode().toUpperCase());
        row.setSuccessorName("示例后置节点（导入时忽略）");
        return row;
    }

    /**
     * 依赖PO转换为依赖行
     * @param item 依赖PO
     * @param data 依赖数据
     * @return 依赖行
     */
    private JobWorkNodeDependExcelRow toDependencyRow(JobWorkFlowDependencyPo item, DependencyData data) {
        JobWorkNodeDependExcelRow row = new JobWorkNodeDependExcelRow();
        row.setPredecessorType(item.getPredecessorType());
        row.setPredecessorId(item.getPredecessorId());
        row.setPredecessorName(data.names.get(endpoint(item.getPredecessorType(), item.getPredecessorId())));
        row.setSuccessorType(item.getSuccessorType());
        row.setSuccessorId(item.getSuccessorId());
        row.setSuccessorName(data.names.get(endpoint(item.getSuccessorType(), item.getSuccessorId())));
        return row;
    }

    /**
     * 加载作业在数据库中所有依赖数据
     * @param workId 作业ID
     * @return 依赖数据
     */
    private DependencyData loadData(String workId) {
        if (StrUtil.isBlank(workId) || jobWorkMapper.selectById(workId) == null) {
            throw new IllegalArgumentException(StrUtil.isBlank(workId) ? "作业ID不能为空" : "作业不存在");
        }
        List<JobWorkNodePo> nodes = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .eq(JobWorkNodePo::getWorkId, workId));
        List<JobWorkFlowGroupPo> groups = jobWorkFlowGroupMapper.selectList(Wrappers.lambdaQuery(JobWorkFlowGroupPo.class)
                .eq(JobWorkFlowGroupPo::getWorkId, workId));
        List<JobWorkFlowDependencyPo> dependencies = jobWorkFlowDependencyMapper.selectList(Wrappers
                .lambdaQuery(JobWorkFlowDependencyPo.class)
                .eq(JobWorkFlowDependencyPo::getWorkId, workId));
        return new DependencyData(workId, nodes, groups, dependencies);
    }

    /**
     * 校验依赖
     * @param dependency 依赖
     * @param data 依赖数据
     * @param importedKeys 已导入的依赖键
     */
    private void validate(JobWorkFlowDependencyPo dependency, DependencyData data, Set<String> importedKeys) {
        if (dependency == null || !data.workId.equals(dependency.getWorkId()) || StrUtil.isBlank(dependency.getPredecessorType())
                || StrUtil.isBlank(dependency.getPredecessorId())
                || StrUtil.isBlank(dependency.getSuccessorType())
                || StrUtil.isBlank(dependency.getSuccessorId())) {
            throw new IllegalArgumentException("依赖参数不完整");
        }
        // 类型必须为NODE或GROUP
        if (!isType(dependency.getPredecessorType()) || !isType(dependency.getSuccessorType())) {
            throw new IllegalArgumentException("依赖端点类型只能是NODE或GROUP");
        }
        if (StrUtil.equals(dependency.getPredecessorType(), dependency.getSuccessorType())
                && StrUtil.equals(dependency.getPredecessorId(), dependency.getSuccessorId())) {
            throw new IllegalArgumentException("依赖的前置和后置端点不能相同");
        }
        // 判断依赖在库中或者在导入的excel中是否存在
        if (!exists(dependency.getPredecessorType(), dependency.getPredecessorId(), data)
                || !exists(dependency.getSuccessorType(), dependency.getSuccessorId(), data)) {
            throw new IllegalArgumentException("依赖端点不存在或不属于当前作业");
        }
        if (data.keys.contains(key(dependency)) || importedKeys.contains(key(dependency))) {
            throw new IllegalArgumentException("该依赖关系已存在");
        }
        if (isMembershipCycle(dependency, data.nodes)) {
            throw new IllegalArgumentException("流程组不能依赖自身包含的节点");
        }
    }

    private boolean exists(String type, String id, DependencyData data) {
        return StrUtil.equals(FlowElementTypeEnum.NODE.name(), type) ? data.nodes.containsKey(id) : data.groups.containsKey(id);
    }

    /**
     * 判断依赖是否为流程组包含节点的依赖
     * @param dependency 依赖
     * @param nodes 节点列表
     * @return 是否为流程组包含节点的依赖
     */
    private boolean isMembershipCycle(JobWorkFlowDependencyPo dependency, Map<String, JobWorkNodePo> nodes) {
        if (StrUtil.equals(FlowElementTypeEnum.GROUP.name(), dependency.getPredecessorType())
                && StrUtil.equals(FlowElementTypeEnum.NODE.name(), dependency.getSuccessorType())) {
            JobWorkNodePo node = nodes.get(dependency.getSuccessorId());
            return node != null && StrUtil.equals(dependency.getPredecessorId(), node.getGroupId());
        }
        if (StrUtil.equals(FlowElementTypeEnum.NODE.name(), dependency.getPredecessorType())
                && StrUtil.equals(FlowElementTypeEnum.GROUP.name(), dependency.getSuccessorType())) {
            JobWorkNodePo node = nodes.get(dependency.getPredecessorId());
            return node != null && StrUtil.equals(dependency.getSuccessorId(), node.getGroupId());
        }
        return false;
    }

    private <T> List<T> readSheet(MultipartFile file, String sheetName, Class<T> head) {
        try {
            return EasyExcel.read(file.getInputStream()).head(head).sheet(sheetName).doReadSync();
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("读取Sheet【" + sheetName + "】失败，请使用下载的模板", exception);
        }
    }

    /**
     * 准备响应 response
     * @param response 响应
     * @param fileName 文件名
     * @throws IOException IO异常
     */
    private void prepareResponse(HttpServletResponse response, String fileName) throws IOException {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + encoded + ".xlsx");
    }

    private String normalizeScriptContent(NodeTypeEnum nodeType, String content) {
        if (NodeTypeEnum.NODE_TYPE_SCRIPT != nodeType || content == null) {
            return content;
        }
        if (content.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("脚本内容不能包含 NUL 字符");
        }
        return content.replace("\r\n", "\n").replace('\r', '\n');
    }

    private Integer normalizeRetryTimes(Integer retryTimes) {
        return retryTimes == null ? 0 : Math.max(-1, retryTimes);
    }

    private boolean isBlank(JobWorkFlowGroupExcelRow row) {
        return row == null || (StrUtil.isBlank(row.getSourceGroupId()) && StrUtil.isBlank(row.getGroupName()));
    }

    private boolean isBlank(JobWorkNodeExcelRow row) {
        return row == null || (StrUtil.isBlank(row.getSourceNodeId()) && StrUtil.isBlank(row.getNodeName()));
    }

    private boolean isBlank(JobWorkNodeDependExcelRow row) {
        return row == null || (StrUtil.isBlank(row.getPredecessorType()) && StrUtil.isBlank(row.getPredecessorId())
                && StrUtil.isBlank(row.getSuccessorType()) && StrUtil.isBlank(row.getSuccessorId()));
    }

    private boolean isExample(String sourceId) {
        return StrUtil.startWithIgnoreCase(StrUtil.trim(sourceId), EXAMPLE_PREFIX);
    }

    /**
     * 强制满足作业ID不可不存在同时必须存在于库中
     * @param workId 作业ID
     * @return 作业ID
     */
    private String requireWorkId(String workId) {
        if (StrUtil.isBlank(workId) || jobWorkMapper.selectById(workId) == null) {
            throw new IllegalArgumentException(StrUtil.isBlank(workId) ? "作业ID不能为空" : "作业不存在");
        }
        return workId;
    }

    /**
     * 强制满足作业ID不可不存在同时必须存在于库中
     * @param dependency 依赖
     * @return 作业ID
     */
    private String requireWorkId(JobWorkFlowDependencyPo dependency) {
        if (dependency == null) {
            throw new IllegalArgumentException("依赖参数不能为空");
        }
        return requireWorkId(dependency.getWorkId());
    }

    /**
     * value 字段为空则抛出异常
     */
    private String required(String value, String sheetName, int rowIndex, String columnName) {
        if (StrUtil.isBlank(value)) {
            throw rowError(sheetName, rowIndex, columnName + "不能为空");
        }
        return value;
    }

    private IllegalArgumentException rowError(String sheetName, int rowIndex, String message) {
        return new IllegalArgumentException("Sheet【" + sheetName + "】第" + (rowIndex + 2) + "行：" + message);
    }

    /**
     * 去除空格后装换为大写
     */
    private String trimToUpperCase(String value) {
        String result = StrUtil.trim(value);
        return result == null ? null : result.toUpperCase();
    }

    /**
     * 节点类型名称是否存在
     * @param type 类型
     * @return 是否是节点或组
     */
    private boolean isType(String type) {
        return FlowElementTypeEnum.NODE.name().equals(type) || FlowElementTypeEnum.GROUP.name().equals(type);
    }

    /**
     * 得到唯一key type + id -> type + id
     * @param dependency 依赖
     * @return 依赖key
     */
    private String key(JobWorkFlowDependencyPo dependency) {
        return endpoint(dependency.getPredecessorType(), dependency.getPredecessorId()) + "->" + endpoint(dependency.getSuccessorType(), dependency.getSuccessorId());
    }

    private String endpoint(String type, String id) {
        return type + ':' + id;
    }

    /**
     * 节点excel列
     */
    private static class NodeExcelColumn {
        private final String title;
        private final Function<JobWorkNodeExcelRow, Object> valueExtractor;

        private NodeExcelColumn(String title, Function<JobWorkNodeExcelRow, Object> valueExtractor) {
            this.title = title;
            this.valueExtractor = valueExtractor;
        }
    }

    /**
     * 依赖数据类
     */
    private static class DependencyData {
        private final String workId;
        private final List<JobWorkNodePo> nodeList;
        private final List<JobWorkFlowGroupPo> groupList;
        private final Map<String, JobWorkNodePo> nodes;
        private final Map<String, JobWorkFlowGroupPo> groups;
        private final Map<String, String> names;
        private final List<JobWorkFlowDependencyPo> dependencies;
        private final Set<String> keys;

        private DependencyData(String workId, List<JobWorkNodePo> nodeList, List<JobWorkFlowGroupPo> groupList, List<JobWorkFlowDependencyPo> dependencies) {
            this.workId = workId;
            this.nodeList = nodeList;
            this.groupList = groupList;
            this.nodes = nodeList.stream().collect(Collectors.toMap(JobWorkNodePo::getNodeId, node -> node));
            this.groups = groupList.stream().collect(Collectors.toMap(JobWorkFlowGroupPo::getGroupId, group -> group));
            this.names = new HashMap<>();
            nodeList.forEach(node -> names.put(FlowElementTypeEnum.NODE.name() + ':' + node.getNodeId(), node.getNodeName()));
            groupList.forEach(group -> names.put(FlowElementTypeEnum.GROUP.name() + ':' + group.getGroupId(), group.getGroupName()));
            this.dependencies = dependencies;
            this.keys = dependencies.stream().map(item -> item.getPredecessorType() + ':' + item.getPredecessorId() + "->" + item.getSuccessorType() + ':' + item.getSuccessorId()).collect(Collectors.toSet());
        }
    }
}
