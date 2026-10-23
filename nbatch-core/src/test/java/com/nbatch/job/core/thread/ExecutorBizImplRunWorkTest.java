package com.nbatch.job.core.thread;

import com.nbatch.job.core.biz.impl.ExecutorBizImpl;
import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.biz.model.ReturnT;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertTrue;

public class ExecutorBizImplRunWorkTest {

    private final ExecutorBizImpl executorBiz = new ExecutorBizImpl();

    @Test
    public void rejectsMissingWorkPayload() {
        assertTrue(executorBiz.runWork(null).isFail());
    }

    @Test
    public void rejectsEmptyNodeList() {
        ExecuteWorkParam param = new ExecuteWorkParam()
                .setWorkId("work-1")
                .setExecuteNodeParamList(Collections.emptyList());

        assertTrue(executorBiz.runWork(param).isFail());
    }

    @Test
    public void rejectsMissingWorkIdentity() {
        ExecuteWorkParam param = new ExecuteWorkParam()
                .setExecuteNodeParamList(Collections.singletonList(new ExecuteNodeParam()));

        ReturnT<String> result = executorBiz.runWork(param);

        assertTrue(result.isFail());
    }
}
