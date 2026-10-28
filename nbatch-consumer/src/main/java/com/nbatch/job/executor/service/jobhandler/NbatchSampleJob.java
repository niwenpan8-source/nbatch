package com.nbatch.job.executor.service.jobhandler;

import com.nbatch.job.core.biz.model.ExecuteNodeParam;
import com.nbatch.job.core.handler.annotation.BatchJob;
import com.nbatch.job.handler.handler.BeanHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 批处理作业开发示例（Bean 模式）
 * <p>
 * 开发步骤：
 * 1、任务开发：在Spring Bean实例中，开发Job方法；
 * 2、注解配置：为Job方法添加注解 "@BatchJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 * 3、任务结果：默认任务结果为 "成功" 状态，不需要主动设置；如有诉求，比如设置任务结果为失败，可以通过 "NbatchJobHelper.handleFail/handleSuccess" 自主设置任务结果；
 */
@Slf4j
@Component
public class NbatchSampleJob {

    /** 每个 CPU 工作线程在单轮中分解的数字数量。 */
    private static final int NUMBERS_PER_WORKER = 40;

    /** 避免示例任务占满宿主机所有 CPU，保留业务线程和管理端响应能力。 */
    private static final int MAX_CPU_WORKERS = 4;

    /**
     * 试除法输入的素数位数上限。
     * 保持 CPU 计算特性，同时确保单轮计算能在 30 秒节奏内完成。
     */
    private static final int FACTOR_PRIME_BIT_LENGTH = 13;

    private Date localDate;

    /**
     * 1、简单任务示例（Bean模式）
     */
    @BatchJob("demoJobHandler")
    public void demoJobHandler() throws Exception {

        ExecuteNodeParam param = BeanHandlerContext.getBeanThreadLocal();

        // CPU 密集型任务：约 12 分钟。按受控工作线程数并行，做真实计算（质因数分解）。
        cpuIntensiveTask(param);
        // 默认执行成功
    }

    /**
     * CPU 密集型示例任务。
     * <p>
     * 通过受控大小的线程池并行执行质因数分解，避免示例任务耗尽宿主机 CPU。
     * 任务被切分为多个时间片轮流执行，每轮推送进度日志，
     * 并响应线程中断以实现优雅停止。
     *
     * @param param 执行节点参数，用于推送运行日志
     */
    private void cpuIntensiveTask(ExecuteNodeParam param) throws Exception {
        final int processors = resolveCpuWorkerCount();
        // 目标总时长约 12 分钟，每轮 30 秒，共 24 轮
        final long targetMillis = TimeUnit.MINUTES.toMillis(12);
        final long roundMillis = TimeUnit.SECONDS.toMillis(30);
        final int rounds = (int) (targetMillis / roundMillis);

        log.info("CPU 密集型任务启动：工作线程数={}, 预计轮数={}, 单轮={}ms", processors, rounds, roundMillis);
        param.pushRunNodeLogDetailCallback(
                String.format("CPU 密集型任务启动，工作线程数=%d，预计耗时约 %d 分钟", processors,
                        TimeUnit.MILLISECONDS.toMinutes(targetMillis)));

        final AtomicLong totalFactors = new AtomicLong(0);
        final AtomicLong processedNumbers = new AtomicLong(0);
        final AtomicBoolean stopped = new AtomicBoolean(false);

        for (int round = 1; round <= rounds; round++) {
            if (Thread.currentThread().isInterrupted()) {
                stopped.set(true);
                log.warn("任务被中断，提前退出（第 {} 轮）", round);
                param.pushRunNodeLogDetailCallback("CPU 密集型任务被中断，提前退出，已完成轮数=" + (round - 1));
                break;
            }

            long roundStart = System.nanoTime();
            long factors = runCpuRound(processors);
            totalFactors.addAndGet(factors);
            processedNumbers.addAndGet((long) processors * 200);

            double costSec = (System.nanoTime() - roundStart) / 1_000_000_000.0;
            log.info("第 {}/{} 轮完成，本轮分解出因子数={}，耗时={}s", round, rounds, factors, String.format("%.2f", costSec));
            param.pushRunNodeLogDetailCallback(
                    String.format("进度 %d/%d，累计处理数值=%d，累计因子=%d",
                            round, rounds, processedNumbers.get(), totalFactors.get()));

            // 控制节奏：本轮计算不足 roundMillis 时补足，保证整体时长贴近 12 分钟
            long sleepMillis = roundMillis - (long) (costSec * 1000);
            if (sleepMillis > 0) {
                TimeUnit.MILLISECONDS.sleep(sleepMillis);
            }
        }

        if (!stopped.get()) {
            log.info("CPU 密集型任务完成，累计处理数值={}，累计因子数={}",
                    processedNumbers.get(), totalFactors.get());
            param.pushRunNodeLogDetailCallback(
                    String.format("CPU 密集型任务完成，累计处理数值=%d，累计因子数=%d",
                            processedNumbers.get(), totalFactors.get()));
        }
    }

