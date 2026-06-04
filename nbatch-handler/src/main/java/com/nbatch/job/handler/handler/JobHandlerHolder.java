package com.nbatch.job.handler.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONObject;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.context.BatchJobHelper;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.handler.IJobHandlerHolder;
import com.nbatch.job.handler.enums.NodeTypeEnum;
import com.nbatch.job.handler.thread.BatchRunnable;
import com.nbatch.job.handler.thread.BatchThreadPoolExecutor;
import com.nbatch.job.handler.utils.BatchThreadPoolUtil;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
            return;
        }
        AtomicBoolean hasFail = new AtomicBoolean(false);
        while (!hasFail.get()) {
            List<ExecuteNodeParam> runnableNodeList = getRunnableNodeList(executeNodeParamList);
            if (CollUtil.isEmpty(runnableNodeList)) {
                break;
            }
            CountDownLatch latch = new CountDownLatch(runnableNodeList.size());
            for (ExecuteNodeParam nodeParam : runnableNodeList) {
                executeNode(workNodeParam, nodeParam, latch, hasFail);
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        if (hasFail.get()) {
            throw new RuntimeException("作业节点执行失败");
        }
        List<ExecuteNodeParam> waitNodeList = executeNodeParamList.stream()
                .filter(x -> x.getNodeRunStatus() == FlowRunStatusEnum.WAIT.getCode())
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(waitNodeList)) {
            throw new RuntimeException("存在未执行节点，可能依赖关系未满足");
        }
    }

    private void executeNode(ExecuteWorkParam workNodeParam, ExecuteNodeParam nodeParam,
                             CountDownLatch latch, AtomicBoolean hasFail) {
        if (hasFail.get()) {
            latch.countDown();
            return;
        }
        BatchThreadPoolExecutor batchThreadPoolExecutor = BatchThreadPoolUtil.getBatchThreadPoolExecutor(nodeParam.getNodeType());
        if (batchThreadPoolExecutor == null) {
            NodeTypeEnum nodeTypeEnum = NodeTypeEnum.getByCode(nodeParam.getNodeType());
            if (nodeTypeEnum == null) {
                BatchJobHelper.log("不支持该节点类型：{}", nodeParam.getNodeType());
                nodeParam.setNodeRunStatus(FlowRunStatusEnum.EXCEPTION.getCode());
                hasFail.set(true);
                latch.countDown();
                return;
            }
            Integer threadPoolNum = nodeTypeEnum.getThreadPoolNum();
            batchThreadPoolExecutor = BatchThreadPoolUtil.newThreadPoolExecutorDiscard(nodeParam.getNodeType(), threadPoolNum,
                    threadPoolNum, 30, TimeUnit.MINUTES, 1000);
        }
        JobNodeHandlerAdapter jobHandlerAdapter = jobHandlerAdapterMap.get(nodeParam.getNodeType());
        if (jobHandlerAdapter == null) {
            BatchJobHelper.log("未找到节点处理器：{}", nodeParam.getNodeType());
            nodeParam.setNodeRunStatus(FlowRunStatusEnum.EXCEPTION.getCode());
            hasFail.set(true);
            latch.countDown();
            return;
        }
        JSONObject cacheObj = getEntries(workNodeParam, nodeParam);
        batchThreadPoolExecutor.executeBatch(new BatchRunnable(cacheObj) {
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
                    hasFail.set(true);
                    BatchJobHelper.log("jobId:{},workId：{},节点执行异常：{}", workNodeParam.getJobId(),
                            workNodeParam.getWorkId(), e.getMessage());
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void runAfter() {
                try {
                    if (success) {
                        nodeParam.setNodeRunStatus(FlowRunStatusEnum.COMPLETE.getCode());
                    } else if (nodeParam.getNodeRunStatus() != FlowRunStatusEnum.EXCEPTION.getCode()) {
                        nodeParam.setNodeRunStatus(FlowRunStatusEnum.EXCEPTION.getCode());
                        hasFail.set(true);
                    }
                } finally {
                    latch.countDown();
                }
            }
        });

    }

    /**
     * 获取可运行的节点列表
     */
    private List<ExecuteNodeParam> getRunnableNodeList(List<ExecuteNodeParam> executeNodeParamList) {
        Set<String> completeNodeIdSet = executeNodeParamList.stream()
                .filter(x -> x.getNodeRunStatus() == FlowRunStatusEnum.COMPLETE.getCode())
                .map(ExecuteNodeParam::getNodeId)
                .collect(Collectors.toSet());

        return executeNodeParamList.stream()
                .filter(x -> x.getNodeRunStatus() == FlowRunStatusEnum.WAIT.getCode())
                .filter(x -> CollUtil.isEmpty(x.getNodeRelationIdList())
                        || completeNodeIdSet.containsAll(x.getNodeRelationIdList()))
                .collect(Collectors.toList());
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



