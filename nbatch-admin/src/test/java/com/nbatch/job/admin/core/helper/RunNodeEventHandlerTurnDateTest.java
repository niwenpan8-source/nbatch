package com.nbatch.job.admin.core.helper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkPo;
import com.nbatch.job.admin.mapper.IJobWorkMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.core.biz.model.RunNodeLogEventParam;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import com.nbatch.job.core.enums.WorkTypeEnum;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证翻牌节点成功后的日期推进规则。
 */
@ExtendWith(MockitoExtension.class)
class RunNodeEventHandlerTurnDateTest {

    @Mock
    private IJobWorkNodeMapper jobWorkNodeMapper;
    @Mock
    private IJobWorkMapper jobWorkMapper;
    @Mock
    private RunNodeLogHelper runNodeLogHelper;
    @Mock
    private RunNodeStateHelper runNodeStateHelper;
    @Mock
    private RunNodeStatusReconciler runNodeStatusReconciler;

    private RunNodeEventHandler eventHandler;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        assistant.setCurrentNamespace("test.JobWorkNodePo");
        TableInfoHelper.initTableInfo(assistant, JobWorkNodePo.class);
    }

    @BeforeEach
    void setUp() {
        eventHandler = new RunNodeEventHandler(jobWorkNodeMapper, jobWorkMapper,
                runNodeLogHelper, runNodeStateHelper, runNodeStatusReconciler);
    }

    @Test
    void shouldAdvanceTurnNodeWhenNodeDateIsBeforeWorkDate() {
        JobWorkNodePo node = new JobWorkNodePo();
        node.setNodeId("node-1");
        node.setWorkId("work-1");
        node.setTurnDate(new Date(1767993600000L));
        node.setRetryTimes(2);
        node.setNodeRunStatus(FlowRunStatusEnum.RUNNING.getCode());
        JobWorkPo work = new JobWorkPo().setWorkId("work-1")
                .setWorkType(WorkTypeEnum.TYPE_TURN.getCode())
                .setTurnDate(new Date(1769126400000L));
        RunNodeLogEventParam event = new RunNodeLogEventParam().setWorkId("work-1")
                .setNodeId("node-1").setTurnDate(node.getTurnDate());
        when(jobWorkNodeMapper.selectById("node-1")).thenReturn(node);
        when(jobWorkMapper.selectById("work-1")).thenReturn(work);
        when(jobWorkNodeMapper.update(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(1);

        ReflectionTestUtils.invokeMethod(eventHandler, "markCompleted", event);

        verify(runNodeStateHelper).clearRetryState("node-1");
        verify(runNodeStateHelper).transitionNonTerminalWorkStatus("work-1", FlowRunStatusEnum.WAIT.getCode());
    }

    @Test
    void shouldCompleteTurnNodeWhenNodeDateHasReachedWorkDate() {
        Date turnDate = new Date(1769126400000L);
        JobWorkNodePo node = new JobWorkNodePo();
        node.setNodeId("node-1");
        node.setWorkId("work-1");
        node.setTurnDate(turnDate);
        node.setNodeRunStatus(FlowRunStatusEnum.RUNNING.getCode());
        JobWorkPo work = new JobWorkPo().setWorkId("work-1")
                .setWorkType(WorkTypeEnum.TYPE_TURN.getCode()).setTurnDate(turnDate);
        RunNodeLogEventParam event = new RunNodeLogEventParam().setWorkId("work-1")
                .setNodeId("node-1").setTurnDate(turnDate);
        when(jobWorkNodeMapper.selectById("node-1")).thenReturn(node);
        when(jobWorkMapper.selectById("work-1")).thenReturn(work);
        when(jobWorkNodeMapper.update(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(1);

        ReflectionTestUtils.invokeMethod(eventHandler, "markCompleted", event);

        verify(runNodeStateHelper).clearRetryState("node-1");
        verify(runNodeStateHelper, never()).transitionNonTerminalWorkStatus("work-1", FlowRunStatusEnum.WAIT.getCode());
    }
}
