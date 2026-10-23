package com.nbatch.job.admin.core.helper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogPo;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.mapper.IJobWorkRunNodeLogMapper;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.biz.model.RunNodeLogEventParam;
import com.nbatch.job.core.constant.HandleCodeConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 负责运行节点日志的查询、活跃状态更新和闭环。
 */
@Component
@Slf4j
public class RunNodeLogHelper {

    @Resource
    private IJobWorkNodeMapper jobWorkNodeMapper;

    @Resource
    private IJobWorkRunNodeLogMapper jobWorkRunNodeLogMapper;

    /**
     * 按日志标识查询节点日志。
     */
    public JobWorkRunNodeLogPo get(String nodeLogId) {
        return StrUtil.isBlank(nodeLogId) ? null : jobWorkRunNodeLogMapper.selectById(nodeLogId);
    }

    /**
     * 批量查询节点日志，避免在节点循环中逐条访问日志表。
     *
     * @param nodeLogIds 日志标识集合
     * @return 按日志标识索引的日志
     */
    public Map<String, JobWorkRunNodeLogPo> getMap(Collection<String> nodeLogIds) {
        if (CollUtil.isEmpty(nodeLogIds)) {
            return java.util.Collections.emptyMap();
        }
        Map<String, JobWorkRunNodeLogPo> result = new HashMap<>();
        List<JobWorkRunNodeLogPo> jobWorkRunNodeLogPos = jobWorkRunNodeLogMapper.selectByIds(nodeLogIds);
        for (JobWorkRunNodeLogPo log : jobWorkRunNodeLogPos) {
            result.put(log.getNodeLogId(), log);
        }
        return result;
    }

