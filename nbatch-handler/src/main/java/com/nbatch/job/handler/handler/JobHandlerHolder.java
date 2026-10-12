package com.nbatch.job.handler.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.json.JSONObject;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.biz.model.RunNodeLogEventParam;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.NodeTypeEnum;
import com.nbatch.job.core.handler.IJobHandlerHolder;
import com.nbatch.job.core.files.RunNodeLogEventLog;
import com.nbatch.job.handler.exception.HandlerException;
import com.nbatch.job.handler.thread.BatchThreadPoolExecutor;
import com.nbatch.job.handler.thread.RunNodeTask;
import com.nbatch.job.handler.utils.BatchThreadPoolUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.nbatch.job.core.enums.RunNodeLogEventTypeEnum.FAIL;
import static com.nbatch.job.handler.enums.ExceptionCodeEnum.SYSTEM_ERROR;

/**
 * 作业处理器容器
 *
 * @author Mr.ni
 * @date 2025/11/19
 */
@Slf4j
@RequiredArgsConstructor
public class JobHandlerHolder implements IJobHandlerHolder {

    /**
     * 线程池保持活跃时间
     */
    private static final int THREAD_POOL_KEEP_ALIVE = 30;

    /**
     * 线程池保持活跃时间单位
     */
    private static final TimeUnit THREAD_POOL_KEEP_ALIVE_UNIT = TimeUnit.MINUTES;

    /**
     * 线程池队列容量
     */
    private static final int THREAD_POOL_QUEUE_CAPACITY = 500;

    private final Map<String, JobNodeHandlerAdapter> jobHandlerAdapterMap;

    @Override
    public void handle(ExecuteWorkParam workNodeParam) {
        if (workNodeParam == null) {
            throw new HandlerException(SYSTEM_ERROR.getCode(), "execute work param is null");
        }
        List<ExecuteNodeParam> nodeParamList = workNodeParam.getExecuteNodeParamList();
        if (CollUtil.isEmpty(nodeParamList)) {
            throw new HandlerException(SYSTEM_ERROR.getCode(), "execute node params is empty");
        }
        if (BatchThreadPoolUtil.isShuttingDown()) {
            for (ExecuteNodeParam nodeParam : nodeParamList) {
                markNodeFailed(workNodeParam, nodeParam, "执行器正在关闭，拒绝新的运行节点");
            }
            return;
        }

        for (ExecuteNodeParam nodeParam : nodeParamList) {
            dispatchNode(workNodeParam, nodeParam);
        }
    }

    /**
     * 分发节点
     * @param workParam 工作参数
     * @param nodeParam 节点参数
     */
    private void dispatchNode(ExecuteWorkParam workParam, ExecuteNodeParam nodeParam) {
        if (nodeParam == null) {
            return;
        }
        try {
            // 得到节点类型
            NodeTypeEnum nodeType = NodeTypeEnum.getByCode(nodeParam.getNodeType());
            if (nodeType == null) {
                markNodeFailed(workParam, nodeParam, "unsupported node type:" + nodeParam.getNodeType());
                return;
            }
            JobNodeHandlerAdapter handlerAdapter = jobHandlerAdapterMap.get(nodeParam.getNodeType());
            if (handlerAdapter == null) {
                markNodeFailed(workParam, nodeParam, "node handler not found:" + nodeParam.getNodeType());
                return;
            }
            Integer threadPoolNum = nodeType.getThreadPoolNum();
            BatchThreadPoolExecutor executor = BatchThreadPoolUtil.newThreadPoolExecutorDiscard(
                    nodeParam.getNodeType() + StrPool.UNDERLINE + resolveWorkId(workParam, nodeParam),
                    threadPoolNum, threadPoolNum, THREAD_POOL_KEEP_ALIVE,
                    THREAD_POOL_KEEP_ALIVE_UNIT, THREAD_POOL_QUEUE_CAPACITY);
            if (!executor.executeBatch(buildRunNodeTask(workParam, nodeParam, handlerAdapter))) {
                markNodeFailed(workParam, nodeParam, "node submit thread pool failed");
            }
        } catch (Exception e) {
            markNodeFailed(workParam, nodeParam, "node submit error:" + e.getMessage());
        }
    }

    /**
     * 构建运行节点任务
     * @param workParam 工作参数
     * @param nodeParam 节点参数
     * @param handlerAdapter 处理器适配器
     * @return 运行节点任务
     */
    private RunNodeTask buildRunNodeTask(ExecuteWorkParam workParam,
                                         ExecuteNodeParam nodeParam,
                                         JobNodeHandlerAdapter handlerAdapter) {
        return new RunNodeTask(buildCacheObject(workParam, nodeParam),
                () -> {
                    try {
                        handlerAdapter.execute(nodeParam);
                    } catch (HandlerException e) {
                        log.error("workId:{}, node execute handler exception error:{}", workParam.getWorkId(),
                                ExceptionUtil.getRootCauseMessage(e));
                        throw e;
                    } catch (Exception e) {
                        log.error("workId:{}, node execute common exception error:{}", workParam.getWorkId(),
                                ExceptionUtil.getRootCauseMessage(e));
                        throw new HandlerException(SYSTEM_ERROR.getCode(), e);
                    }
                },
                () -> handlerAdapter.stop(nodeParam));
    }

    /**
     * 标记节点失败
     * @param workParam 工作参数
     * @param nodeParam 节点参数
     * @param message 失败信息
     */
    private void markNodeFailed(ExecuteWorkParam workParam, ExecuteNodeParam nodeParam, String message) {
        log.info("workId:{}, nodeId:{}, {}", workParam.getWorkId(), nodeParam.getNodeId(), message);
        RunNodeLogEventLog.getInstance().append(new RunNodeLogEventParam()
                .setEventType(FAIL.getValue())
                .setWorkId(resolveWorkId(workParam, nodeParam))
                .setNodeId(nodeParam.getNodeId())
                .setNodeLogId(nodeParam.getNodeLogId())
                .setTurnDate(nodeParam.getTurnDate())
                .setWorkType(workParam.getWorkType())
                .setHandleCode(HandleCodeConstant.HANDLE_CODE_FAIL)
                .setHandleMsg(message));
    }

    /**
     * 如果节点参数中没有workId，则使用工作参数中的workId
     * @param workParam 工作参数
     * @param nodeParam 节点参数
     * @return workId
     */
    private String resolveWorkId(ExecuteWorkParam workParam, ExecuteNodeParam nodeParam) {
        return nodeParam.getWorkId() == null ? workParam.getWorkId() : nodeParam.getWorkId();
    }

    private JSONObject buildCacheObject(ExecuteWorkParam workParam, ExecuteNodeParam nodeParam) {
        JSONObject cacheObj = new JSONObject();
        cacheObj.putOpt("nodeLogId", nodeParam.getNodeLogId());
        cacheObj.putOpt("workId", resolveWorkId(workParam, nodeParam));
        cacheObj.putOpt("nodeId", nodeParam.getNodeId());
        cacheObj.putOpt("turnDate", nodeParam.getTurnDate());
        cacheObj.putOpt("workType", workParam.getWorkType());
        return cacheObj;
    }

}
