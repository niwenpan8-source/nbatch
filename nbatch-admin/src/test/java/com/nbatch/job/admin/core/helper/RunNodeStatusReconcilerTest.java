package com.nbatch.job.admin.core.helper;

import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.admin.core.domain.po.JobWorkRunNodeLogPo;
import com.nbatch.job.admin.mapper.IJobRegistryMapper;
import com.nbatch.job.admin.mapper.IJobWorkNodeMapper;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证节点超时核对规则。
 */
@ExtendWith(MockitoExtension.class)
public class RunNodeStatusReconcilerTest {

    @Mock
    private IJobWorkNodeMapper jobWorkNodeMapper;

    @Mock
    private IJobRegistryMapper jobRegistryMapper;

    @Mock
    private RunNodeLogHelper runNodeLogHelper;

    @Mock
    private RunNodeStateHelper runNodeStateHelper;

    @Mock
    private RunNodeStopCommandHelper runNodeStopCommandHelper;

    private RunNodeStatusReconciler reconciler;

    @BeforeEach
    public void setUp() {
        reconciler = new RunNodeStatusReconciler(jobWorkNodeMapper, jobRegistryMapper,
                runNodeLogHelper, runNodeStateHelper, runNodeStopCommandHelper);
    }

    @Test
    public void shouldUseSameTimeoutForDispatchedAndRunningNodes() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 24, 10, 0);
        JobWorkRunNodeLogPo log = new JobWorkRunNodeLogPo();
        log.setLastActiveTime(now.minusMinutes(10));

        assertTrue(needsReconcile(FlowRunStatusEnum.DISPATCHED.getCode(), log, now));
        assertTrue(needsReconcile(FlowRunStatusEnum.RUNNING.getCode(), log, now));
    }

    @Test
    public void shouldNotReconcileBeforeUnifiedTimeout() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 24, 10, 0);
        JobWorkRunNodeLogPo log = new JobWorkRunNodeLogPo();
        log.setLastActiveTime(now.minusMinutes(9));

        assertFalse(needsReconcile(FlowRunStatusEnum.DISPATCHED.getCode(), log, now));
        assertFalse(needsReconcile(FlowRunStatusEnum.RUNNING.getCode(), log, now));
    }

    @Test
    public void shouldUseCreateTimeWhenLastActiveTimeMissing() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 24, 10, 0);
        JobWorkRunNodeLogPo log = new JobWorkRunNodeLogPo();
        log.setCreateTime(now.minusMinutes(10));

        assertTrue(needsReconcile(FlowRunStatusEnum.RUNNING.getCode(), log, now));
    }

    @Test
    public void shouldIgnoreInactiveNode() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 24, 10, 0);
        JobWorkRunNodeLogPo log = new JobWorkRunNodeLogPo();
        log.setLastActiveTime(now.minusMinutes(11));

        assertFalse(needsReconcile(FlowRunStatusEnum.COMPLETE.getCode(), log, now));
    }

    private boolean needsReconcile(Integer status, JobWorkRunNodeLogPo log, LocalDateTime now) {
        JobWorkNodePo node = new JobWorkNodePo();
        node.setNodeRunStatus(status);
        return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(reconciler, "needsReconcile", node, log, now));
    }
}
