package com.nbatch.job.core.files;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import com.nbatch.job.core.biz.model.RunNodeLogPullResult;
import com.nbatch.job.core.util.GsonTool;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 运行节点本地可靠日志存储。
 * 节点状态事件和详细日志通过不同实例使用同一套分段、租约和 ACK 机制。
 */
@Slf4j
public class LocalPullLogStore {

    private static final String EVENT_FILE_PREFIX = "event-slot-";
    private static final String EVENT_FILE_SUFFIX = ".log";
    private static final String ACK_FILE_NAME = "ack.offset";

    /** 本地日志通道目录，同一存储实现可复用到不同日志类型。 */
    private final String eventDirName;

    private static final int DEFAULT_PULL_SIZE = 100;
    private static final int MAX_PULL_SIZE = 1000;

    /**
     * 每个段文件最多保存的事件数量。
     */
    private static final int SEGMENT_EVENT_COUNT = 5000;

    /**
     * 固定槽文件数量，超过后按 slot 复用，避免文件名无限增长。
     */
    private static final int SEGMENT_FILE_COUNT = 20;
    private static final int MAX_CLEAN_FILES_PER_ROUND = 5;

    /**
     * 满足数量或时间任一条件就刷盘。
     */
    private static final int FLUSH_EVENT_COUNT = 10;
    private static final long FLUSH_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(3);

    /**
     * ACK 刷盘间隔
     */
    private static final long ACK_FLUSH_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(3);

    /**
     * 写入失败后一秒重试。
     */
    private static final long WRITE_RETRY_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(1);

    /**
     * 拉取租约超时时间。
     */
    private static final long PULL_LEASE_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(30);

    /**
     * 当前批次最多暂存的日志数量，避免后台刷盘变慢时单个 List 无限增长。
     */
    private static final int MAX_BUFFERED_LOG_RECORDS = 500;

    /**
     * 待刷批次数量上限，磁盘变慢时通过队列反压。
     */
    private static final int MAX_PENDING_FLUSH_BATCHES = 100;

    /**
     * 保护 offset、内存日志列表和待刷队列等内存状态。
     */
    private final Object lock = new Object();

    /** 串行段文件读写和 ACK 持久化。 */
    private final Object fileLock = new Object();

    /** 调用线程写入内存列表，不直接执行磁盘 IO。 */
    private final List<RunNodeLogPullResult.LogRecord> bufferedLogRecordList = new ArrayList<>();

    /**
     * 包装好的FlushBatch会首先进入队列，然后由后台刷盘线程执行
     */
    private final Deque<FlushBatch> pendingFlushBatchQueue = new ArrayDeque<>();

    private Thread flushThread;

    /**
     * 停止标记，停止后不再接受新事件。
     */
    private volatile boolean toStop;

    /**
     * 下一条事件 offset，以及管理端已经确认处理到的 offset。
     */
    private volatile long nextOffset;
    private volatile long ackOffset;

    /**
     * 同一时间只允许一个管理端拉取日志-租约
     */
    private PullLease pullLease;
    private AckLease lastAckLease;

    /**
     * 当前内存日志列表所属段的起始 offset。
     */
    private volatile long currentSegmentBaseOffset;

    /** 上一次刷盘时间和清理时间。 */
    private long lastFlushTime;

    /** 上一次清理时间。 */
    private long lastCleanTime;

    /** ACK 合并持久化请求，降低高频 ACK 的文件写入次数。 */
    private boolean ackFlushRequested;

    /** ACK 合并持久化请求时间。 */
    private long ackFlushRequestTime;

    public LocalPullLogStore(String eventDirName) {
        this.eventDirName = eventDirName;
    }

    public void start() {
        synchronized (lock) {
            toStop = false;
            bufferedLogRecordList.clear();
            pendingFlushBatchQueue.clear();
            synchronized (fileLock) {
                FileUtil.mkdir(getEventDir());
                // 已确认 offset 单独保存，重启后可以从下一个未确认事件继续拉取。
                ackOffset = readLong(getAckFile(), -1L);
                // 扫描现有段文件的最大 offset，保证新事件 offset 继续递增。
                nextOffset = Math.max(readMaxOffset() + 1, ackOffset + 1);
            }
            if (nextOffset < 0) {
                nextOffset = 0;
            }
            currentSegmentBaseOffset = resolveSegmentBaseOffset(nextOffset);
            lastFlushTime = System.currentTimeMillis();
            lastCleanTime = lastFlushTime;
            ackFlushRequested = false;
            pullLease = null;
            lastAckLease = null;
        }
        startFlushThread();
    }

