package com.nbatch.job.handler.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONObject;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.context.BatchJobHelper;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.handler.IJobHandlerHolder;
import com.nbatch.job.core.enums.NodeTypeEnum;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.thread.BatchRunnable;
import com.nbatch.job.handler.thread.BatchThreadPoolExecutor;
import com.nbatch.job.handler.utils.BatchThreadPoolUtil;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.nbatch.job.handler.enums.ExceptionCodeEnum.SYSTEM_ERROR;
/**
 * @description: job handler 容器
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@RequiredArgsConstructor
public class JobHandlerHolder implements IJobHandlerHolder {

    private static final long NODE_AWAIT_TIMEOUT = 24;

    private static final TimeUnit NODE_AWAIT_TIME_UNIT = TimeUnit.HOURS;

    private final Map<String, JobNodeHandlerAdapter> jobHandlerAdapterMap;

    /**
     * 处理作业节点
     */
    @Override
    public void handle(ExecuteWorkParam workNodeParam) {
        List<ExecuteNodeParam> executeNodeParamList = workNodeParam.getExecuteNodeParamList();
        if (CollUtil.isEmpty(executeNodeParamList)) {
            BatchJobHelper.log("jobId:{},workId:{}, no executable node", workNodeParam.getJobId(),
                    workNodeParam.getWorkId());
            throw new HandlerException(SYSTEM_ERROR.getCode(), "execute node params is empty");
        }

        CountDownLatch latch = new CountDownLatch(executeNodeParamList.size());
        for (ExecuteNodeParam nodeParam : executeNodeParamList) {
            executeNode(workNodeParam, nodeParam, latch);
        }
        awaitNodes(workNodeParam, latch);
        checkNodeResults(executeNodeParamList);
    }

    /**
     * 等待所有作业节点执行完成
     */
    private void awaitNodes(ExecuteWorkParam workNodeParam, CountDownLatch latch) {
        try {
            boolean completed = latch.await(NODE_AWAIT_TIMEOUT, NODE_AWAIT_TIME_UNIT);
            if (!completed) {
                throw new HandlerException(SYSTEM_ERROR.getCode(),
                        "wait executable nodes timeout, jobId:" + workNodeParam.getJobId()
                                + ", workId:" + workNodeParam.getWorkId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HandlerException(SYSTEM_ERROR.getCode(), e);
        }
    }

    /**
     * 检查所有作业节点执行结果
     */
    private void checkNodeResults(List<ExecuteNodeParam> executeNodeParamList) {
        StringBuilder failMsg = new StringBuilder();
        for (ExecuteNodeParam nodeParam : executeNodeParamList) {
            if (nodeParam == null) {
                appendFailMessage(failMsg, "null", null);
                continue;
            }
            Integer status = nodeParam.getNodeRunStatus();
            if (!isCompleteStatus(status)) {
                appendFailMessage(failMsg, getNodeKey(nodeParam), status);
            }
        }
        if (failMsg.length() > 0) {
            throw new HandlerException(SYSTEM_ERROR.getCode(), "node execute failed: " + failMsg);
        }
    }

    private void appendFailMessage(StringBuilder failMsg, String nodeKey, Integer status) {
        if (failMsg.length() > 0) {
            failMsg.append("; ");
        }
        failMsg.append("node=").append(nodeKey).append(", status=").append(status);
    }

    private boolean isCompleteStatus(Integer status) {
        return Integer.valueOf(FlowRunStatusEnum.COMPLETE.getCode()).equals(status);
    }

    private boolean isExceptionStatus(Integer status) {
        return Integer.valueOf(FlowRunStatusEnum.EXCEPTION.getCode()).equals(status);
    }

    /**
     * 执行作业节点
     */
    private void executeNode(ExecuteWorkParam workNodeParam, ExecuteNodeParam nodeParam,
                             CountDownLatch latch) {
        try {
            if (nodeParam == null) {
                latch.countDown();
                return;
            }
            BatchThreadPoolExecutor executor = getExecutor(nodeParam);
            if (executor == null) {
                markNodeFailed(nodeParam, latch, "unsupported node type:" + nodeParam.getNodeType());
                return;
            }
            JobNodeHandlerAdapter handlerAdapter = jobHandlerAdapterMap.get(nodeParam.getNodeType());
            if (handlerAdapter == null) {
                markNodeFailed(nodeParam, latch, "node handler not found:" + nodeParam.getNodeType());
                return;
            }
            submitNode(workNodeParam, nodeParam, handlerAdapter, executor, latch);
        } catch (Exception e) {
            if (nodeParam != null) {
                nodeParam.setNodeRunStatus(FlowRunStatusEnum.EXCEPTION.getCode());
            }
            latch.countDown();
            BatchJobHelper.log("jobId:{},workId:{}, node submit error:{}", workNodeParam.getJobId(),
                    workNodeParam.getWorkId(), e.getMessage());
        }
    }

    private BatchThreadPoolExecutor getExecutor(ExecuteNodeParam nodeParam) {
        NodeTypeEnum nodeTypeEnum = NodeTypeEnum.getByCode(nodeParam.getNodeType());
        if (nodeTypeEnum == null) {
            return null;
        }
        Integer threadPoolNum = nodeTypeEnum.getThreadPoolNum();
        return BatchThreadPoolUtil.newThreadPoolExecutorDiscard(getNodeThreadPoolKey(nodeParam), threadPoolNum,
                threadPoolNum, 30, TimeUnit.MINUTES, 1000);
    }

    private void markNodeFailed(ExecuteNodeParam nodeParam, CountDownLatch latch, String message) {
        BatchJobHelper.log(message);
        nodeParam.setNodeRunStatus(FlowRunStatusEnum.EXCEPTION.getCode());
        latch.countDown();
    }

    private void submitNode(ExecuteWorkParam workNodeParam, ExecuteNodeParam nodeParam,
                            JobNodeHandlerAdapter handlerAdapter, BatchThreadPoolExecutor executor,
                            CountDownLatch latch) {
        BatchRunnable batchRunnable = buildNodeRunnable(workNodeParam, nodeParam, handlerAdapter, latch);
        boolean submitted = executor.executeBatch(batchRunnable);
        if (!submitted) {
            nodeParam.setNodeRunStatus(FlowRunStatusEnum.EXCEPTION.getCode());
            latch.countDown();
            BatchJobHelper.log("jobId:{},workId:{}, node submit thread pool failed, nodeId:{}", workNodeParam.getJobId(),
                    workNodeParam.getWorkId(), nodeParam.getNodeId());
        }
    }

    private BatchRunnable buildNodeRunnable(ExecuteWorkParam workNodeParam, ExecuteNodeParam nodeParam,
                                            JobNodeHandlerAdapter handlerAdapter, CountDownLatch latch) {
        JSONObject cacheObj = getEntries(workNodeParam, nodeParam);
        return new BatchRunnable(cacheObj) {
            private final AtomicBoolean finished = new AtomicBoolean(false);
            private boolean success = false;

            @Override
            public void runBefore() {
                nodeParam.setNodeRunStatus(FlowRunStatusEnum.RUNNING.getCode());
            }

            @Override
            public void runBatch() {
                try {
                    handlerAdapter.execute(nodeParam);
                    success = true;
                } catch (Exception e) {
                    nodeParam.setNodeRunStatus(FlowRunStatusEnum.EXCEPTION.getCode());
                    BatchJobHelper.log("jobId:{},workId:{}, node execute error:{}", workNodeParam.getJobId(),
                            workNodeParam.getWorkId(), e.getMessage());
                    if (e instanceof HandlerException) {
                        throw (HandlerException) e;
                    }
                    throw new HandlerException(SYSTEM_ERROR.getCode(), e);
                }
            }

            @Override
            public void runAfter() {
                try {
                    if (success) {
                        nodeParam.setNodeRunStatus(FlowRunStatusEnum.COMPLETE.getCode());
                    } else if (!isExceptionStatus(nodeParam.getNodeRunStatus())) {
                        nodeParam.setNodeRunStatus(FlowRunStatusEnum.EXCEPTION.getCode());
                    }
                } finally {
                    finish();
                }
            }

            @Override
            public void runStop() {
                try {
                    nodeParam.setNodeRunStatus(FlowRunStatusEnum.STOPPED.getCode());
                } finally {
                    finish();
                }
            }

            private void finish() {
                if (finished.compareAndSet(false, true)) {
                    latch.countDown();
                }
            }
        };
    }

    private String getNodeThreadPoolKey(ExecuteNodeParam nodeParam) {
        // return nodeParam.getNodeType() + ":" + getNodeKey(nodeParam);
        // 目前只根据节点类型分配线程池
        return nodeParam.getNodeType();
    }

    private String getNodeKey(ExecuteNodeParam nodeParam) {
        if (nodeParam.getNodeId() != null) {
            return nodeParam.getNodeId();
        }
        if (nodeParam.getRunNodeId() != null) {
            return nodeParam.getRunNodeId();
        }
        return String.valueOf(System.identityHashCode(nodeParam));
    }

    /**
     * 获取缓存对象
     */
    private JSONObject getEntries(ExecuteWorkParam workNodeParam, ExecuteNodeParam nodeParam) {
        JSONObject cacheObj = new JSONObject();
        cacheObj.putOpt("jobId", workNodeParam.getJobId());
        cacheObj.putOpt("logId", workNodeParam.getJobLogId());
        cacheObj.putOpt("nodeLogId", nodeParam.getNodeLogId());
        cacheObj.putOpt("workId", workNodeParam.getWorkId());
        cacheObj.putOpt("runWorkId", workNodeParam.getRunWorkId());
        cacheObj.putOpt("nodeId", nodeParam.getNodeId());
        cacheObj.putOpt("runNodeId", nodeParam.getRunNodeId());
        cacheObj.putOpt("turnDate", nodeParam.getTurnDate());
        cacheObj.putOpt("workType", workNodeParam.getWorkType());
        return cacheObj;
    }

}
