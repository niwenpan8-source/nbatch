package com.nbatch.job.core.thread;

import com.nbatch.job.core.biz.model.RunNodeLogDetailParam;
import com.nbatch.job.core.biz.model.RunNodeLogPullResult;
import com.nbatch.job.core.files.RunNodeFilePath;
import com.nbatch.job.core.files.RunNodeLogDetailLog;
import com.nbatch.job.core.util.GsonTool;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RunNodeLogDetailLogTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldPullAndAckDetailLog() throws Exception {
        RunNodeFilePath.initDataPath(temporaryFolder.newFolder("detail-log-data").getAbsolutePath());
        RunNodeLogDetailLog detailLog = RunNodeLogDetailLog.getInstance();
        detailLog.start();
        try {
            long offset = detailLog.append(new RunNodeLogDetailParam()
                    .setDetailLogId("detail-1")
                    .setWorkId("work-1")
                    .setNodeId("node-1")
                    .setNodeLogId("node-log-1")
                    .setExecuteTime(LocalDateTime.now())
                    .setHandleMsg("详细日志"));
            assertTrue(offset >= 0);

            RunNodeLogPullResult result = awaitPull(detailLog, "admin-a");
            assertNotNull(result.getLeaseToken());
            assertEquals(1, result.getLogList().size());
            assertEquals("detail-1", GsonTool.fromJson(result.getLogList().get(0).getPayload(),
                    RunNodeLogDetailParam.class).getDetailLogId());
            assertTrue(detailLog.ack("admin-a", result.getLeaseToken(), result.getLogList().get(0).getOffset()));
        } finally {
            detailLog.toStop();
        }
    }

    private RunNodeLogPullResult awaitPull(RunNodeLogDetailLog detailLog, String consumerId) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
        RunNodeLogPullResult result;
        do {
            result = detailLog.tryPull(consumerId, 10);
            if (result != null && result.getLogList() != null && !result.getLogList().isEmpty()) {
                return result;
            }
            TimeUnit.MILLISECONDS.sleep(20);
        } while (System.currentTimeMillis() < deadline);
        throw new AssertionError("未拉取到详细日志");
    }
}