    public void toStop() {
        toStop = true;
        synchronized (lock) {
            lock.notifyAll();
        }
        if (flushThread != null) {
            try {
                // 主线程启动子线程去执行耗时任务（如查询数据库），必须等待子线程获取到数据后，主线程才能进行后续的数据汇总处理。
                flushThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 追加业务序列化后的日志载荷。
     */
    public long append(String payload) {
        if (payload == null) {
            return -1L;
        }
        synchronized (lock) {
            if (toStop) {
                return -1L;
            }
            long offset = nextOffset;
            // 包装台日志对象
            RunNodeLogPullResult.LogRecord logRecord = new RunNodeLogPullResult.LogRecord()
                    .setOffset(offset).setTimestamp(System.currentTimeMillis()).setPayload(payload);
            long segmentBaseOffset = resolveSegmentBaseOffset(offset);
            // 如果两个文件其实offset不一样，说明跨段了
            if (segmentBaseOffset != currentSegmentBaseOffset) {
                // 跨段时先把旧段交给后台写线程，批次顺序就是文件写入顺序。
                if (!enqueueLogRecordBufferLocked()) {
                    return -1L;
                }
                currentSegmentBaseOffset = segmentBaseOffset;
            }

            // 如果内存日志列表 > 大于列表最大数量，则将内存日志列表写入到批次当中，如果批次也没有空间了返回-1
            if (bufferedLogRecordList.size() >= MAX_BUFFERED_LOG_RECORDS
                    && !enqueueLogRecordBufferLocked()) {
                return -1L;
            }
            bufferedLogRecordList.add(logRecord);
            if (bufferedLogRecordList.size() >= FLUSH_EVENT_COUNT) {
                lock.notifyAll();
            }
            nextOffset++;
            return offset;
        }
    }

    /**
     * 从未确认位置读取已落盘记录；尚在内存中的记录由下一轮拉取获取。
     */
    public RunNodeLogPullResult pull(Integer maxSize) {
        long ackSnapshot;
        long startOffset;
        synchronized (lock) {
            ackSnapshot = ackOffset;
            startOffset = ackSnapshot + 1;
        }

        int limit = maxSize == null || maxSize <= 0 ? DEFAULT_PULL_SIZE : Math.min(maxSize, MAX_PULL_SIZE);
        List<RunNodeLogPullResult.LogRecord> logList;
        synchronized (fileLock) {
            // 拉取操作只读取已写入批次，不再替代后台线程同步写文件。
            logList = readRecords(startOffset, limit);
        }
        long nextPullOffset = logList.isEmpty() ? startOffset : logList.get(logList.size() - 1).getOffset() + 1;
        return new RunNodeLogPullResult()
                .setAckOffset(ackSnapshot)
                .setNextOffset(nextPullOffset)
                .setLogList(logList);
    }

    /**
     * 获取当前事件批次的独占拉取租约。
     * 同一管理端的重试会返回原批次，其他管理端在租约有效期内快速失败。
     */
    public RunNodeLogPullResult tryPull(String consumerId, Integer maxSize) {
        synchronized (lock) {
            long now = System.currentTimeMillis();
            clearExpiredPullLease(now);
            if (pullLease != null) {
                if (!pullLease.consumerId.equals(consumerId)) {
                    return null;
                }
                pullLease.refresh(now);
                return pullLease.pullResult;
            }

            RunNodeLogPullResult result = pull(maxSize);
            if (CollUtil.isEmpty(result.getLogList())) {
                return result;
            }
            String leaseToken = UUID.randomUUID().toString();
            result.setLeaseToken(leaseToken);
            pullLease = new PullLease(consumerId, leaseToken, result, now + PULL_LEASE_TIMEOUT_MILLIS);
            return result;
        }
    }

    /**
     * 推进内存 ACK，后台线程合并持久化后才能据此删除旧段。
     */
    public void ack(Long offset) {
        if (offset == null) {
            return;
        }
        synchronized (lock) {
            long maxAckOffset = nextOffset - 1;
            if (offset <= ackOffset || offset > maxAckOffset) {
                if (offset > maxAckOffset) {
                    log.warn("忽略无效的运行节点事件确认，offset：{}，最大确认 offset：{}", offset, maxAckOffset);
                }
                return;
            }
            ackOffset = offset;
            if (!ackFlushRequested) {
                ackFlushRequested = true;
                ackFlushRequestTime = System.currentTimeMillis();
            }
            lock.notifyAll();
        }
    }

    /**
     * 仅当前租约持有者可以确认事件。重复 ACK 返回成功，避免响应丢失后重复处理。
     */
    public boolean ack(String consumerId, String leaseToken, Long offset) {
        synchronized (lock) {
            // 确认当中时间是不是拉取日志的那个管理端
            if (lastAckLease != null && lastAckLease.matches(consumerId, leaseToken, offset)) {
                return true;
            }
            clearExpiredPullLease(System.currentTimeMillis());
            if (pullLease == null || !pullLease.matches(consumerId, leaseToken)
                    || !pullLease.containsOffset(offset)) {
                return false;
            }
            ack(offset);
            lastAckLease = new AckLease(consumerId, leaseToken, offset);
            pullLease = null;
            return true;
        }
    }

    private void clearExpiredPullLease(long now) {
        if (pullLease != null && pullLease.expiresAtMillis <= now) {
            pullLease = null;
        }
    }

    /**
     * 后台刷盘线程，避免每追加一条事件都强制 sync。
     */
    private void startFlushThread() {
        flushThread = new Thread(this::runFlushLoop);
        flushThread.setDaemon(true);
        flushThread.setName("job, run node event flush thread");
        flushThread.start();
    }

    /**
     * 持续执行事件批次 和 ACK 的后台刷盘循环。
     */
    private void runFlushLoop() {
        try {
            while (true) {
                try {
                    if (!flushOnce()) {
                        return;
                    }
                } catch (InterruptedException e) {
                    throw e;
                } catch (Throwable e) {
                    log.error("运行节点事件刷新操作发生错误。", e);
                    TimeUnit.MILLISECONDS.sleep(WRITE_RETRY_INTERVAL_MILLIS);
                }
            }
        } catch (InterruptedException e) {
            if (!toStop) {
                log.warn("运行节点事件刷新线程被中断。", e);
            }
            Thread.currentThread().interrupt();
        } catch (Throwable e) {
            log.error("运行节点事件刷新线程发生错误。", e);
        } finally {
            log.info(">>>>>>>>>>> 作业运行节点事件刷新线程已销毁。");
        }
    }

    /**
     * 每轮最多写入一个日志批次和一个到期 ACK 快照。
     */
    private boolean flushOnce() throws InterruptedException {
        FlushBatch batch = null;
        Long ackSnapshot = null;
        synchronized (lock) {
            while (true) {
                long now = System.currentTimeMillis();

                // 停止且没有添加数据以及没有处理批次
                if (toStop && bufferedLogRecordList.isEmpty() && pendingFlushBatchQueue.isEmpty()) {
                    return false;
                }

                // 如果满足list 写入批次条件同时批次还有空间，进行写入
                if (shouldEnqueueLogRecordBufferLocked(now)
                        && pendingFlushBatchQueue.size() < MAX_PENDING_FLUSH_BATCHES) {
                    enqueueLogRecordBufferLocked();
                }

                // 优先写入已经封装完成的日志批次。
                FlushBatch firstBatch = pendingFlushBatchQueue.peekFirst();
                if (firstBatch != null) {
                    batch = pendingFlushBatchQueue.pollFirst();
                    lock.notifyAll();
                }

                // 日志持续写入时也要按周期持久化 ACK，避免 ACK 被日志批次长期饿死。
                if (ackFlushRequested
                        && (toStop || now - ackFlushRequestTime >= ACK_FLUSH_INTERVAL_MILLIS)) {
                    ackSnapshot = ackOffset;
                }
                if (batch != null || ackSnapshot != null) {
                    break;
                }

                // 暂无就绪任务，等待下一个触发时机
                lock.wait(resolveFlushWaitMillisLocked(now));
            }
        }

        if (batch != null && !writeFlushBatch(batch, toStop)) {
            requeueFailedBatch(batch);
        }
        if (ackSnapshot != null && !persistAckOffset(ackSnapshot)) {
            TimeUnit.MILLISECONDS.sleep(WRITE_RETRY_INTERVAL_MILLIS);
        }
        return true;
    }

    /** 判断数量、时间或停止条件是否已经触发批次入队。 */
    private boolean shouldEnqueueLogRecordBufferLocked(long now) {
        if (bufferedLogRecordList.isEmpty()) {
            return false;
        }
        // 停止或缓冲数量或时间条件满足，则入队
        return toStop
                || bufferedLogRecordList.size() >= FLUSH_EVENT_COUNT
                || now - lastFlushTime >= FLUSH_INTERVAL_MILLIS;
    }

    /** 计算后台线程距离下一次刷盘条件满足还需等待的时间。 */
    private long resolveFlushWaitMillisLocked(long now) {
        if (toStop) {
            return 1L;
        }
        long waitMillis = FLUSH_INTERVAL_MILLIS;
        if (!bufferedLogRecordList.isEmpty()) {
            if (bufferedLogRecordList.size() >= FLUSH_EVENT_COUNT) {
                return 1L;
            }
            waitMillis = Math.min(waitMillis,
                    Math.max(FLUSH_INTERVAL_MILLIS - (now - lastFlushTime), 1L));
        }
        if (ackFlushRequested) {
            waitMillis = Math.min(waitMillis,
                    Math.max(ACK_FLUSH_INTERVAL_MILLIS - (now - ackFlushRequestTime), 1L));
        }
        return Math.max(waitMillis, 1L);
    }

    /**
     * 把内存日志列表写入批次缓存当中
     */
    private boolean enqueueLogRecordBufferLocked() {
        if (bufferedLogRecordList.isEmpty()) {
            return true;
        }
        while (pendingFlushBatchQueue.size() >= MAX_PENDING_FLUSH_BATCHES && !toStop) {
            try {
                // 唤醒使用对象锁住的所有线程
                lock.notifyAll();
                lock.wait(WRITE_RETRY_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        // 如果已经停止同时pendingFlushBatchQueue中的内容大于 可以添加最大批次数
        if (toStop && pendingFlushBatchQueue.size() >= MAX_PENDING_FLUSH_BATCHES) {
            return false;
        }

        // 等待队列可用期间会释放 lock，因此必须在等待结束后再复制并清空当前列表。
        // 这样等待期间新增的日志会随本次快照入队，不会被后续 clear 丢弃。
        FlushBatch batch = new FlushBatch(currentSegmentBaseOffset, new ArrayList<>(bufferedLogRecordList));
        pendingFlushBatchQueue.addLast(batch);
        bufferedLogRecordList.clear();
        lastFlushTime = System.currentTimeMillis();
        lock.notifyAll();
        return true;
    }

    /**
     * 写入失败的批次放回队首，保持 offset 的落盘顺序。
     */
    private void requeueFailedBatch(FlushBatch batch) throws InterruptedException {
        synchronized (lock) {
            pendingFlushBatchQueue.addFirst(batch);
        }
        TimeUnit.MILLISECONDS.sleep(WRITE_RETRY_INTERVAL_MILLIS);
    }

    /**
     * 将不可变批次编码后追加到它所属的段文件。
     */
    private boolean writeFlushBatch(FlushBatch batch, boolean force) {
        synchronized (fileLock) {
            if (!prepareSegmentFileLocked(batch.segmentBaseOffset)) {
                return false;
            }
            File file = getSegmentFile(batch.segmentBaseOffset);
            FileUtil.touch(file);
            try (FileOutputStream outputStream = new FileOutputStream(file, true);
                 FileChannel channel = outputStream.getChannel();
                 BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
                for (RunNodeLogPullResult.LogRecord logRecord : batch.logRecordList) {
                    bufferedOutputStream.write(encodeRecordLine(logRecord));
                }
                bufferedOutputStream.flush();
                if (force) {
                    channel.force(false);
                }
                return true;
            } catch (Exception e) {
                log.error("写入运行节点本地日志分段失败，文件：{}", file.getAbsolutePath(), e);
                return false;
            }
        }
    }

    /**
     * 先持久化 ACK，再按该快照清理已完全确认的段文件。
     */
    private boolean persistAckOffset(long ackSnapshot) {
        boolean persisted;
        synchronized (fileLock) {
            persisted = writeAckOffset(ackSnapshot);
            if (persisted && System.currentTimeMillis() - lastCleanTime >= FLUSH_INTERVAL_MILLIS) {
                // 只按已经持久化的 ACK 清理，重启后不会引用已删除的段。
                cleanSegmentFilesLocked(ackSnapshot);
                lastCleanTime = System.currentTimeMillis();
            }
        }
        synchronized (lock) {
            if (persisted) {
                ackFlushRequested = ackOffset > ackSnapshot;
            }
            if (ackFlushRequested) {
                ackFlushRequestTime = System.currentTimeMillis();
            }
        }
        return persisted;
    }

    private boolean prepareSegmentFileLocked(long segmentBaseOffset) {
        File segmentFile = getSegmentFile(segmentBaseOffset);
        if (!segmentFile.exists() || segmentFile.length() <= 0) {
            return true;
        }
        // 槽位文件复用时，先确认其中是否为当前分段的数据。
        long fileSegmentBaseOffset = readSegmentBaseOffset(segmentFile);
        if (fileSegmentBaseOffset == segmentBaseOffset) {
            return true;
        }
        long lastOffset = readLastOffset(segmentFile, -1L);
        if (lastOffset > ackOffset) {
            // 未确认数据绝不能被循环槽覆盖，写线程会重试并对追加方施加背压。
            log.error("运行节点本地日志容量已满，拒绝覆盖未确认分段，文件：{}，文件分段起始 offset：{}，目标分段起始 offset：{}，最后 offset：{}，确认 offset：{}",
                    segmentFile.getAbsolutePath(), fileSegmentBaseOffset, segmentBaseOffset, lastOffset, ackOffset);
            return false;
        }
        FileUtil.del(segmentFile);
        FileUtil.touch(segmentFile);
        return true;
    }

    /**
     * 读取指定偏移量范围的日志事件。
     * @param startOffset 起始偏移量
     * @param limit 限制数量
     * @return 日志事件列表
     */
    private List<RunNodeLogPullResult.LogRecord> readRecords(long startOffset, int limit) {
        List<RunNodeLogPullResult.LogRecord> logList = new ArrayList<>();
        for (File segmentFile : getSortedSegmentFiles()) {
            // 读取分段起始偏移量
            long segmentBaseOffset = readSegmentBaseOffset(segmentFile);
            // 不满足的文件直接跳过
            if (segmentBaseOffset == Long.MAX_VALUE
                    || segmentBaseOffset + SEGMENT_EVENT_COUNT - 1 < startOffset) {
                // 整个段都小于拉取起点，直接跳过。
                continue;
            }
            // 读取满limit或者读取到最后一条记录后返回
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    Files.newInputStream(segmentFile.toPath()), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    RunNodeLogPullResult.LogRecord logRecord = decodeRecordLine(line);
                    if (logRecord != null && logRecord.getOffset() != null && logRecord.getOffset() >= startOffset) {
                        logList.add(logRecord);
                        if (logList.size() >= limit) {
                            return logList;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("拉取运行节点本地日志分段失败，文件：{}", segmentFile.getAbsolutePath(), e);
            }
        }
        return logList;
    }

    /**
     * 每轮限量删除已经完全 ACK 且不再写入的旧段。
     */
    private void cleanSegmentFilesLocked(long persistedAckOffset) {
        List<File> segmentFiles = getSortedSegmentFiles();
        int deletedCount = 0;
        for (File segmentFile : segmentFiles) {
            if (deletedCount >= MAX_CLEAN_FILES_PER_ROUND) {
                break;
            }
            long segmentBaseOffset = readSegmentBaseOffset(segmentFile);
            if (segmentBaseOffset == Long.MAX_VALUE || segmentBaseOffset == currentSegmentBaseOffset) {
                // 当前段可能还有未刷入的新事件，不能清理。
                continue;
            }
            if (segmentBaseOffset + SEGMENT_EVENT_COUNT - 1 <= persistedAckOffset && FileUtil.del(segmentFile)) {
                deletedCount++;
            }
        }
    }

    /**
     * 读取运行节点本地日志最大 offset。
     * @return 最大 offset
     */
    private long readMaxOffset() {
        long maxOffset = -1;
        for (File segmentFile : getSortedSegmentFiles()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    Files.newInputStream(segmentFile.toPath()), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    RunNodeLogPullResult.LogRecord logRecord = decodeRecordLine(line);
                    if (logRecord != null && logRecord.getOffset() != null) {
                        maxOffset = Math.max(maxOffset, logRecord.getOffset());
                    }
                }
            } catch (Exception e) {
                log.error("读取运行节点本地日志最大 offset 失败，文件：{}", segmentFile.getAbsolutePath(), e);
            }
        }
        return maxOffset;
    }

    /**
     * 读取当前文件行数
     */
    private long readLong(File file, long defaultValue) {
        if (!file.exists()) {
            return defaultValue;
        }
        try {
            List<String> lineList = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            if (CollUtil.isEmpty(lineList)) {
                return defaultValue;
            }
            return Long.parseLong(lineList.get(0));
        } catch (Exception e) {
            log.error("读取运行节点事件确认 offset 失败。", e);
            return defaultValue;
        }
    }

    /**
     * 将管理端已确认处理的位置写入独立 ACK 文件。
     */
    private boolean writeAckOffset(long offset) {
        try {
            Files.write(getAckFile().toPath(), Collections.singletonList(String.valueOf(offset)), StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            log.error("写入运行节点事件确认 offset 失败。", e);
            return false;
        }
    }

    /**
     * 获取已排序的段文件列表。
     */
    private List<File> getSortedSegmentFiles() {
        // 通过文件前缀以及后缀得到文件 list
        File[] files = getEventDir().listFiles(file -> file.isFile()
                && file.getName().startsWith(EVENT_FILE_PREFIX)
                && file.getName().endsWith(EVENT_FILE_SUFFIX));
        List<File> segmentFiles = new ArrayList<>();
        if (files != null) {
            Collections.addAll(segmentFiles, files);
        }
        segmentFiles.sort(Comparator.comparingLong(this::readSegmentBaseOffset));
        return segmentFiles;
    }

    /**
     * 通过段文件首条事件反推该文件所属的段起始 offset。
     */
    private long readSegmentBaseOffset(File file) {
        long firstOffset = readFirstOffset(file, Long.MAX_VALUE);
        return firstOffset == Long.MAX_VALUE ? Long.MAX_VALUE : resolveSegmentBaseOffset(firstOffset);
    }

    private long readFirstOffset(File file, long defaultValue) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                RunNodeLogPullResult.LogRecord logRecord = decodeRecordLine(line);
                return logRecord == null || logRecord.getOffset() == null ? defaultValue : logRecord.getOffset();
            }
        } catch (Exception e) {
            log.error("读取运行节点本地日志分段首个 offset 失败，文件：{}", file.getAbsolutePath(), e);
        }
        return defaultValue;
    }

    private long readLastOffset(File file, long defaultValue) {
        long lastOffset = defaultValue;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                RunNodeLogPullResult.LogRecord logRecord = decodeRecordLine(line);
                if (logRecord != null && logRecord.getOffset() != null) {
                    lastOffset = logRecord.getOffset();
                }
            }
        } catch (Exception e) {
            log.error("读取运行节点本地日志分段最后 offset 失败，文件：{}", file.getAbsolutePath(), e);
        }
        return lastOffset;
    }

    /**
     * 计算 offset 所在段的起始 offset。比如说现在的偏移量为5010，那么这个方法会返回5000，即5010所在的分段的起始偏移量。
     */
    private long resolveSegmentBaseOffset(long offset) {
        return offset / SEGMENT_EVENT_COUNT * SEGMENT_EVENT_COUNT;
    }

    /**
     * 将通用记录编码成单行，避免载荷内容里的换行破坏文件分隔。
     */
    private byte[] encodeRecordLine(RunNodeLogPullResult.LogRecord logRecord) {
        byte[] jsonBytes = GsonTool.toJson(logRecord).getBytes(StandardCharsets.UTF_8);
        String line = Base64.getEncoder().encodeToString(jsonBytes) + '\n';
        return line.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 读取 Base64 编码的通用日志记录。
     */
    private RunNodeLogPullResult.LogRecord decodeRecordLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        String record = line.trim();
        String json = new String(Base64.getDecoder().decode(record), StandardCharsets.UTF_8);
        return GsonTool.fromJson(json, RunNodeLogPullResult.LogRecord.class);
    }

    private File getEventDir() {
        return new File(RunNodeFilePath.getDataBasePath(), eventDirName);
    }

    /**
     * 根据段起始 offset 映射到固定 slot 文件。
     */
    private File getSegmentFile(long segmentBaseOffset) {
        long segmentIndex = segmentBaseOffset / SEGMENT_EVENT_COUNT;
        // 段序号映射到固定 slot，达到循环复用文件的目的。
        long slotIndex = Math.floorMod(segmentIndex, SEGMENT_FILE_COUNT);
        return new File(getEventDir(), String.format(EVENT_FILE_PREFIX + "%05d" + EVENT_FILE_SUFFIX, slotIndex));
    }

    private File getAckFile() {
        return new File(getEventDir(), ACK_FILE_NAME);
    }

    /**
     * 拉取租约。
     */
    private static final class PullLease {
        private final String consumerId;
        private final String leaseToken;
        private final RunNodeLogPullResult pullResult;
        private final long firstOffset;
        private final long lastOffset;
        private long expiresAtMillis;

        private PullLease(String consumerId, String leaseToken, RunNodeLogPullResult pullResult,
                          long expiresAtMillis) {
            this.consumerId = consumerId;
            this.leaseToken = leaseToken;
            this.pullResult = pullResult;
            this.firstOffset = pullResult.getLogList().get(0).getOffset();
            List<RunNodeLogPullResult.LogRecord> logList = pullResult.getLogList();
            this.lastOffset = logList.get(logList.size() - 1).getOffset();
            this.expiresAtMillis = expiresAtMillis;
        }

        private void refresh(long now) {
            expiresAtMillis = now + PULL_LEASE_TIMEOUT_MILLIS;
        }

        private boolean matches(String consumerId, String leaseToken) {
            return this.consumerId.equals(consumerId) && this.leaseToken.equals(leaseToken);
        }

        private boolean containsOffset(Long offset) {
            return offset != null && offset >= firstOffset && offset <= lastOffset;
        }
    }

    private static final class AckLease {

        /**
         * 确认的消费者 ID。
         */
        private final String consumerId;

        /**
         * 确认的租约令牌。
         */
        private final String leaseToken;

        /**
         * 确认的偏移量。
         */
        private final Long offset;

        private AckLease(String consumerId, String leaseToken, Long offset) {
            this.consumerId = consumerId;
            this.leaseToken = leaseToken;
            this.offset = offset;
        }

        private boolean matches(String consumerId, String leaseToken, Long offset) {
            return this.consumerId.equals(consumerId) && this.leaseToken.equals(leaseToken)
                    && this.offset.equals(offset);
        }
    }

    /**
     * 内存日志列表转出的不可变写入快照。
     */
    private static final class FlushBatch {
        /**
         * 目标段起始 offset。
         */
        private final long segmentBaseOffset;

        /**
         * 待写入的日志记录，由后台线程统一编码。
         */
        private final List<RunNodeLogPullResult.LogRecord> logRecordList;

        /**
         * 创建一个等待后台线程顺序落盘的不可变批次。
         */
        private FlushBatch(long segmentBaseOffset, List<RunNodeLogPullResult.LogRecord> logRecordList) {
            this.segmentBaseOffset = segmentBaseOffset;
            this.logRecordList = logRecordList;
        }

    }

}
