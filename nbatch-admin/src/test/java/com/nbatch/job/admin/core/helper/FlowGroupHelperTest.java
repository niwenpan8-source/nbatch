package com.nbatch.job.admin.core.helper;

import com.nbatch.job.admin.core.domain.po.JobWorkNodePo;
import com.nbatch.job.core.enums.FlowRunStatusEnum;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlowGroupHelperTest {

    private final FlowGroupHelper helper = new FlowGroupHelper(null, null);

    @Test
    void shouldAggregateGroupStatusByDefinedPriority() {
        assertEquals(FlowRunStatusEnum.WAIT.getCode(), helper.resolveStatus(Collections.emptyList()));
        assertEquals(FlowRunStatusEnum.COMPLETE.getCode(), helper.resolveStatus(Arrays.asList(
                node(FlowRunStatusEnum.COMPLETE), node(FlowRunStatusEnum.SKIPPED))));
        assertEquals(FlowRunStatusEnum.RUNNING.getCode(), helper.resolveStatus(Arrays.asList(
                node(FlowRunStatusEnum.WAIT), node(FlowRunStatusEnum.RUNNING))));
        assertEquals(FlowRunStatusEnum.RUNNING.getCode(), helper.resolveStatus(Arrays.asList(
                node(FlowRunStatusEnum.DISPATCHED), node(FlowRunStatusEnum.RUNNING))));
        assertEquals(FlowRunStatusEnum.DISPATCHED.getCode(), helper.resolveStatus(Arrays.asList(
                node(FlowRunStatusEnum.WAIT), node(FlowRunStatusEnum.DISPATCHED))));
        assertEquals(FlowRunStatusEnum.STOPPED.getCode(), helper.resolveStatus(Arrays.asList(
                node(FlowRunStatusEnum.RUNNING), node(FlowRunStatusEnum.STOPPED))));
        assertEquals(FlowRunStatusEnum.STOPPING.getCode(), helper.resolveStatus(Arrays.asList(node(FlowRunStatusEnum.STOPPED), node(FlowRunStatusEnum.STOPPING))));
        assertEquals(FlowRunStatusEnum.EXCEPTION.getCode(), helper.resolveStatus(Arrays.asList(
                node(FlowRunStatusEnum.STOPPED), node(FlowRunStatusEnum.EXCEPTION))));
        assertEquals(FlowRunStatusEnum.UNINITIALIZED.getCode(), helper.resolveStatus(Collections.singletonList(
                node(FlowRunStatusEnum.UNINITIALIZED))));
        assertEquals(FlowRunStatusEnum.WAIT.getCode(), helper.resolveStatus(Arrays.asList(
                node(FlowRunStatusEnum.UNINITIALIZED), node(FlowRunStatusEnum.WAIT))));
        assertEquals(FlowRunStatusEnum.SKIPPED.getCode(), helper.resolveStatus(Collections.singletonList(
                node(FlowRunStatusEnum.SKIPPED))));
    }

    private JobWorkNodePo node(FlowRunStatusEnum status) {
        JobWorkNodePo node = new JobWorkNodePo();
        node.setNodeRunStatus(status.getCode());
        return node;
    }
}
