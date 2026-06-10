package com.nbatch.job.core.thread;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import com.nbatch.job.core.biz.model.RunNodeLogEventParam;
import com.nbatch.job.core.biz.model.RunNodeLogPullResult;
import com.nbatch.job.core.log.RunNodeEventDataPath;
import com.nbatch.job.core.util.GsonTool;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 运行节点本地事件日志，只记录运行节点闭环事件。
 */
@Slf4j
public class RunNodeLogEventLog {

    private static final RunNodeLogEventLog INSTANCE = new RunNodeLogEventLog();
    private static final String EVENT_DIR_NAME = "run-node-event";
    private static final String EVENT_FILE_NAME = "event.log";
    private static final String ACK_FILE_NAME = "ack.offset";
    private static final int DEFAULT_PULL_SIZE = 100;
    private static final long EVENT_RETENTION_MILLIS = TimeUnit.HOURS.toMillis(12);

    private final Object lock = new Object();
    private long nextOffset;
    private long ackOffset;

    public static RunNodeLogEventLog getInstance() {
        return INSTANCE;
    }

    public void start() {
        synchronized (lock) {
            FileUtil.mkdir(getEventDir());
            FileUtil.touch(getEventFile());
            ackOffset = readLong(getAckFile(), -1L);
            nextOffset = Math.max(readMaxOffset() + 1, ackOffset + 1);
            if (nextOffset < 0) {
                nextOffset = 0;
            }
        }
    }

    /**
     * 追加运行节点事件。
     *
     * @param eventParam 运行节点事件参数
     * @return 运行节点事件偏移量
     */
    public long append(RunNodeLogEventParam eventParam) {
        if (eventParam == null) {
            return -1L;
        }
        synchronized (lock) {
            long offset = nextOffset;
            eventParam.setOffset(offset).setTimestamp(System.currentTimeMillis());
            try (FileOutputStream outputStream = new FileOutputStream(getEventFile(), true)) {
                outputStream.write(GsonTool.toJson(eventParam).getBytes(StandardCharsets.UTF_8));
                outputStream.write('\n');
                outputStream.flush();
                outputStream.getFD().sync();
                nextOffset++;
                return offset;
            } catch (Exception e) {
                log.error("append run node event error", e);
                return -1L;
            }
        }
    }

    public RunNodeLogPullResult pull(Long offset, Integer maxSize) {
        synchronized (lock) {
            long startOffset = offset == null ? ackOffset + 1 : Math.max(offset, ackOffset + 1);
            int limit = maxSize == null || maxSize <= 0 ? DEFAULT_PULL_SIZE : Math.min(maxSize, 1000);
            List<RunNodeLogEventParam> eventList = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(getEventFile().toPath()), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    RunNodeLogEventParam eventParam = GsonTool.fromJson(line, RunNodeLogEventParam.class);
                    if (eventParam.getOffset() != null && eventParam.getOffset() >= startOffset) {
                        eventList.add(eventParam);
                        if (eventList.size() >= limit) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.error("pull run node event error", e);
            }
            long nextPullOffset = eventList.isEmpty() ? startOffset : eventList.get(eventList.size() - 1).getOffset() + 1;
            return new RunNodeLogPullResult()
                    .setAckOffset(ackOffset)
                    .setNextOffset(nextPullOffset)
                    .setEventList(eventList);
        }
    }

    public void ack(Long offset) {
        if (offset == null) {
            return;
        }
        synchronized (lock) {
            if (offset <= ackOffset) {
                return;
            }
            ackOffset = offset;
            writeAckOffset();
            cleanExpiredAckedEvents();
        }
    }

    private void cleanExpiredAckedEvents() {
        List<String> remainLineList = new ArrayList<>();
        long expireTime = System.currentTimeMillis() - EVENT_RETENTION_MILLIS;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(getEventFile().toPath()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                RunNodeLogEventParam eventParam = GsonTool.fromJson(line, RunNodeLogEventParam.class);
                if (shouldKeepEvent(eventParam, expireTime)) {
                    remainLineList.add(line);
                }
            }
        } catch (Exception e) {
            log.error("read run node event clean data error", e);
            return;
        }
        try (FileOutputStream outputStream = new FileOutputStream(getEventFile(), false)) {
            for (String line : remainLineList) {
                outputStream.write(line.getBytes(StandardCharsets.UTF_8));
                outputStream.write('\n');
            }
            outputStream.flush();
            outputStream.getFD().sync();
        } catch (Exception e) {
            log.error("write run node event clean data error", e);
        }
    }

    private boolean shouldKeepEvent(RunNodeLogEventParam eventParam, long expireTime) {
        if (eventParam.getOffset() == null || eventParam.getOffset() > ackOffset) {
            return true;
        }
        return eventParam.getTimestamp() != null && eventParam.getTimestamp() >= expireTime;
    }

    private long readMaxOffset() {
        long maxOffset = -1;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(getEventFile().toPath()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                RunNodeLogEventParam eventParam = GsonTool.fromJson(line, RunNodeLogEventParam.class);
                if (eventParam.getOffset() != null) {
                    maxOffset = Math.max(maxOffset, eventParam.getOffset());
                }
            }
        } catch (Exception e) {
            log.error("read run node event max offset error", e);
        }
        return maxOffset;
    }

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
            log.error("read run node event ack offset error", e);
            return defaultValue;
        }
    }

    private void writeAckOffset() {
        try {
            Files.write(getAckFile().toPath(), Collections.singletonList(String.valueOf(ackOffset)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("write run node event ack offset error", e);
        }
    }

    private File getEventDir() {
        return new File(RunNodeEventDataPath.getDataBasePath(), EVENT_DIR_NAME);
    }

    private File getEventFile() {
        return new File(getEventDir(), EVENT_FILE_NAME);
    }

    private File getAckFile() {
        return new File(getEventDir(), ACK_FILE_NAME);
    }
}
