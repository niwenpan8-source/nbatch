package com.nbatch.job.handler.thread;

import cn.hutool.json.JSONObject;
import com.nbatch.job.core.biz.client.AdminBizClient;
import com.nbatch.job.core.biz.model.RegistryParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.enums.RegistryConfig;
import com.nbatch.job.core.files.RunNodeFilePath;
import com.nbatch.job.core.files.RunNodeLogEventLog;
import com.nbatch.job.core.util.GsonTool;
import com.nbatch.job.handler.utils.BatchThreadPoolUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertTrue;

/**
 * Measures executor registration while node tasks saturate CPU and create heap churn.
 */
public class ConsumerRegistrationPressureTest {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int WORK_COUNT = Integer.getInteger("nbatch.registryPressure.workCount",
            Math.max(8, Math.min(24, CPU_COUNT * 2)));
    private static final int MEMORY_PER_TASK_MB = Integer.getInteger("nbatch.registryPressure.memoryPerTaskMb", 2);
    private static final long PRESSURE_MILLIS = Long.getLong("nbatch.registryPressure.durationMillis",
            TimeUnit.SECONDS.toMillis(8));
    private static final long ADMIN_RESPONSE_DELAY_MILLIS = Long.getLong(
            "nbatch.registryPressure.adminResponseDelayMillis", 0L);
    private static final int CHURN_SLOT_COUNT = 4;
    private static final int CHURN_BLOCK_SIZE = 64 * 1024;
    private static final Date TURN_DATE = new Date(0L);
    private static final AtomicLong BLACK_HOLE = new AtomicLong();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private HttpServer adminServer;
    private ExecutorService adminServerExecutor;
    private final AtomicInteger receivedRegistrationCount = new AtomicInteger();

    @Before
    public void setUp() throws Exception {
        RunNodeFilePath.initDataPath(temporaryFolder.newFolder("event-data").getAbsolutePath());
        RunNodeLogEventLog.getInstance().start();
        startAdminServer();
    }

    @After
    public void tearDown() {
        BatchThreadPoolUtil.shutdownAllThreadPool();
        RunNodeLogEventLog.getInstance().toStop();
        if (adminServer != null) {
            adminServer.stop(0);
        }
        if (adminServerExecutor != null) {
            adminServerExecutor.shutdownNow();
        }
    }

