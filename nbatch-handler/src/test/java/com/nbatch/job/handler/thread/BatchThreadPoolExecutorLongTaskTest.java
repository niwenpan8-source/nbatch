package com.nbatch.job.handler.thread;

import cn.hutool.json.JSONObject;
import com.nbatch.job.core.enums.RunNodeExecutionStateEnum;
import com.nbatch.job.core.files.RunNodeFilePath;
import com.nbatch.job.core.files.RunNodeLogEventLog;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BatchThreadPoolExecutorLongTaskTest {

    private static final Date TURN_DATE = new Date(0L);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final List<BatchThreadPoolExecutor> executorList = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        RunNodeFilePath.initDataPath(temporaryFolder.newFolder("event-data").getAbsolutePath());
        RunNodeLogEventLog.getInstance().start();
    }

    @After
    public void tearDown() throws Exception {
        for (BatchThreadPoolExecutor executor : executorList) {
            if (!executor.isShutdown()) {
                executor.shutdown();
            }
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
        RunNodeLogEventLog.getInstance().toStop();
    }

    @Test
    public void shouldRunMultipleLongWorksWithBoundedConcurrency() throws Exception {
        BatchThreadPoolExecutor executor = newExecutor(2, 20);
        CountDownLatch firstTwoStarted = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch allFinished = new CountDownLatch(6);
        List<String> nodeLogIds = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            String nodeLogId = "multi-log-" + i;
            nodeLogIds.add(nodeLogId);
            assertTrue(executor.executeBatch(newRunnable("multi-node-" + i, nodeLogId, () -> {
                firstTwoStarted.countDown();
                await(release);
                allFinished.countDown();
            })));
        }

        assertTrue(firstTwoStarted.await(2, TimeUnit.SECONDS));
        Map<String, String> stateMap = queryStates(executor, nodeLogIds);
        assertEquals(2, countState(stateMap, RunNodeExecutionStateEnum.RUNNING.getValue()));
        assertEquals(4, countState(stateMap, RunNodeExecutionStateEnum.QUEUED.getValue()));

        release.countDown();
        assertTrue(allFinished.await(5, TimeUnit.SECONDS));
        awaitAbsent(executor, nodeLogIds);
    }

    @Test
    public void shouldKeepSubmissionAndStateQueryResponsiveWhenWorkPoolIsSaturated() throws Exception {
        BatchThreadPoolExecutor executor = newExecutor(2, 200);
        CountDownLatch running = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch allFinished = new CountDownLatch(102);
        List<String> nodeLogIds = new ArrayList<>();
        ExecutorService caller = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> submitted = caller.submit(() -> {
                for (int i = 0; i < 102; i++) {
                    String nodeLogId = "slow-log-" + i;
                    nodeLogIds.add(nodeLogId);
                    if (!executor.executeBatch(newRunnable("slow-node-" + i, nodeLogId, () -> {
                        running.countDown();
                        await(release);
                        allFinished.countDown();
                    }))) {
                        return false;
                    }
                }
                return true;
            });
            assertTrue(submitted.get(2, TimeUnit.SECONDS));
            assertTrue(running.await(2, TimeUnit.SECONDS));

            Future<Map<String, String>> queried = caller.submit(() -> queryStates(executor, nodeLogIds));
            Map<String, String> stateMap = queried.get(1, TimeUnit.SECONDS);
            assertEquals(2, countState(stateMap, RunNodeExecutionStateEnum.RUNNING.getValue()));
            assertEquals(100, countState(stateMap, RunNodeExecutionStateEnum.QUEUED.getValue()));
        } finally {
            release.countDown();
            caller.shutdownNow();
        }
        assertTrue(allFinished.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void shouldReportOldAttemptAbsentAfterExecutorRestart() throws Exception {
        BatchThreadPoolExecutor oldExecutor = newExecutor(1, 10);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        String nodeLogId = "restart-old-log";
        assertTrue(oldExecutor.executeBatch(newRunnable("restart-node", nodeLogId, () -> {
            started.countDown();
            try {
                TimeUnit.MINUTES.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                finished.countDown();
            }
        })));
        assertTrue(started.await(2, TimeUnit.SECONDS));

        oldExecutor.shutdown();
        assertTrue(finished.await(2, TimeUnit.SECONDS));
        assertTrue(oldExecutor.awaitTermination(2, TimeUnit.SECONDS));

        BatchThreadPoolExecutor restartedExecutor = newExecutor(1, 10);
        Map<String, String> restartedStateMap = queryStates(restartedExecutor,
                Arrays.asList(nodeLogId));
        assertFalse(restartedStateMap.containsKey(nodeLogId));
    }

    @Test
    public void shouldNotRunRecoveredAttemptUntilInterruptIgnoringTaskActuallyExits() throws Exception {
        BatchThreadPoolExecutor executor = newExecutor(2, 10);
        AtomicBoolean releaseOldTask = new AtomicBoolean();
        CountDownLatch oldStarted = new CountDownLatch(1);
        CountDownLatch interruptObserved = new CountDownLatch(1);
        CountDownLatch oldFinished = new CountDownLatch(1);
        String oldNodeLogId = "recover-old-log";

        assertTrue(executor.executeBatch(newRunnable("recover-node", oldNodeLogId, () -> {
            oldStarted.countDown();
            while (!releaseOldTask.get()) {
                if (Thread.interrupted()) {
                    interruptObserved.countDown();
                }
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
            }
            oldFinished.countDown();
        })));
        assertTrue(oldStarted.await(2, TimeUnit.SECONDS));

        assertEquals(1, executor.stopRunNodes(Arrays.asList(oldNodeLogId), true));
        assertTrue(interruptObserved.await(2, TimeUnit.SECONDS));
        assertEquals(RunNodeExecutionStateEnum.STOP_REQUESTED.getValue(),
                queryStates(executor, Arrays.asList(oldNodeLogId)).get(oldNodeLogId));

        CountDownLatch rejectedAttemptStarted = new CountDownLatch(1);
        assertTrue(executor.executeBatch(newRunnable("recover-node", "recover-new-log", rejectedAttemptStarted::countDown)));
        assertFalse(rejectedAttemptStarted.await(300, TimeUnit.MILLISECONDS));
        assertFalse(queryStates(executor, Arrays.asList("recover-new-log")).containsKey("recover-new-log"));

        releaseOldTask.set(true);
        assertTrue(oldFinished.await(2, TimeUnit.SECONDS));
        awaitAbsent(executor, Arrays.asList(oldNodeLogId));

        CountDownLatch finalAttemptStarted = new CountDownLatch(1);
        assertTrue(executor.executeBatch(newRunnable("recover-node", "recover-final-log", finalAttemptStarted::countDown)));
        assertTrue(finalAttemptStarted.await(2, TimeUnit.SECONDS));
    }

    @Test
    public void shouldRejectDelayedRunRequestThatArrivesAfterStop() throws Exception {
        BatchThreadPoolExecutor executor = newExecutor(1, 10);
        String nodeLogId = "delayed-after-stop-log";
        CountDownLatch taskStarted = new CountDownLatch(1);
        try {
            RunNodeStopRegistry.requestStop(Arrays.asList(nodeLogId));

            assertTrue(executor.executeBatch(newRunnable("delayed-after-stop-node", nodeLogId,
                    taskStarted::countDown)));

            assertFalse(taskStarted.await(300, TimeUnit.MILLISECONDS));
            assertTrue(queryStates(executor, Arrays.asList(nodeLogId)).isEmpty());
        } finally {
            RunNodeStopRegistry.clearForTest(nodeLogId);
        }
    }

    private BatchThreadPoolExecutor newExecutor(int poolSize, int queueSize) {
        BatchThreadPoolExecutor executor = new BatchThreadPoolExecutor("test-" + executorList.size(),
                poolSize, poolSize, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(queueSize),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
        executorList.add(executor);
        return executor;
    }

    private RunNodeTask newRunnable(String nodeId, String nodeLogId, Runnable task) {
        JSONObject cache = new JSONObject();
        cache.putOpt("workId", "work-" + nodeId);
        cache.putOpt("workId", "run-work-" + nodeId);
        cache.putOpt("nodeId", "node-" + nodeId);
        cache.putOpt("nodeId", nodeId);
        cache.putOpt("nodeLogId", nodeLogId);
        cache.putOpt("turnDate", TURN_DATE);
        cache.putOpt("workType", 0);
        return new RunNodeTask(cache, task);
    }

    private Map<String, String> queryStates(BatchThreadPoolExecutor executor, List<String> nodeLogIds) {
        Map<String, String> stateMap = new HashMap<>();
        executor.collectRunNodeStates(nodeLogIds, stateMap);
        return stateMap;
    }

    private int countState(Map<String, String> stateMap, String state) {
        int count = 0;
        for (String value : stateMap.values()) {
            if (state.equals(value)) {
                count++;
            }
        }
        return count;
    }

    private void awaitAbsent(BatchThreadPoolExecutor executor, List<String> nodeLogIds) throws Exception {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
        while (System.currentTimeMillis() < deadline) {
            if (queryStates(executor, nodeLogIds).isEmpty()) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(10);
        }
        assertTrue("tasks did not leave the execution registry", queryStates(executor, nodeLogIds).isEmpty());
    }

    private static void await(CountDownLatch latch) {
        boolean interrupted = false;
        while (true) {
            try {
                latch.await();
                break;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
