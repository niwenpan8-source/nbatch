package com.nbatch.job.handler.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONObject;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.context.BatchJobHelper;
import com.nbatch.job.core.handler.IJobHandlerHolder;
import com.nbatch.job.handler.enums.NodeTypeEnum;
import com.nbatch.job.handler.thread.BatchRunnable;
import com.nbatch.job.handler.thread.BatchThreadPoolExecutor;
import com.nbatch.job.handler.utils.BatchThreadPoolUtil;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
        for (ExecuteNodeParam nodeParam : executeNodeParamList) {
            BatchThreadPoolExecutor batchThreadPoolExecutor = BatchThreadPoolUtil.getBatchThreadPoolExecutor(nodeParam.getNodeType());
            if (batchThreadPoolExecutor == null) {
                NodeTypeEnum nodeTypeEnum = NodeTypeEnum.getByCode(nodeParam.getNodeType());
                if (nodeTypeEnum == null) {
                    BatchJobHelper.log("不支持该节点类型：{}", nodeParam.getNodeType());
                    continue;
                }
                Integer threadPoolNum = nodeTypeEnum.getThreadPoolNum();
                batchThreadPoolExecutor = BatchThreadPoolUtil.newThreadPoolExecutorDiscard(nodeParam.getNodeType(), threadPoolNum,
                        threadPoolNum, 30, TimeUnit.MINUTES, 1000);
            }
            JobNodeHandlerAdapter jobHandlerAdapter = jobHandlerAdapterMap.get(nodeParam.getNodeType());
            JSONObject cacheObj = getEntries(workNodeParam, nodeParam);
            batchThreadPoolExecutor.executeBatch(new BatchRunnable(cacheObj) {
                @Override
                public void run() {
                    try {
                        jobHandlerAdapter.execute(nodeParam);
                    } catch (Exception e) {
                        BatchJobHelper.log("jobId:{},workId：{},节点执行异常：{}", workNodeParam.getJobId(),
                                workNodeParam.getWorkId(), e.getMessage());
                        throw new RuntimeException(e);
                    }
                }
            });

        }
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
        return cacheObj;
    }
}