    /**
     * 查询节点当前批次最近一条未闭环日志。
     */
    public JobWorkRunNodeLogPo findUnclosed(String nodeId) {
        JobWorkNodePo node = jobWorkNodeMapper.selectById(nodeId);
        if (node == null) {
            return null;
        }
        return jobWorkRunNodeLogMapper.selectOne(Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                .eq(JobWorkRunNodeLogPo::getWorkId, node.getWorkId())
                .eq(JobWorkRunNodeLogPo::getNodeId, node.getNodeId())
                .eq(node.getTurnDate() != null, JobWorkRunNodeLogPo::getTurnDate, node.getTurnDate())
                .isNull(JobWorkRunNodeLogPo::getCallBackTime)
                .orderByDesc(JobWorkRunNodeLogPo::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 批量查询节点当前批次最近一条未闭环日志。
     */
    public Map<String, JobWorkRunNodeLogPo> findUnclosedMap(List<JobWorkNodePo> nodes) {
        if (CollUtil.isEmpty(nodes)) {
            return Collections.emptyMap();
        }
        Map<String, JobWorkNodePo> nodeMap = nodes.stream().collect(Collectors.toMap(
                JobWorkNodePo::getNodeId, node -> node, (first, ignored) -> first));
        List<JobWorkRunNodeLogPo> jobWorkRunNodeLogPos = jobWorkRunNodeLogMapper.selectList(Wrappers
                .lambdaQuery(JobWorkRunNodeLogPo.class)
                .in(JobWorkRunNodeLogPo::getNodeId, nodeMap.keySet())
                .isNull(JobWorkRunNodeLogPo::getCallBackTime)
                .orderByDesc(JobWorkRunNodeLogPo::getCreateTime));
        Map<String, JobWorkRunNodeLogPo> result = new HashMap<>();
        for (JobWorkRunNodeLogPo log : jobWorkRunNodeLogPos) {
            JobWorkNodePo node = nodeMap.get(log.getNodeId());
            // 与单节点查询保持一致：翻牌作业仅使用当前批次日志，顺序作业不限制批次日期。
            if (node == null || !StrUtil.equals(node.getWorkId(), log.getWorkId())
                    || (node.getTurnDate() != null && !Objects.equals(node.getTurnDate(), log.getTurnDate()))) {
                continue;
            }
            String key = buildWorkNodeTurnKey(node.getWorkId(), node.getNodeId(), node.getTurnDate());
            // 查询结果按创建时间倒序，保留当前作业和批次下最新的一条未闭环日志。
            result.putIfAbsent(key, log);
        }
        return result;
    }

    /**
     * 构建运行节点日志索引键：作业标识、节点标识和批次日期。
     */
    public static String buildWorkNodeTurnKey(String workId, String nodeId, Date turnDate) {
        return StrUtil.blankToDefault(workId, "") + "_"
                + StrUtil.blankToDefault(nodeId, "") + "_"
                + (turnDate == null ? "" : turnDate.getTime());
    }

    /**
     * 批量查询指定作业和批次下各节点最近一条未闭环日志。
     *
     * @param workId   作业标识
     * @param turnDate 批次日期，顺序作业可为空
     * @param nodeIds  节点标识集合
     * @return 按创建时间倒序返回的未闭环日志
     */
    public List<JobWorkRunNodeLogPo> findUnclosed(String workId, Date turnDate, Collection<String> nodeIds) {
        if (StrUtil.isBlank(workId) || CollUtil.isEmpty(nodeIds)) {
            return Collections.emptyList();
        }
        return jobWorkRunNodeLogMapper.selectList(Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                .eq(JobWorkRunNodeLogPo::getWorkId, workId)
                .in(JobWorkRunNodeLogPo::getNodeId, nodeIds)
                .eq(turnDate != null, JobWorkRunNodeLogPo::getTurnDate, turnDate)
                .isNull(JobWorkRunNodeLogPo::getCallBackTime)
                .orderByDesc(JobWorkRunNodeLogPo::getCreateTime));
    }

    /**
     * 根据事件上下文查询当前执行尝试的未闭环日志。
     */
    public JobWorkRunNodeLogPo findCurrentUnclosed(RunNodeLogEventParam event) {
        if (event == null || StrUtil.isBlank(event.getWorkId()) || StrUtil.isBlank(event.getNodeId())) {
            return null;
        }
        return jobWorkRunNodeLogMapper.selectOne(Wrappers.lambdaQuery(JobWorkRunNodeLogPo.class)
                .eq(JobWorkRunNodeLogPo::getWorkId, event.getWorkId())
                .eq(JobWorkRunNodeLogPo::getNodeId, event.getNodeId())
                .eq(event.getTurnDate() != null, JobWorkRunNodeLogPo::getTurnDate, event.getTurnDate())
                .isNull(JobWorkRunNodeLogPo::getCallBackTime)
                .orderByDesc(JobWorkRunNodeLogPo::getCreateTime)
                .last("limit 1"));
    }

    /**
     * 判断当前日志是否仍属于当前执行节点。
     */
    public boolean isCurrentUnclosed(String nodeId, String nodeLogId) {
        JobWorkRunNodeLogPo log = StrUtil.isBlank(nodeId) || StrUtil.isBlank(nodeLogId) ? null : findUnclosed(nodeId);
        return log != null && StrUtil.equals(log.getNodeLogId(), nodeLogId);
    }

    /**
     * 根据管理端确认的执行状态刷新未闭环日志的处理结果和最后活跃时间；不写入执行开始、结束或回调时间。
     */
    public void refreshActiveLogStatus(String nodeLogId, Integer handleCode, String handleMsg) {
        if (StrUtil.isBlank(nodeLogId)) {
            return;
        }
        JobWorkRunNodeLogPo log = new JobWorkRunNodeLogPo();
        log.setNodeLogId(nodeLogId)
                .setHandleCode(handleCode)
                .setHandleMsg(handleMsg)
                .setLastActiveTime(LocalDateTime.now());
        jobWorkRunNodeLogMapper.updateById(log);
    }

    /**
     * 刷新执行器心跳对应的日志活跃时间。
     */
    public void heartbeat(String nodeLogId) {
        if (StrUtil.isBlank(nodeLogId)) {
            return;
        }
        JobWorkRunNodeLogPo log = new JobWorkRunNodeLogPo();
        log.setNodeLogId(nodeLogId).setLastActiveTime(LocalDateTime.now());
        jobWorkRunNodeLogMapper.updateById(log);
    }


    /**
     * 写入 STARTED 事件结果，并首次记录执行器上报的实际开始时间。
     */
    public void markStarted(String nodeLogId, Long timestamp, String handleMsg) {
        if (StrUtil.isBlank(nodeLogId)) {
            return;
        }
        LocalDateTime eventTime = toEventTime(timestamp);
        jobWorkRunNodeLogMapper.update(null, Wrappers.lambdaUpdate(JobWorkRunNodeLogPo.class)
                .set(JobWorkRunNodeLogPo::getHandleCode, 0)
                .set(JobWorkRunNodeLogPo::getHandleMsg, handleMsg)
                .set(JobWorkRunNodeLogPo::getLastActiveTime, LocalDateTime.now())
                .set(eventTime != null, JobWorkRunNodeLogPo::getExecuteStartTime, eventTime)
                .eq(JobWorkRunNodeLogPo::getNodeLogId, nodeLogId));
    }

    /**
     * 写入日志最终结果并完成闭环；终态事件时间仅首次写入实际结束时间。
     */
    public void finish(String nodeLogId, Integer handleCode, String handleMsg) {
        finish(nodeLogId, handleCode, handleMsg, null);
    }

    public void finish(String nodeLogId, Integer handleCode, String handleMsg, Long timestamp) {
        if (StrUtil.isBlank(nodeLogId)) {
            return;
        }
        LocalDateTime eventTime = toEventTime(timestamp);
        LocalDateTime now = LocalDateTime.now();
        jobWorkRunNodeLogMapper.update(null, Wrappers.lambdaUpdate(JobWorkRunNodeLogPo.class)
                .set(JobWorkRunNodeLogPo::getHandleCode, handleCode)
                .set(JobWorkRunNodeLogPo::getHandleMsg, handleMsg)
                .set(JobWorkRunNodeLogPo::getLastActiveTime, now)
                .set(JobWorkRunNodeLogPo::getCallBackTime, now)
                .set(eventTime != null, JobWorkRunNodeLogPo::getExecuteEndTime, eventTime)
                .eq(JobWorkRunNodeLogPo::getNodeLogId, nodeLogId));
    }

    /**
     * 按日志标识关闭未闭环日志，并使用条件更新避免覆盖已完成日志。
     */
    public int closeUnclosed(String nodeLogId, String handleMsg) {
        if (StrUtil.isBlank(nodeLogId)) {
            return 0;
        }
        return jobWorkRunNodeLogMapper.update(null, Wrappers.lambdaUpdate(JobWorkRunNodeLogPo.class)
                .set(JobWorkRunNodeLogPo::getHandleCode, HandleCodeConstant.HANDLE_CODE_FAIL)
                .set(JobWorkRunNodeLogPo::getHandleMsg,
                        StrUtil.blankToDefault(handleMsg, "运行节点已结束，关闭旧的未闭环日志"))
                .set(JobWorkRunNodeLogPo::getLastActiveTime, LocalDateTime.now())
                .set(JobWorkRunNodeLogPo::getCallBackTime, LocalDateTime.now())
                .eq(JobWorkRunNodeLogPo::getNodeLogId, nodeLogId)
                .isNull(JobWorkRunNodeLogPo::getCallBackTime));
    }

    /**
     * 创建节点下发日志。
     */
    public void create(ExecuteNodeParam node, String executorAddress, Integer handleCode,
                       String handleMsg, LocalDateTime callBackTime) {
        JobWorkRunNodeLogPo log = cn.hutool.core.bean.BeanUtil.toBean(node, JobWorkRunNodeLogPo.class);
        log.setWorkId(node.getWorkId()).setNodeId(node.getNodeId()).setTurnDate(node.getTurnDate())
                .setExecutorAddress(executorAddress).setHandleCode(handleCode).setHandleMsg(handleMsg);
        LocalDateTime now = LocalDateTime.now();
        log.setCreateTime(now).setLastActiveTime(now).setCallBackTime(callBackTime);
        jobWorkRunNodeLogMapper.insert(log);
    }

    /**
     * 创建执行器事件对应的节点日志。
     */
    public void create(RunNodeLogEventParam event, Integer handleCode, String handleMsg,
                       LocalDateTime callBackTime) {
        create(event, handleCode, handleMsg, callBackTime, null, null);
    }

    /**
     * 创建执行器事件对应的节点日志，并写入事件携带的实际执行时间。
     */
    public void create(RunNodeLogEventParam event, Integer handleCode, String handleMsg,
                       LocalDateTime callBackTime, Long executeStartTimestamp, Long executeEndTimestamp) {
        JobWorkRunNodeLogPo log = new JobWorkRunNodeLogPo();
        log.setNodeLogId(event.getNodeLogId()).setWorkId(event.getWorkId()).setNodeId(event.getNodeId())
                .setTurnDate(event.getTurnDate()).setHandleCode(handleCode).setHandleMsg(handleMsg)
                .setExecuteStartTime(toEventTime(executeStartTimestamp))
                .setExecuteEndTime(toEventTime(executeEndTimestamp));
        LocalDateTime now = LocalDateTime.now();
        log.setCreateTime(now).setLastActiveTime(now).setCallBackTime(callBackTime);
        jobWorkRunNodeLogMapper.insert(log);
    }

    private LocalDateTime toEventTime(Long timestamp) {
        return timestamp == null ? null
                : Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

}