    /**
     * 执行一轮 CPU 计算：每个线程对一批大整数做质因数分解。
     *
     * @param threads 并行线程数
     * @return 本轮分解出的质因子总数
     */
    private long runCpuRound(int threads) throws Exception {
        // 使用受限位数的半素数，避免试除法单轮运行时间不可预测。
        List<BigInteger> batch = new ArrayList<>(threads * NUMBERS_PER_WORKER);
        for (int i = 0; i < threads * NUMBERS_PER_WORKER; i++) {
            BigInteger p = BigInteger.probablePrime(FACTOR_PRIME_BIT_LENGTH,
                    java.util.concurrent.ThreadLocalRandom.current());
            BigInteger q = BigInteger.probablePrime(FACTOR_PRIME_BIT_LENGTH,
                    java.util.concurrent.ThreadLocalRandom.current());
            batch.add(p.multiply(q));
        }

        java.util.concurrent.Phaser phaser = new java.util.concurrent.Phaser(threads);
        AtomicLong factorCount = new AtomicLong(0);

        List<Thread> workers = new ArrayList<>(threads);
        for (int t = 0; t < threads; t++) {
            final int startIdx = t;
            Thread worker = new Thread(() -> {
                try {
                    for (int i = startIdx; i < batch.size(); i += threads) {
                        factorCount.addAndGet(factorize(batch.get(i)).size());
                    }
                } finally {
                    phaser.arrive();
                }
            }, "cpu-worker-" + t);
            worker.setDaemon(true);
            workers.add(worker);
            worker.start();
        }
        phaser.awaitAdvanceInterruptibly(phaser.getPhase());
        return factorCount.get();
    }

    /**
     * 执行一轮不可中断的 CPU 计算。
     *
     * <p>该示例刻意不检查线程中断标记。执行器发出停止请求后，当前线程仍会等待
     * 本轮所有计算线程结束，避免主任务提前返回而子线程继续消耗 CPU。</p>
     */
    private long runCpuRoundIgnoringInterrupt(int threads) {
        List<BigInteger> batch = new ArrayList<>(threads * NUMBERS_PER_WORKER);
        for (int i = 0; i < threads * NUMBERS_PER_WORKER; i++) {
            BigInteger p = BigInteger.probablePrime(FACTOR_PRIME_BIT_LENGTH,
                    java.util.concurrent.ThreadLocalRandom.current());
            BigInteger q = BigInteger.probablePrime(FACTOR_PRIME_BIT_LENGTH,
                    java.util.concurrent.ThreadLocalRandom.current());
            batch.add(p.multiply(q));
        }

        java.util.concurrent.Phaser phaser = new java.util.concurrent.Phaser(threads);
        int phase = phaser.getPhase();
        AtomicLong factorCount = new AtomicLong(0);
        for (int threadIndex = 0; threadIndex < threads; threadIndex++) {
            final int startIndex = threadIndex;
            Thread worker = new Thread(() -> {
                try {
                    for (int i = startIndex; i < batch.size(); i += threads) {
                        factorCount.addAndGet(factorize(batch.get(i)).size());
                    }
                } finally {
                    phaser.arrive();
                }
            }, "cpu-uninterruptible-worker-" + threadIndex);
            worker.setDaemon(true);
            worker.start();
        }
        // awaitAdvance 不响应 interrupt，必须等待本轮 CPU 计算完整结束。
        phaser.awaitAdvance(phase);
        return factorCount.get();
    }

    /**
     * 试除法质因数分解（CPU 密集）。
     */
    private List<BigInteger> factorize(BigInteger n) {
        List<BigInteger> factors = new ArrayList<>();
        BigInteger d = BigInteger.valueOf(2);
        BigInteger limit = BigInteger.valueOf(2).pow(FACTOR_PRIME_BIT_LENGTH);
        while (n.compareTo(BigInteger.ONE) > 0 && d.compareTo(limit) <= 0) {
            while (n.mod(d).equals(BigInteger.ZERO)) {
                factors.add(d);
                n = n.divide(d);
            }
            d = d.equals(BigInteger.valueOf(2)) ? BigInteger.valueOf(3) : d.add(BigInteger.valueOf(2));
        }
        return factors;
    }

