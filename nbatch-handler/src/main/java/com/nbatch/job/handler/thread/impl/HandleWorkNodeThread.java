package com.nbatch.job.handler.thread.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.context.BatchJobHelper;
import com.nbatch.job.core.enums.RunWorkStatusEnum;
import com.nbatch.job.core.enums.WorkTypeEnum;
import com.nbatch.job.core.executor.BatchJobExecutor;
import com.nbatch.job.handler.enums.NodeTypeEnum;
import com.nbatch.job.handler.handler.JobNodeHandlerAdapter;
import com.nbatch.job.handler.thread.BatchRunnable;
import com.nbatch.job.handler.thread.BatchThreadPoolExecutor;
import com.nbatch.job.handler.utils.BatchThreadPoolUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @description: 处理作业节点线程
 * @author: Mr.ni
 * @date: 2025/12/5
 */
@Slf4j
@RequiredArgsConstructor
public class HandleWorkNodeThread implements SmartInitializingSingleton, DisposableBean {

    private final Map<String, JobNodeHandlerAdapter> jobHandlerAdapterMap;

    /**
     * job results handle work node queue
     */
    private final ConcurrentHashMap<String, ExecuteWorkParam> workNodeExecuteMap
            = new ConcurrentHashMap<>();

    @Override
    public void destroy() {
        this.start();
    }

    @Override
    public void afterSingletonsInstantiated() {
        this.toStop();
    }
    
    public void pushCallBack(ExecuteWorkParam param) {
        workNodeExecuteMap.put(param.getRunWorkId(), param);
        log.debug(">>>>>>>>>>> job, push handle work node, runWorkId:{}", param.getRunWorkId());
    }

    /**
     * handle work node thread
     */
    private Thread triggerCallbackThread;
    private volatile boolean toStop = false;

