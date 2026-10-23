package com.nbatch.job.core.thread;

import com.nbatch.job.core.biz.model.RunNodeLogEventParam;
import com.nbatch.job.core.biz.model.RunNodeLogPullResult;
import com.nbatch.job.core.files.RunNodeFilePath;
import com.nbatch.job.core.files.RunNodeLogEventLog;
import com.nbatch.job.core.util.GsonTool;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RunNodeLogEventLogTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldFlushEventsAndPersistAckInBackground() throws Exception {
        File dataDir = temporaryFolder.newFolder("event-data");
        RunNodeFilePath.initDataPath(dataDir.getAbsolutePath());
        RunNodeLogEventLog eventLog = RunNodeLogEventLog.getInstance();
        eventLog.start();
        try {
            for (int i = 0; i < 120; i++) {
                long offset = eventLog.append(new RunNodeLogEventParam()
                        .setNodeLogId("node-log-" + i)
                        .setHandleMsg(i == 0 ? "first line\nsecond line" : "event-" + i));
                assertEquals(i, offset);
            }

            RunNodeLogPullResult pullResult = awaitEvents(eventLog, 120);
            List<RunNodeLogPullResult.LogRecord> logList = pullResult.getLogList();
            assertEquals(120, logList.size());
            assertEquals("first line\nsecond line", GsonTool.fromJson(logList.get(0).getPayload(),
                    RunNodeLogEventParam.class).getHandleMsg());
            for (int i = 0; i < logList.size(); i++) {
                assertEquals(Long.valueOf(i), logList.get(i).getOffset());
            }

            eventLog.ack(119L);
            File ackFile = new File(new File(dataDir, "run-node-event"), "ack.offset");
            awaitAck(ackFile, "119");
        } finally {
            eventLog.toStop();
        }
    }

    @Test
    public void shouldOnlyAllowLeaseOwnerToPullAndAck() throws Exception {
        File dataDir = temporaryFolder.newFolder("lease-event-data");
        RunNodeFilePath.initDataPath(dataDir.getAbsolutePath());
        RunNodeLogEventLog eventLog = RunNodeLogEventLog.getInstance();
        eventLog.start();
        try {
            eventLog.append(new RunNodeLogEventParam().setNodeLogId("lease-node-log"));

            RunNodeLogPullResult ownerResult = awaitLease(eventLog, "admin-a");
            assertNotNull(ownerResult.getLeaseToken());
            assertNull(eventLog.tryPull("admin-b", 10));

            RunNodeLogPullResult retryResult = eventLog.tryPull("admin-a", 10);
            assertEquals(ownerResult.getLeaseToken(), retryResult.getLeaseToken());
            Long offset = ownerResult.getLogList().get(0).getOffset();
            assertFalse(eventLog.ack("admin-b", ownerResult.getLeaseToken(), offset));
            assertTrue(eventLog.ack("admin-a", ownerResult.getLeaseToken(), offset));
            assertTrue(eventLog.ack("admin-a", ownerResult.getLeaseToken(), offset));
        } finally {
            eventLog.toStop();
        }
    }

    private RunNodeLogPullResult awaitEvents(RunNodeLogEventLog eventLog, int expectedSize) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
        RunNodeLogPullResult result;
        do {
            result = eventLog.pull(expectedSize);
            if (result.getLogList().size() >= expectedSize) {
                return result;
            }
            TimeUnit.MILLISECONDS.sleep(20L);
        } while (System.currentTimeMillis() < deadline);
        assertEquals(expectedSize, result.getLogList().size());
        return result;
    }

    private RunNodeLogPullResult awaitLease(RunNodeLogEventLog eventLog, String consumerId) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
        RunNodeLogPullResult result;
        do {
            result = eventLog.tryPull(consumerId, 10);
            if (result != null && !result.getLogList().isEmpty()) {
                return result;
            }
            TimeUnit.MILLISECONDS.sleep(20L);
        } while (System.currentTimeMillis() < deadline);
        fail("run node event lease was not acquired");
        return null;
    }

    private void awaitAck(File ackFile, String expectedValue) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
        while (System.currentTimeMillis() < deadline) {
            if (ackFile.exists()) {
                String value = new String(Files.readAllBytes(ackFile.toPath()), StandardCharsets.UTF_8).trim();
                if (expectedValue.equals(value)) {
                    return;
                }
            }
            TimeUnit.MILLISECONDS.sleep(20L);
        }
        fail("ack offset was not persisted");
    }
}