    /**
     * 不可中断的忙等：通过纯自旋消耗指定毫秒，不调用任何可被中断的方法，
     * 也不读取或清除中断标记，从而保证 CPU 密集型任务时长不被外部中断提前打断。
     *
     * @param millis 需要消耗的毫秒数（<=0 时立即返回）
     */
    private void busyWaitUninterruptibly(long millis) {
        if (millis <= 0) {
            return;
        }
        final long deadline = System.nanoTime() + millis * 1_000_000L;
        long value = blackhole;
        while (System.nanoTime() < deadline) {
            value = value * 31 + 7;
        }
        // 防止循环被 JIT 优化掉，同时避免每次自旋读写 volatile 字段。
        blackhole = value;
    }

    private int resolveCpuWorkerCount() {
        return Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), MAX_CPU_WORKERS));
    }

    @SuppressWarnings("unused")
    private volatile long blackhole = 0;

    public void init() {
        log.info("初始化处理器。");
    }

    public void destroy() {
        log.info("销毁处理器。");
    }


    /**
     * 不响应中断的 CPU 密集型任务示例（Bean 模式）。
     */
    @BatchJob("demoJobHandler2")
    public void demoJobHandler2() throws Exception {

        ExecuteNodeParam param = BeanHandlerContext.getBeanThreadLocal();

        // CPU 密集型任务：约 12 分钟。停止请求不会打断当前计算轮次或忙等时长。
        // 使用 runCpuRoundIgnoringInterrupt / factorize 做真实质因数分解，按受控线程数并行。
        final int processors = resolveCpuWorkerCount();
        // 目标总时长约 12 分钟，每轮 30 秒，共 24 轮
        final long targetMillis = TimeUnit.MINUTES.toMillis(12);
        final long roundMillis = TimeUnit.SECONDS.toMillis(30);
        final int rounds = (int) (targetMillis / roundMillis);

        log.info("CPU 密集型任务(demoJobHandler2)启动：工作线程数={}, 预计轮数={}, 单轮={}ms",
                processors, rounds, roundMillis);
        param.pushRunNodeLogDetailCallback(
                String.format("CPU 密集型任务(demoJobHandler2)启动，工作线程数=%d，预计耗时约 %d 分钟",
                        processors, TimeUnit.MILLISECONDS.toMinutes(targetMillis)));

        final AtomicLong totalFactors = new AtomicLong(0);
        final AtomicLong processedNumbers = new AtomicLong(0);

        for (int round = 1; round <= rounds; round++) {
            long roundStart = System.nanoTime();
            long factors = runCpuRoundIgnoringInterrupt(processors);
            totalFactors.addAndGet(factors);
            processedNumbers.addAndGet((long) processors * 200);

            double costSec = (System.nanoTime() - roundStart) / 1_000_000_000.0;
            log.info("第 {}/{} 轮完成，本轮分解出因子数={}，耗时={}s", round, rounds, factors, String.format("%.2f", costSec));
            param.pushRunNodeLogDetailCallback(
                    String.format("进度 %d/%d，累计处理数值=%d，累计因子=%d",
                            round, rounds, processedNumbers.get(), totalFactors.get()));

            // 控制节奏：本轮计算不足 roundMillis 时补足，保证整体时长贴近 12 分钟。
            // 不使用 sleep/wait 等可中断方法，改用不可中断的忙等（busy-spin）。
            long remainMillis = roundMillis - (long) (costSec * 1000);
            if (remainMillis > 0) {
                busyWaitUninterruptibly(remainMillis);
            }
        }

        log.info("CPU 密集型任务(demoJobHandler2)完成，累计处理数值={}，累计因子数={}",
                processedNumbers.get(), totalFactors.get());
        param.pushRunNodeLogDetailCallback(
                String.format("CPU 密集型任务(demoJobHandler2)完成，累计处理数值=%d，累计因子数=%d",
                        processedNumbers.get(), totalFactors.get()));
    }


    /**
     * 不响应中断的 CPU 密集型任务示例（Bean 模式）。
     */
    @BatchJob("demoJobHandler3")
    public void demoJobHandler3() throws Exception {
        ExecuteNodeParam param = BeanHandlerContext.getBeanThreadLocal();
        localDate = param.getTurnDate();
        for (int i = 0; i < 100; i++) {
            log.info("第 {}/{} 轮完成", i, 10);
            log.info("当前类对象：{}", localDate);
            param.pushRunNodeLogDetailCallback(
                    String.format("进度 %d/%d",
                            i, 10));
            Thread.sleep(1000);
        }
    }


}