    public void start() {

        // valid
        if (BatchJobExecutor.getAdminBizList() == null) {
            log.warn(">>>>>>>>>>> job, executor handle work node config fail, adminAddresses is null.");
            return;
        }

        // handle work node
        triggerCallbackThread = new Thread(() -> {
            // normal handle work node
            while (!toStop) {
                try {
                    if (ObjUtil.isNotEmpty(workNodeExecuteMap)) {
                        List<ExecuteWorkParam> executeWorkParamList = new ArrayList<>(workNodeExecuteMap.values());
                        if (CollUtil.isNotEmpty(executeWorkParamList)) {
                            executeWorkNode(executeWorkParamList);
                        }
                        executeWorkParamList.clear();
                    }
                } catch (Throwable e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                    }
                }
            }

            // 最后会执行的线程，这里我们需要发送作业日志，如果存在还在运行的作业，需要将这些作业状态改回去
            try {
                if (ObjUtil.isNotEmpty(workNodeExecuteMap)) {
                    List<ExecuteWorkParam> executeWorkParamList = new ArrayList<>(workNodeExecuteMap.values());
                    if (CollUtil.isNotEmpty(executeWorkParamList)) {
                        executeWorkNode(executeWorkParamList);
                    }
                    executeWorkParamList.clear();
                }
            } catch (Throwable e) {
                if (!toStop) {
                    log.error(e.getMessage(), e);
                }
            }
            log.info(">>>>>>>>>>> job, executor callback thread destroy.");

        });
        triggerCallbackThread.setDaemon(true);
        triggerCallbackThread.setName("job, executor TriggerCallbackThread");
        triggerCallbackThread.start();

    }

    public void toStop() {
        toStop = true;
        // 停止工作节点处理线程。首先中断线程执行，然后等待线程完全终止。
        // 如果在等待过程中发生异常，则记录错误日志。这是典型的线程优雅关闭操作。
        if (triggerCallbackThread != null) {
            triggerCallbackThread.interrupt();
            try {
                triggerCallbackThread.join();
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }
    
    private void executeWorkNode(List<ExecuteWorkParam> executeWorkParamList) {
        for (ExecuteWorkParam executeWorkParam : executeWorkParamList) {
            ExecuteWorkParam needExecuteRunNode = getNeedExecuteRunNode(executeWorkParam);
            // 每种节点类型使用一种线程池进行运行
            assert needExecuteRunNode != null;
            List<ExecuteNodeParam> executeNodeParamList = needExecuteRunNode.getExecuteNodeParamList();
            if (CollUtil.isEmpty(executeNodeParamList)) {
                // 日志缓存使用线程变量，InheritableThreadLocal，支持子线程继承父线程的日志缓存
                BatchJobHelper.log("jobId:{},workId：{},此次执行没有运行节点不需要运行", needExecuteRunNode.getJobId(),
                        needExecuteRunNode.getWorkId());
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
                JSONObject cacheObj = getEntries(needExecuteRunNode, nodeParam);
                //
                batchThreadPoolExecutor.executeBatch(new BatchRunnable(cacheObj) {
                    @Override
                    public void runBefore() {
                        ExecuteWorkParam cacheExecuteWorkParam = workNodeExecuteMap.get(nodeParam.getRunWorkId());
                        for (ExecuteNodeParam executeNodeParam : cacheExecuteWorkParam.getExecuteNodeParamList()) {
                            if (StrUtil.equals(executeNodeParam.getRunNodeId(), nodeParam.getNodeId())) {
                                nodeParam.setNodeRunStatus(RunWorkStatusEnum.RUNNING.getCode());
                            }
                        }
                    }

                    @Override
                    public void runBatch() {
                        try {
                            jobHandlerAdapter.execute(nodeParam);
                        } catch (Exception e) {
                            BatchJobHelper.log("jobId:{},workId：{},节点执行异常：{}", needExecuteRunNode.getJobId(),
                                    needExecuteRunNode.getWorkId(), e.getMessage());
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void  runAfter() {
                        ExecuteWorkParam cacheExecuteWorkParam = workNodeExecuteMap.get(nodeParam.getRunWorkId());
                        for (ExecuteNodeParam executeNodeParam : cacheExecuteWorkParam.getExecuteNodeParamList()) {
                            if (StrUtil.equals(executeNodeParam.getRunNodeId(), nodeParam.getNodeId())) {
                                nodeParam.setNodeRunStatus(RunWorkStatusEnum.RUNNING.getCode());
                            }
                        }
                    }
                });

            }
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
        cacheObj.putOpt("workType", workNodeParam.getWorkType());
        return cacheObj;
    }

    /**
     * 得到需要执行的运行节点
     */
    private ExecuteWorkParam getNeedExecuteRunNode(ExecuteWorkParam param) {
        
        if (param == null) {
            log.info("执行作业对象为空！");
            BatchJobHelper.log("执行作业对象为空！");
            return null;
        }
        if (CollUtil.isEmpty(param.getExecuteNodeParamList())) {
            log.info("执行作业节点为空！");
            BatchJobHelper.log("执行作业节点为空！");
            return null;
        }
        ExecuteWorkParam copyExecuteWorkParam = BeanUtil.toBean(param, ExecuteWorkParam.class);
        // 当前作业翻牌日期
        Date turnDate = copyExecuteWorkParam.getTurnDate();

        // 查找作业当中没有运行同时翻牌时间必须和作业时间相同的节点,对于节点只有运行状态为等待中的节点才需要执行
        List<ExecuteNodeParam> enableExecuteNode = copyExecuteWorkParam.getExecuteNodeParamList().stream()
                .filter(x -> x.getNodeRunStatus() == RunWorkStatusEnum.WAIT.getCode()
                        && DateUtil.compare(x.getTurnDate(), turnDate) == 0).collect(Collectors.toList());

        List<String> allRunNodeIdByTypeList = getExecuteCompileteNodeIdByTypeList(copyExecuteWorkParam.getExecuteNodeParamList(), 
                turnDate, copyExecuteWorkParam.getWorkType());

        // 根据关联节点对不可运行的节点进行过滤
        enableExecuteNode = enableExecuteNode.stream().filter(x -> {
            if (CollUtil.isEmpty(x.getNodeRelationIdList())) {
                return true;
            }
            if (CollUtil.isEmpty(allRunNodeIdByTypeList)) {
                return false;
            }
            return new HashSet<>(allRunNodeIdByTypeList).containsAll(x.getNodeRelationIdList());
        }).collect(Collectors.toList());
        copyExecuteWorkParam.setExecuteNodeParamList(enableExecuteNode);
        return copyExecuteWorkParam;
    }


    /**
     * 得到所有运行结束的节点id
     *
     * @param jobWorkRunNodePos 作业运行节点
     * @param workType         作业类型
     */
    private List<String> getExecuteCompileteNodeIdByTypeList(List<ExecuteNodeParam> jobWorkRunNodePos,
                                                  Date turnDate, Integer workType) {
        if (workType == WorkTypeEnum.TYPE_TURN.getCode()) {
            return jobWorkRunNodePos.stream()
                    .filter(x -> {
                        boolean flag = x.getNodeRunStatus() == RunWorkStatusEnum.COMPLETE.getCode();
                        // 这里由于当作业类型为顺序类型时翻牌时间为空，不判断翻牌时间
                        if (flag && x.getTurnDate() != null) {
                            flag = DateUtil.compare(x.getTurnDate(), DateUtil.offset(turnDate, DateField.DAY_OF_MONTH, 1)) == 0;
                        }
                        return flag;
                    })
                    .map(ExecuteNodeParam::getNodeId)
                    .collect(Collectors.toList());
        } else if (workType == WorkTypeEnum.TYPE_SEQUENCE.getCode()) {
            return jobWorkRunNodePos.stream()
                    .filter(x -> x.getNodeRunStatus() == RunWorkStatusEnum.COMPLETE.getCode())
                    .map(ExecuteNodeParam::getNodeId)
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }


}
