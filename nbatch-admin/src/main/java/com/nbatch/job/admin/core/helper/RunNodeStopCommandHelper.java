package com.nbatch.job.admin.core.helper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogPo;
import com.nbatch.job.admin.core.executor.ExecutorBizProxy;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.StopRunNodeParam;
import com.nbatch.job.core.constant.HandleCodeConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 按执行器和线程池批量发送节点停止命令。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RunNodeStopCommandHelper {

    private final IJobWorkNodeMapper jobWorkNodeMapper;

    public ReturnT<String> send(List<JobWorkRunNodeLogPo> runNodeLogs) {
        if (CollUtil.isEmpty(runNodeLogs)) {
            return ReturnT.SUCCESS;
        }
        List<String> errors = new ArrayList<>();
        // 检查日志是否包含必需字段
        List<JobWorkRunNodeLogPo> validLogs = runNodeLogs.stream()
                .filter(this::hasRequiredFields)
                .collect(Collectors.toList());
        if (validLogs.size() != runNodeLogs.size()) {
            errors.add("存在缺少执行器、执行日志标识或节点信息的运行记录");
        }
        Map<String, List<JobWorkRunNodeLogPo>> addresses = validLogs.stream()
                .collect(Collectors.groupingBy(JobWorkRunNodeLogPo::getExecutorAddress));
        for (Map.Entry<String, List<JobWorkRunNodeLogPo>> addressEntry : addresses.entrySet()) {
            Map<String, List<JobWorkRunNodeLogPo>> pools = addressEntry.getValue().stream()
                    .collect(Collectors.groupingBy(this::resolveThreadPoolKey));
            for (Map.Entry<String, List<JobWorkRunNodeLogPo>> poolEntry : pools.entrySet()) {
                if (StrUtil.isBlank(poolEntry.getKey())) {
                    errors.add(addressEntry.getKey() + ":缺少节点类型");
                    continue;
                }
                try {
                    StopRunNodeParam param = new StopRunNodeParam().setThreadPoolKey(poolEntry.getKey())
                            .setNodeLogIdList(poolEntry.getValue().stream()
                                    .map(JobWorkRunNodeLogPo::getNodeLogId).collect(Collectors.toList()));
                    ReturnT<String> result = ExecutorBizProxy.stopRunNode(addressEntry.getKey(), param);
                    if (result.getCode() != HandleCodeConstant.HANDLE_CODE_SUCCESS) {
                        errors.add(addressEntry.getKey() + ":" + poolEntry.getKey());
                    }
                } catch (Exception e) {
                    log.warn("停止命令异常，执行器：{}，线程池键：{}", addressEntry.getKey(), poolEntry.getKey(), e);
                    errors.add(addressEntry.getKey() + ":" + poolEntry.getKey());
                }
            }
        }
        return errors.isEmpty() ? ReturnT.SUCCESS : ReturnT.error("停止失败的执行器：" + String.join(",", errors));
    }

    /**
     * 检查日志是否包含必需字段。
     */
    private boolean hasRequiredFields(JobWorkRunNodeLogPo logPo) {
        return logPo != null && StrUtil.isNotBlank(logPo.getExecutorAddress())
                && StrUtil.isNotBlank(logPo.getNodeLogId()) && StrUtil.isNotBlank(logPo.getWorkId())
                && StrUtil.isNotBlank(logPo.getNodeId());
    }

    /**
     * 解析线程池键。
     */
    private String resolveThreadPoolKey(JobWorkRunNodeLogPo logPo) {
        JobWorkNodePo node = jobWorkNodeMapper.selectById(logPo.getNodeId());
        return node == null || StrUtil.isBlank(node.getNodeType()) ? "" : node.getNodeType() + "_" + logPo.getWorkId();
    }
}
