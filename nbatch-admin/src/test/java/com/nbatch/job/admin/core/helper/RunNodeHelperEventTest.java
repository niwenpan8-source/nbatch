package com.nbatch.job.admin.core.helper;

import com.nbatch.job.core.biz.model.RunNodeLogEventParam;
import com.nbatch.job.core.enums.RunNodeLogEventTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * 验证兼容入口仅转交给对应职责组件。
 */
@ExtendWith(MockitoExtension.class)
public class RunNodeHelperEventTest {

    @Mock
    private RunNodeDispatchHelper runNodeDispatchHelper;
    @Mock
    private RunNodeEventHandler runNodeEventHandler;
    @Mock
    private RunNodeStatusReconciler runNodeStatusReconciler;

    private RunNodeHelper runNodeHelper;

    @BeforeEach
    public void setUp() {
        runNodeHelper = new RunNodeHelper(runNodeDispatchHelper, runNodeEventHandler, runNodeStatusReconciler);
    }

    @Test
    public void shouldDelegateNodeEvent() {
        RunNodeLogEventParam event = new RunNodeLogEventParam().setNodeLogId("node-log-1")
                .setEventType(RunNodeLogEventTypeEnum.HEARTBEAT.getValue());

        runNodeHelper.handleRunNodeEvent(event);

        verify(runNodeEventHandler).handle(event);
    }

    @Test
    public void shouldDelegateTimeoutReconcile() {
        runNodeHelper.updateTimeOutRunNodeStatus();

        verify(runNodeStatusReconciler).reconcileTimeoutNodes();
    }
}
