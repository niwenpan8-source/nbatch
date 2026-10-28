package com.nbatch.job.admin.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.service.IJobWorkFlowEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 按作业聚合并推送流程图运行态。只发送状态、翻牌日期和剩余重试次数，
 * 流程结构仍由页面初始化和手动刷新接口负责加载。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobWorkFlowEventServiceImpl implements IJobWorkFlowEventService {

    /**
     * SSE 连接最长存活时间。超时后浏览器会自动重连并重新获取完整轻量快照，
     * 防止代理或客户端异常断开后服务端永久保留连接。
     */
    private static final long SSE_TIMEOUT = 15 * 60 * 1000L;

    /**
     * 心跳间隔
     */
    private static final long HEARTBEAT_INTERVAL = 30 * 1000L;

    /**
     * 通知间隔
     */
    private static final long NOTICE_INTERVAL = 2000L;

    private final IJobWorkMapper jobWorkMapper;
    private final IJobWorkNodeMapper jobWorkNodeMapper;

    @Resource(name = "jobWorkFlowEventTaskScheduler")
    private ThreadPoolTaskScheduler taskScheduler;

    private ScheduledFuture<?> runtimeUpdateTask;
    private ScheduledFuture<?> heartbeatTask;

    /**
     * 按作业 ID 聚合的 SSE 连接。
     */
    private final Map<String, Set<SseEmitter>> emittersByWorkId = new ConcurrentHashMap<>();
    private final Map<String, RuntimeState> lastRuntimeStates = new ConcurrentHashMap<>();
    private final AtomicLong eventSequence = new AtomicLong();

    /**
     * @param workId 作业 ID
     */
    @Override
    public SseEmitter subscribe(String workId) {
        RuntimeState state = loadRuntimeState(workId);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitter.onCompletion(() -> removeEmitter(workId, emitter));
        emitter.onTimeout(() -> removeEmitter(workId, emitter));
        emitter.onError(error -> removeEmitter(workId, emitter));

        if (state == null) {
            emitter.complete();
            return emitter;
        }
        emittersByWorkId.computeIfAbsent(workId, key -> new CopyOnWriteArraySet<>()).add(emitter);
        lastRuntimeStates.put(workId, state);
        send(workId, emitter, toPayload(workId, state, state.nodes));
        return emitter;
    }

    @PostConstruct
    public void startRuntimeTasks() {
        runtimeUpdateTask = taskScheduler.scheduleWithFixedDelay(this::publishRuntimeUpdates, NOTICE_INTERVAL);
        heartbeatTask = taskScheduler.scheduleWithFixedDelay(this::evictDisconnectedEmitters, HEARTBEAT_INTERVAL);
    }

    @PreDestroy
    public void stopRuntimeTasks() {
        cancelTask(runtimeUpdateTask);
        cancelTask(heartbeatTask);
    }

    /**
     * 取消任务
     */
    private void cancelTask(ScheduledFuture<?> task) {
        if (task != null) {
            task.cancel(false);
        }
    }

    /**
     * 将 1000ms 内的多次数据库状态变化合并为一条事件，只轮询当前有浏览器订阅的作业。
     */
    public void publishRuntimeUpdates() {
        for (String workId : new LinkedHashSet<>(emittersByWorkId.keySet())) {
            Set<SseEmitter> emitters = emittersByWorkId.get(workId);
            if (emitters == null || emitters.isEmpty()) {
                lastRuntimeStates.remove(workId);
                continue;
            }
            try {
                // 加载当前作业的运行态
                RuntimeState current = loadRuntimeState(workId);
                if (current == null) {
                    completeWorkEmitters(workId);
                    continue;
                }
                // 插入新值返回旧值
                RuntimeState previous = lastRuntimeStates.put(workId, current);
                // 旧值为空，直接发送
                if (previous == null) {
                    broadcast(workId, toPayload(workId, current, current.nodes));
                    continue;
                }
                // 得到发生改变的节点
                List<NodeRuntimeState> changedNodes = changedNodes(previous, current);
                if (!previous.sameWorkState(current) || !changedNodes.isEmpty()) {
                    broadcast(workId, toPayload(workId, current, changedNodes));
                }
            } catch (Exception exception) {
                log.warn("推送作业流程运行态失败，workId：{}", workId, exception);
            }
        }
    }

    /**
     * 即使运行态没有变化也向客户端写入注释事件。借此识别浏览器异常退出、网络中断等场景，
     * 并清理对应的连接和运行态快照。
     */
    public void evictDisconnectedEmitters() {
        for (String workId : new LinkedHashSet<>(emittersByWorkId.keySet())) {
            Set<SseEmitter> emitters = emittersByWorkId.get(workId);
            if (emitters == null || emitters.isEmpty()) {
                lastRuntimeStates.remove(workId);
                continue;
            }
            for (SseEmitter emitter : emitters) {
                sendHeartbeat(workId, emitter);
            }
        }
    }

    /**
     * 加载作业节点运行状态
     */
    private RuntimeState loadRuntimeState(String workId) {
        JobWorkPo work = jobWorkMapper.selectOne(Wrappers.lambdaQuery(JobWorkPo.class)
                .select(JobWorkPo::getWorkId, JobWorkPo::getRunWorkStatus, JobWorkPo::getTurnDate)
                .eq(JobWorkPo::getWorkId, workId)
                .last("LIMIT 1"));
        if (work == null) {
            return null;
        }
        List<JobWorkNodePo> nodes = jobWorkNodeMapper.selectList(Wrappers.lambdaQuery(JobWorkNodePo.class)
                .select(JobWorkNodePo::getNodeId, JobWorkNodePo::getNodeRunStatus,
                        JobWorkNodePo::getTurnDate, JobWorkNodePo::getCurrentRetryTimes)
                .eq(JobWorkNodePo::getWorkId, workId)
                .orderByAsc(JobWorkNodePo::getNodeId));
        Map<String, NodeRuntimeState> nodeStates = new LinkedHashMap<>();
        for (JobWorkNodePo node : nodes) {
            nodeStates.put(node.getNodeId(), new NodeRuntimeState(node.getNodeId(), node.getNodeRunStatus(),
                    formatTurnDate(node.getTurnDate()), node.getCurrentRetryTimes()));
        }
        return new RuntimeState(work.getRunWorkStatus(), formatTurnDate(work.getTurnDate()), nodeStates);
    }

    private String formatTurnDate(java.util.Date turnDate) {
        return turnDate == null ? null : DateUtil.formatDate(turnDate);
    }

    /**
     * 获取发生变化的节点运行态。
     * @param previous 上一次的运行态
     * @param current 当前的运行态
     * @return 发生变化的节点运行态
     */
    private List<NodeRuntimeState> changedNodes(RuntimeState previous, RuntimeState current) {
        List<NodeRuntimeState> changed = new ArrayList<>();
        for (Map.Entry<String, NodeRuntimeState> entry : current.nodes.entrySet()) {
            if (!Objects.equals(entry.getValue(), previous.nodes.get(entry.getKey()))) {
                changed.add(entry.getValue());
            }
        }
        return changed;
    }

    /**
     * 将作业运行态转换为事件负载。
     * @param workId 作业 ID
     * @param state 作业运行态
     * @param nodes 作业节点运行态
     * @return 事件负载
     */
    private Map<String, Object> toPayload(String workId, RuntimeState state, Map<String, NodeRuntimeState> nodes) {
        return toPayload(workId, state, new ArrayList<>(nodes.values()));
    }

    private Map<String, Object> toPayload(String workId, RuntimeState state, List<NodeRuntimeState> nodes) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", String.valueOf(eventSequence.incrementAndGet()));
        payload.put("workId", workId);
        payload.put("serverTime", System.currentTimeMillis());
        Map<String, Object> work = new LinkedHashMap<>();
        work.put("runStatus", state.runStatus);
        work.put("turnDate", state.turnDate);
        payload.put("work", work);
        List<Map<String, Object>> nodePayload = new ArrayList<>();
        for (NodeRuntimeState node : nodes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nodeId", node.nodeId);
            item.put("runStatus", node.runStatus);
            item.put("turnDate", node.turnDate);
            item.put("retryTimes", node.retryTimes);
            nodePayload.add(item);
        }
        payload.put("nodes", nodePayload);
        return payload;
    }

    /**
     * 广播发送
     */
    private void broadcast(String workId, Map<String, Object> payload) {
        Set<SseEmitter> emitters = emittersByWorkId.getOrDefault(workId, Collections.emptySet());
        for (SseEmitter emitter : emitters) {
            send(workId, emitter, payload);
        }
    }

    /**
     * 发送
     */
    private void send(String workId, SseEmitter emitter, Map<String, Object> payload) {
        try {
            emitter.send(SseEmitter.event().id((String) payload.get("eventId"))
                    .name("runtime-update").data(payload));
        } catch (IOException | IllegalStateException exception) {
            removeFailedEmitter(workId, emitter);
        }
    }

    /**
     * 发送心跳
     */
    private void sendHeartbeat(String workId, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().comment("keepalive"));
        } catch (IOException | IllegalStateException exception) {
            removeFailedEmitter(workId, emitter);
        }
    }

    /**
     * 移除心跳失败的emitter
     */
    private void removeFailedEmitter(String workId, SseEmitter emitter) {
        removeEmitter(workId, emitter);
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // 连接已被容器关闭，无需再次处理。
        }
    }

    /**
     * 移除指定作业的 SSE 连接。
     * @param workId 作业 ID
     * @param emitter SSE 连接
     */
    private void removeEmitter(String workId, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersByWorkId.get(workId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByWorkId.remove(workId, emitters);
            lastRuntimeStates.remove(workId);
        }
    }

    /**
     * 完成指定作业的 SSE 连接。
     * @param workId 作业 ID
     */
    private void completeWorkEmitters(String workId) {
        Set<SseEmitter> emitters = emittersByWorkId.remove(workId);
        lastRuntimeStates.remove(workId);
        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                emitter.complete();
            }
        }
    }

    /**
     * 作业运行态。
     */
    private static final class RuntimeState {
        private final Integer runStatus;
        private final String turnDate;
        private final Map<String, NodeRuntimeState> nodes;

        private RuntimeState(Integer runStatus, String turnDate, Map<String, NodeRuntimeState> nodes) {
            this.runStatus = runStatus;
            this.turnDate = turnDate;
            this.nodes = nodes;
        }

        /**
         * 判断作业运行态是否相同。
         * @param other 其他作业运行态
         * @return 是否相同
         */
        private boolean sameWorkState(RuntimeState other) {
            return other != null && Objects.equals(runStatus, other.runStatus)
                    && Objects.equals(turnDate, other.turnDate);
        }
    }

    /**
     * 节点运行态。
     */
    private static final class NodeRuntimeState {
        private final String nodeId;
        private final Integer runStatus;
        private final String turnDate;
        private final Integer retryTimes;

        private NodeRuntimeState(String nodeId, Integer runStatus, String turnDate, Integer retryTimes) {
            this.nodeId = nodeId;
            this.runStatus = runStatus;
            this.turnDate = turnDate;
            this.retryTimes = retryTimes;
        }

        /**
         * 判断节点运行态是否相同。
         * @param other 其他节点运行态
         * @return 是否相同
         */
        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof NodeRuntimeState)) {
                return false;
            }
            NodeRuntimeState that = (NodeRuntimeState) other;
            return Objects.equals(nodeId, that.nodeId) && Objects.equals(runStatus, that.runStatus)
                    && Objects.equals(turnDate, that.turnDate) && Objects.equals(retryTimes, that.retryTimes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeId, runStatus, turnDate, retryTimes);
        }
    }
}
