package com.nbatch.job.admin.service.impl;

import cn.hutool.core.date.DateUtil;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.core.helper.RunWorkHelper;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.admin.service.IJobWorkNodeRunService;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.WorkTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JobWorkServiceImplRecoveryTest {

    private static final String WORK_ID = "work-1";

    @Mock
    private IJobWorkNodeMapper jobWorkNodeMapper;
    @Mock
    private IJobWorkMapper jobWorkMapper;
    @Mock
    private RunWorkHelper runWorkHelper;
    @Mock
    private IJobWorkNodeRunService jobWorkNodeRunService;

    private JobWorkRunServiceImpl service;

    @BeforeEach
    public void setUp() {
        service = new JobWorkRunServiceImpl();
        ReflectionTestUtils.setField(service, "jobWorkNodeMapper", jobWorkNodeMapper);
        ReflectionTestUtils.setField(service, "jobWorkMapper", jobWorkMapper);
        ReflectionTestUtils.setField(service, "runWorkHelper", runWorkHelper);
        ReflectionTestUtils.setField(service, "jobWorkNodeRunService", jobWorkNodeRunService);
    }

    @Test
    public void shouldRecoverOnlyAfterEveryOldAttemptIsStopped() {
        List<JobWorkNodePo> runNodeList = Arrays.asList(
                runNode("node-1"),
                runNode("node-2"));
        prepareRecovery(runNodeList);
        when(jobWorkNodeRunService.stopAndConfirmRunNodesExited(any(), any())).thenReturn(ReturnT.SUCCESS);
        when(runWorkHelper.recoverRunWorkSnapshot(any(), any())).thenReturn(ReturnT.SUCCESS);

        ReturnT<String> result = service.recoverRunWork(WORK_ID);

        assertTrue(result.isSuccess());
        verify(runWorkHelper).recoverRunWorkSnapshot(any(), any());
    }

    @Test
    public void shouldBlockWholeRecoveryWhileOneLongTaskIsStillExiting() {
        List<JobWorkNodePo> runNodeList = Arrays.asList(
                runNode("node-1"),
                runNode("node-2"));
        prepareRecovery(runNodeList);
        when(jobWorkNodeRunService.stopAndConfirmRunNodesExited(any(), any()))
                .thenReturn(ReturnT.error("长任务已收到停止请求但尚未退出"));

        ReturnT<String> result = service.recoverRunWork(WORK_ID);

        assertFalse(result.isSuccess());
        assertTrue(result.getMsg().contains("尚未退出"));
        verify(runWorkHelper, never()).recoverRunWorkSnapshot(any(), any());
    }

    @Test
    public void shouldBlockRecoveryWhenConsumerStateQueryFails() {
        prepareRecovery(Collections.singletonList(runNode("node-1")));
        when(jobWorkNodeRunService.stopAndConfirmRunNodesExited(any(), any()))
                .thenReturn(ReturnT.error(HandleCodeConstant.HANDLE_CODE_FAIL, "执行器状态查询失败"));

        ReturnT<String> result = service.recoverRunWork(WORK_ID);

        assertFalse(result.isSuccess());
        assertTrue(result.getMsg().contains("状态查询失败"));
        verify(runWorkHelper, never()).recoverRunWorkSnapshot(any(), any());
    }

    @Test
    public void shouldRequireNewStopBarrierWhenConsumerRestartsDuringRecovery() {
        prepareRecovery(Collections.singletonList(runNode("node-1")));
        when(jobWorkNodeRunService.stopAndConfirmRunNodesExited(any(), any()))
                .thenReturn(ReturnT.error("执行器未返回旧任务的停止确认，state:ABSENT"));

        ReturnT<String> result = service.recoverRunWork(WORK_ID);

        assertFalse(result.isSuccess());
        assertTrue(result.getMsg().contains("state:ABSENT"));
        verify(runWorkHelper, never()).recoverRunWorkSnapshot(any(), any());
    }

    @Test
    public void shouldRecoverStoppedNodesThatWereNeverDispatched() {
        prepareRecovery(Collections.singletonList(runNode("node-1")));
        when(jobWorkNodeRunService.stopAndConfirmRunNodesExited(any(), any())).thenReturn(ReturnT.SUCCESS);
        when(runWorkHelper.recoverRunWorkSnapshot(any(), any())).thenReturn(ReturnT.SUCCESS);

        ReturnT<String> result = service.recoverRunWork(WORK_ID);

        assertTrue(result.isSuccess());
        verify(runWorkHelper).recoverRunWorkSnapshot(any(), any());
    }

    @Test
    public void shouldRecoverUninitializedNode() {
        JobWorkPo work = new JobWorkPo()
                .setWorkId(WORK_ID)
                .setRunWorkStatus(FlowRunStatusEnum.STOPPED.getCode());
        JobWorkNodePo node = new JobWorkNodePo();
        node.setWorkId(WORK_ID);
        node.setNodeId("node-new");
        node.setNodeRunStatus(FlowRunStatusEnum.UNINITIALIZED.getCode());
        when(jobWorkMapper.selectById(WORK_ID)).thenReturn(work);
        when(jobWorkNodeMapper.selectList(any())).thenReturn(Collections.singletonList(node));
        when(jobWorkNodeRunService.stopAndConfirmRunNodesExited(any(), any())).thenReturn(ReturnT.SUCCESS);
        when(runWorkHelper.recoverRunWorkSnapshot(any(), any())).thenReturn(ReturnT.SUCCESS);

        ReturnT<String> result = service.recoverRunWork(WORK_ID);

        assertTrue(result.isSuccess());
        verify(runWorkHelper).recoverRunWorkSnapshot(any(), any());
    }

    @Test
    public void shouldConfirmStoppedNodesImmediatelyWhenStoppingWork() {
        JobWorkPo work = new JobWorkPo()
                .setWorkId(WORK_ID)
                .setRunWorkStatus(FlowRunStatusEnum.RUNNING.getCode())
                .setTurnDate(DateUtil.date());
        List<JobWorkNodePo> nodes = Collections.singletonList(runningNode("node-1"));
        when(jobWorkMapper.selectById(WORK_ID)).thenReturn(work);
        when(jobWorkNodeMapper.selectList(any())).thenReturn(nodes);
        when(runWorkHelper.stopRunWorkSnapshot(any(), any())).thenReturn(ReturnT.SUCCESS);
        when(jobWorkNodeRunService.stopAndConfirmRunNodesExited(any(), any())).thenReturn(ReturnT.SUCCESS);

        ReturnT<String> result = service.stopLatestRunWork(WORK_ID);

        assertTrue(result.isSuccess());
        assertTrue(result.getMsg().contains("已停止"));
        verify(jobWorkNodeRunService).stopAndConfirmRunNodesExited(any(), any());
    }

    @Test
    public void shouldKeepStoppingWhenExecutorConfirmsTaskIsStillExiting() {
        JobWorkPo work = new JobWorkPo()
                .setWorkId(WORK_ID)
                .setRunWorkStatus(FlowRunStatusEnum.RUNNING.getCode())
                .setTurnDate(DateUtil.date());
        List<JobWorkNodePo> nodes = Collections.singletonList(runningNode("node-1"));
        when(jobWorkMapper.selectById(WORK_ID)).thenReturn(work);
        when(jobWorkNodeMapper.selectList(any())).thenReturn(nodes);
        when(runWorkHelper.stopRunWorkSnapshot(any(), any())).thenReturn(ReturnT.SUCCESS);
        when(jobWorkNodeRunService.stopAndConfirmRunNodesExited(any(), any()))
                .thenReturn(ReturnT.error("长任务已收到停止请求但尚未退出"));

        ReturnT<String> result = service.stopLatestRunWork(WORK_ID);

        assertTrue(result.isSuccess());
        assertTrue(result.getMsg().contains("尚未退出"));
    }

    @Test
    public void shouldConfirmStoppingWorkAgainWithoutFreezingSnapshot() {
        JobWorkPo work = new JobWorkPo()
                .setWorkId(WORK_ID)
                .setRunWorkStatus(FlowRunStatusEnum.STOPPING.getCode())
                .setTurnDate(DateUtil.date());
        JobWorkNodePo stoppingNode = runningNode("node-1");
        stoppingNode.setNodeRunStatus(FlowRunStatusEnum.STOPPING.getCode());
        when(jobWorkMapper.selectById(WORK_ID)).thenReturn(work);
        when(jobWorkNodeMapper.selectList(any())).thenReturn(Collections.singletonList(stoppingNode));
        when(jobWorkNodeRunService.stopAndConfirmRunNodesExited(any(), any())).thenReturn(ReturnT.SUCCESS);

        ReturnT<String> result = service.stopLatestRunWork(WORK_ID);

        assertTrue(result.isSuccess());
        assertTrue(result.getMsg().contains("已停止"));
        verify(runWorkHelper, never()).stopRunWorkSnapshot(any(), any());
        verify(jobWorkNodeRunService).stopAndConfirmRunNodesExited(any(), any());
    }

    @Test
    public void shouldRejectTurnRerunDateAfterToday() {
        JobWorkPo work = new JobWorkPo()
                .setWorkId(WORK_ID)
                .setWorkType(WorkTypeEnum.TYPE_TURN.getCode())
                .setRunWorkStatus(FlowRunStatusEnum.COMPLETE.getCode())
                .setTurnDate(DateUtil.offsetDay(DateUtil.date(), 1));
        when(jobWorkMapper.selectById(WORK_ID)).thenReturn(work);

        ReturnT<String> result = service.rerunRunWork(WORK_ID,
                DateUtil.formatDate(DateUtil.offsetDay(DateUtil.date(), 1)));

        assertFalse(result.isSuccess());
        assertTrue(result.getMsg().contains("不能晚于今天"));
        verify(runWorkHelper, never()).resetRunWorkSnapshot(any(), any(), any());
    }

    private void prepareRecovery(List<JobWorkNodePo> runNodeList) {
        prepareRecovery(runNodeList, FlowRunStatusEnum.STOPPED);
    }

    private void prepareRecovery(List<JobWorkNodePo> runNodeList, FlowRunStatusEnum status) {
        JobWorkPo work = new JobWorkPo()
                .setWorkId(WORK_ID)
                .setRunWorkStatus(status.getCode());
        when(jobWorkMapper.selectById(WORK_ID)).thenReturn(work);
        List<JobWorkNodePo> nodeList = runNodeList.stream().map(runNode -> {
            JobWorkNodePo node = new JobWorkNodePo();
            node.setNodeId(runNode.getNodeId());
            node.setWorkId(WORK_ID);
            node.setNodeType("bean");
            node.setNodeRunStatus(status.getCode());
            return node;
        }).collect(java.util.stream.Collectors.toList());
        when(jobWorkNodeMapper.selectList(any())).thenReturn(nodeList);
    }

    private JobWorkNodePo runNode(String nodeId) {
        JobWorkNodePo runNodePo = new JobWorkNodePo();
        runNodePo.setNodeId(nodeId);
        runNodePo.setWorkId(WORK_ID);
        runNodePo.setNodeRunStatus(FlowRunStatusEnum.STOPPED.getCode());
        return runNodePo;
    }

    private JobWorkNodePo runningNode(String nodeId) {
        JobWorkNodePo runNodePo = runNode(nodeId);
        runNodePo.setNodeRunStatus(FlowRunStatusEnum.RUNNING.getCode());
        return runNodePo;
    }
}