    @Test
    public void shouldMeasureRegistrationUnderNodePressure() throws Exception {
        AdminBizClient adminClient = new AdminBizClient(
                "http://127.0.0.1:" + adminServer.getAddress().getPort(), null, 3);
        RegistryMetrics registryMetrics = new RegistryMetrics();
        AtomicBoolean stopRegistry = new AtomicBoolean();
        CountDownLatch firstRegistration = new CountDownLatch(1);
        Thread registryThread = new Thread(
                () -> runRegistryLoop(adminClient, registryMetrics, stopRegistry, firstRegistration),
                "test-consumer-registry-thread");
        registryThread.setDaemon(true);
        registryThread.start();
        assertTrue("initial registration did not finish", firstRegistration.await(5, TimeUnit.SECONDS));

        CountDownLatch tasksReady = new CountDownLatch(WORK_COUNT);
        CountDownLatch startPressure = new CountDownLatch(1);
        CountDownLatch tasksFinished = new CountDownLatch(WORK_COUNT);
        String keyPrefix = "registry-pressure-bean-" + System.nanoTime() + "-work-";
        long heapBefore = usedHeap();
        for (int i = 0; i < WORK_COUNT; i++) {
            BatchThreadPoolExecutor executor = BatchThreadPoolUtil.newThreadPoolExecutorDiscard(
                    keyPrefix + i, 1, 1, 30, TimeUnit.MINUTES, 1000);
            String nodeLogId = "registry-pressure-log-" + i;
            assertTrue(executor.executeBatch(newRunnable("registry-pressure-node-" + i, nodeLogId, () -> {
                runCpuAndHeapPressure(tasksReady, startPressure);
                tasksFinished.countDown();
            })));
        }

        assertTrue("pressure tasks did not start", tasksReady.await(10, TimeUnit.SECONDS));
        long heapWithRetainedTasks = usedHeap();
        AtomicBoolean stopHeapMonitor = new AtomicBoolean();
        AtomicLong peakHeap = new AtomicLong(heapWithRetainedTasks);
        Thread heapMonitor = startHeapMonitor(stopHeapMonitor, peakHeap);
        long pressureStartedAt = System.nanoTime();
        startPressure.countDown();
        assertTrue("pressure tasks did not finish", tasksFinished.await(
                PRESSURE_MILLIS + TimeUnit.SECONDS.toMillis(15), TimeUnit.MILLISECONDS));
        long pressureFinishedAt = System.nanoTime();
        stopHeapMonitor.set(true);
        heapMonitor.interrupt();
        heapMonitor.join(TimeUnit.SECONDS.toMillis(2));

        stopRegistry.set(true);
        registryThread.interrupt();
        registryThread.join(TimeUnit.SECONDS.toMillis(5));
        assertTrue("registry thread did not stop", !registryThread.isAlive());
        assertTrue("no registration was attempted", registryMetrics.attemptCount.get() > 0);
        assertTrue(receivedRegistrationCount.get() >= registryMetrics.successCount.get());

        long maxSuccessGapMillis = registryMetrics.maxSuccessGapMillis(
                pressureStartedAt, pressureFinishedAt);
        System.out.println("CONSUMER_REGISTRY_PRESSURE_METRICS"
                + " cpuCount=" + CPU_COUNT
                + " workPools=" + WORK_COUNT
                + " taskThreads=" + WORK_COUNT
                + " heartbeatThreads=" + WORK_COUNT
                + " retainedTaskMemoryMb=" + (WORK_COUNT * MEMORY_PER_TASK_MB)
                + " heapBeforeMb=" + toMb(heapBefore)
                + " heapWithRetainedTasksMb=" + toMb(heapWithRetainedTasks)
                + " peakHeapMb=" + toMb(peakHeap.get())
                + " adminResponseDelayMs=" + ADMIN_RESPONSE_DELAY_MILLIS
                + " registrationAttempts=" + registryMetrics.attemptCount.get()
                + " registrationSuccesses=" + registryMetrics.successCount.get()
                + " registrationFailures=" + registryMetrics.failureCount.get()
                + " adminReceivedRegistrations=" + receivedRegistrationCount.get()
                + " registrationMaxLatencyMs=" + registryMetrics.maxLatencyMillis()
                + " maxSuccessfulRegistrationGapMs=" + maxSuccessGapMillis
                + " adminDeadTimeoutMs=" + TimeUnit.SECONDS.toMillis(RegistryConfig.DEAD_TIMEOUT)
                + " wouldBeMarkedOffline="
                + (maxSuccessGapMillis >= TimeUnit.SECONDS.toMillis(RegistryConfig.DEAD_TIMEOUT)));
    }

