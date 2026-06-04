package com.nbatch.job.handler.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONObject;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.context.BatchJobHelper;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.handler.IJobHandlerHolder;
import com.nbatch.job.handler.enums.NodeTypeEnum;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.thread.BatchRunnable;
import com.nbatch.job.handler.thread.BatchThreadPoolExecutor;
import com.nbatch.job.handler.utils.BatchThreadPoolUtil;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.nbatch.job.handler.enums.ExceptionCodeEnum.SYSTEM_ERROR;

/**
 * @description: job handler 容器
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@RequiredArgsConstructor
public class JobHandlerHolder implements IJobHandlerHolder {


    private final Map<String, JobNodeHandlerAdapter> jobHandlerAdapterMap;

    /**
     * 测试
     */
    @Override
    public void handle(ExecuteWorkParam workNodeParam) {
        // 每种节点类型使用一种线程池进行运行
        List<ExecuteNodeParam> executeNodeParamList = workNodeParam.getExecuteNodeParamList();
        if (CollUtil.isEmpty(executeNodeParamList)) {
            // 日志缓存使用线程变量，InheritableThreadLocal，支持子线程继承父线程的日志缓存
            BatchJobHelper.log("jobId:{},workId：{},此次执行没有运行节点不需要运行", workNodeParam.getJobId(),
                    workNodeParam.getWorkId());
            throw new HandlerException(SYSTEM_ERROR.getCode(), "执行节点参数为空");
        }

        // 这个的作用是起到了屏障的作用，当前 JobHandlerHolder.handle() 需要等本轮可执行节点完成后，
        // 才能判断下一批依赖节点是否可执行
        CountDownLatch latch = new CountDownLatch(executeNodeParamList.size());
        for (ExecuteNodeParam nodeParam : executeNodeParamList) {
            executeNode(workNodeParam, nodeParam, latch);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HandlerException(SYSTEM_ERROR.getCode(), e);
        }
    }

    private void executeNode(ExecuteWorkParam workNodeParam, ExecuteNodeParam nodeParam,
                             CountDownLatch latch) {
        String threadPoolKey = getNodeThreadPoolKey(nodeParam);
        BatchThreadPoolExecutor batchThreadPoolExecutor = BatchThreadPoolUtil.getBatchThreadPoolExecutor(threadPoolKey);
        if (batchThreadPoolExecutor == null) {
            NodeTypeEnum nodeTypeEnum = NodeTypeEnum.getByCode(nodeParam.getNodeType());
            if (nodeTypeEnum == null) {
                BatchJobHelper.log("不支持该节点类型：{}", nodeParam.getNodeType());
                nodeParam.setNodeRunStatus(FlowRunStatusEnum.EXCEPTION.getCode());
                latch.countDown();
                return;
            }
            Integer threadPoolNum = nodeTypeEnum.getThreadPoolNum();
            batchThreadPoolExecutor = BatchThreadPoolUtil.newThreadPoolExecutorDiscard(threadPoolKey, threadPoolNum,
                    threadPoolNum, 30, TimeUnit.MINUTES, 1000);
        }
        JobNodeHandlerAdapter jobHandlerAdapter = jobHandlerAdapterMap.get(nodeParam.getNodeType());
        if (jobHandlerAdapter == null) {
            BatchJobHelper.log("未找到节点处理器：{}", nodeParam.getNodeType());
            nodeParam.setNodeRunStatus(FlowRunStatusEnum.EXCEPTION.getCode());
            latch.countDown();
            return;
        }
        JSONObject cacheObj = getEntries(workNodeParam, nodeParam);
        BatchRunnable batchRunnable = new BatchRunnable(cacheObj) {
            private boolean success = false;

            @Override
            public void runBefore() {
                nodeParam.setNodeRunStatus(FlowRunStatusEnum.RUNNING.getCode());
            }

            @Override
            public void runBatch() {
                try {
                    jobHandlerAdapter.execute(nodeParam);
                    success = true;
                } catch (Exception e) {
                    nodeParam.setNodeRunStatus(FlowRunStatusEnum.EXCEPTION.getCode());
                    BatchJobHelper.log("jobId:{},workId：{},节点执行异常：{}", workNodeParam.getJobId(),
                            workNodeParam.getWorkId(), e.getMessage());
                    throw new HandlerException(SYSTEM_ERROR.getCode(), e);
                }
            }

            @Override
            public void runAfter() {
                try {
                    if (success) {
                        nodeParam.setNodeRunStatus(FlowRunStatusEnum.COMPLETE.getCode());
                    } else if (nodeParam.getNodeRunStatus() != FlowRunStatusEnum.EXCEPTION.getCode()) {
                        nodeParam.setNodeRunStatus(FlowRunStatusEnum.EXCEPTION.getCode());
                    }
                } finally {
                    latch.countDown();
                }
            }
        };

        boolean submitted = batchThreadPoolExecutor.executeBatch(batchRunnable);
        if (!submitted) {
            nodeParam.setNodeRunStatus(FlowRunStatusEnum.EXCEPTION.getCode());
            latch.countDown();
            BatchJobHelper.log("jobId:{},workId：{},节点提交线程池失败，nodeId:{}", workNodeParam.getJobId(),
                    workNodeParam.getWorkId(), nodeParam.getNodeId());
        }

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
     * 获取可运行的节点列表
     */
    private List<ExecuteNodeParam> getRunnableNodeList(List<ExecuteNodeParam> waitNodeList, Set<String> completeNodeIdSet) {
        List<ExecuteNodeParam> runnableNodeList = new ArrayList<>();
        for (ExecuteNodeParam nodeParam : waitNodeList) {
            if (CollUtil.isEmpty(nodeParam.getNodeRelationIdList())
                    || completeNodeIdSet.containsAll(nodeParam.getNodeRelationIdList())) {
                runnableNodeList.add(nodeParam);
            }
        }
        return runnableNodeList;
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
        cacheObj.putOpt("workType", workNodeParam.getWorkType());
        return cacheObj;
    }

}