    private Thread startHeapMonitor(AtomicBoolean stop, AtomicLong peakHeap) {
        Thread monitor = new Thread(() -> {
            while (!stop.get()) {
                peakHeap.accumulateAndGet(usedHeap(), Math::max);
                try {
                    TimeUnit.MILLISECONDS.sleep(20);
                } catch (InterruptedException e) {
                    if (stop.get()) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }, "test-consumer-heap-monitor");
        monitor.setDaemon(true);
        monitor.start();
        return monitor;
    }

    private void runRegistryLoop(AdminBizClient adminClient,
                                 RegistryMetrics metrics,
                                 AtomicBoolean stop,
                                 CountDownLatch firstRegistration) {
        RegistryParam registryParam = new RegistryParam(
                RegistryConfig.RegistType.EXECUTOR.name(), "pressure-consumer", "http://127.0.0.1:9999/");
        while (!stop.get()) {
            long startedAt = System.nanoTime();
            metrics.attemptCount.incrementAndGet();
            ReturnT<String> result = adminClient.registry(registryParam);
            long finishedAt = System.nanoTime();
            metrics.record(result, startedAt, finishedAt);
            firstRegistration.countDown();
            try {
                TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
            } catch (InterruptedException e) {
                if (stop.get()) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void runCpuAndHeapPressure(CountDownLatch ready, CountDownLatch start) {
        byte[] retainedMemory = new byte[MEMORY_PER_TASK_MB * 1024 * 1024];
        byte[][] churnRing = new byte[CHURN_SLOT_COUNT][];
        for (int i = 0; i < retainedMemory.length; i += 4096) {
            retainedMemory[i] = (byte) i;
        }
        ready.countDown();
        await(start);

        long value = 1;
        int iteration = 0;
        int churnIndex = 0;
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(PRESSURE_MILLIS);
        while (System.nanoTime() < deadline) {
            value ^= value << 13;
            value ^= value >>> 7;
            value ^= value << 17;
            retainedMemory[(int) (value & Integer.MAX_VALUE) % retainedMemory.length]++;
            if ((++iteration & 0x7FFF) == 0) {
                byte[] block = new byte[CHURN_BLOCK_SIZE];
                block[0] = (byte) value;
                churnRing[churnIndex++ % CHURN_SLOT_COUNT] = block;
            }
        }
        BLACK_HOLE.addAndGet(value + retainedMemory[0]
                + (churnRing[0] == null ? 0 : churnRing[0][0]));
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

    private void startAdminServer() throws IOException {
        adminServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 20);
        adminServerExecutor = Executors.newFixedThreadPool(2);
        adminServer.setExecutor(adminServerExecutor);
        adminServer.createContext("/api/registry", this::handleRegistry);
        adminServer.start();
    }

    private void handleRegistry(HttpExchange exchange) throws IOException {
        try {
            drain(exchange.getRequestBody());
            receivedRegistrationCount.incrementAndGet();
            sleepAdminResponseDelay();
            byte[] response = GsonTool.toJson(ReturnT.SUCCESS).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(response);
            }
        } finally {
            exchange.close();
        }
    }

    private void sleepAdminResponseDelay() {
        if (ADMIN_RESPONSE_DELAY_MILLIS <= 0) {
            return;
        }
        try {
            TimeUnit.MILLISECONDS.sleep(ADMIN_RESPONSE_DELAY_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void drain(InputStream input) throws IOException {
        byte[] buffer = new byte[1024];
        while (input.read(buffer) >= 0) {
            // Consume the request so HttpURLConnection can reuse normal response handling.
        }
        input.close();
    }

    private long usedHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private long toMb(long bytes) {
        return bytes / 1024 / 1024;
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

    private static final class RegistryMetrics {
        private final AtomicInteger attemptCount = new AtomicInteger();
        private final AtomicInteger successCount = new AtomicInteger();
        private final AtomicInteger failureCount = new AtomicInteger();
        private final AtomicLong maxLatencyNanos = new AtomicLong();
        private final List<Long> successTimes = Collections.synchronizedList(new ArrayList<>());

        private void record(ReturnT<String> result, long startedAt, long finishedAt) {
            updateMax(maxLatencyNanos, finishedAt - startedAt);
            if (result != null && result.isSuccess()) {
                successCount.incrementAndGet();
                successTimes.add(finishedAt);
            } else {
                failureCount.incrementAndGet();
            }
        }

        private long maxLatencyMillis() {
            return TimeUnit.NANOSECONDS.toMillis(maxLatencyNanos.get());
        }

        private long maxSuccessGapMillis(long pressureStartedAt, long pressureFinishedAt) {
            List<Long> snapshot;
            synchronized (successTimes) {
                snapshot = new ArrayList<>(successTimes);
            }
            long previous = pressureStartedAt;
            long maxGap = 0;
            for (Long successTime : snapshot) {
                if (successTime < pressureStartedAt) {
                    continue;
                }
                if (successTime > pressureFinishedAt) {
                    break;
                }
                maxGap = Math.max(maxGap, successTime - previous);
                previous = successTime;
            }
            maxGap = Math.max(maxGap, pressureFinishedAt - previous);
            return TimeUnit.NANOSECONDS.toMillis(maxGap);
        }

        private static void updateMax(AtomicLong target, long value) {
            long oldValue;
            do {
                oldValue = target.get();
                if (value <= oldValue) {
                    return;
                }
            } while (!target.compareAndSet(oldValue, value));
        }
    }
}
