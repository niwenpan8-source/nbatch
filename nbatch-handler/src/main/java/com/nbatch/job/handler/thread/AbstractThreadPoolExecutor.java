package com.nbatch.job.handler.thread;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.enums.RunNodeExecutionStateEnum;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 可标识、可停止任务的通用执行框架。
 * 具体业务仅通过任务准入和生命周期回调接入。
 */
@Log4j2
public abstract class AbstractThreadPoolExecutor<T extends AbstractThreadPoolExecutor.ManagedTask>
        extends ThreadPoolExecutor {

    /**
     * 线程池托管任务，同时保存 Future 状态和业务停止入口。
     */
    public abstract static class ManagedTask extends FutureTask<Void> {

        /**
         * 任务是否已启动。
         */
        volatile boolean started;

        /**
         * 任务是否已退出。
         */
        volatile boolean exited;

        /**
         * 任务是否已处理完成。
         */
        final AtomicBoolean completionHandled = new AtomicBoolean();

        /** 任务是否已停止。 */
        private final AtomicBoolean stopRequested = new AtomicBoolean();

        protected ManagedTask() {
            this(new TaskCallable());
        }

        private ManagedTask(TaskCallable callable) {
            super(callable);
            callable.bind(this);
        }

        /**
         * 返回任务唯一标识。
         */
        public abstract String getTaskId();

        /**
         * 执行业务逻辑。
         */
        protected abstract void executeTask();

        /**
         * 首次收到停止请求时释放业务资源。
         */
        protected abstract void  onStopRequested();

        /**
         * 首次停止时通知业务并中断正在执行的线程。
         */
        public final boolean requestStop() {
            if (!stopRequested.compareAndSet(false, true)) {
                return false;
            }
            // 执行任务自带的停止，可以重写，安全退出
            try {
                onStopRequested();
            } catch (Exception e) {
                log.warn("停止任务资源时发生错误，taskId：{}", getTaskId(), e);
            }
            cancel(true);
            return true;
        }

        public final boolean isStopRequested() {
            return stopRequested.get();
        }

        void markStarted() {
            started = true;
        }

        void markExited() {
            exited = true;
        }

        /**
         * 返回任务实时状态。
         */
        String resolveState() {
            if (exited) {
                return RunNodeExecutionStateEnum.ABSENT.getValue();
            }
            if (stopRequested.get()) {
                return RunNodeExecutionStateEnum.STOP_REQUESTED.getValue();
            }
            return started ? RunNodeExecutionStateEnum.RUNNING.getValue() : RunNodeExecutionStateEnum.QUEUED.getValue();
        }

        private static final class TaskCallable implements Callable<Void> {
            private ManagedTask task;

            private void bind(ManagedTask task) {
                this.task = task;
            }

            @Override
            public Void call() {
                task.executeTask();
                return null;
            }
        }
    }

    /**
     * 任务生命周期事件。
     */
    protected enum TaskEvent {
        STARTED, SUCCEEDED, FAILED, STOPPED
    }

    private final String threadPoolKey;
    private final Map<String, T> taskMap = new ConcurrentHashMap<>();

    protected AbstractThreadPoolExecutor(String threadPoolKey,
                                         int corePoolSize,
                                         int maximumPoolSize,
                                         long keepAliveTime,
                                         TimeUnit unit,
                                         BlockingQueue<Runnable> workQueue,
                                         ThreadFactory threadFactory,
                                         RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        this.threadPoolKey = threadPoolKey;
    }

    /**
     * 原子登记后提交，重复 taskId 视为幂等成功。
     */
    protected synchronized boolean executeTask(T task) {
        // taskId 为空时不允许提交
        String taskId = task == null ? null : task.getTaskId();
        if (task == null || StrUtil.isBlank(taskId)) {
            log.error("任务为空或 taskId 为空。");
            return false;
        }
        // 已登记的任务不允许重复提交
        if (taskMap.containsKey(taskId)) {
            return true;
        }
        // 得到当前正在执行的任务
        List<T> activeTasks = taskMap.values().stream()
                .filter(activeTask -> !activeTask.exited)
                .collect(Collectors.toList());
        if (!canSubmit(task, activeTasks)) {
            return true;
        }

        taskMap.put(taskId, task);
        try {
            super.execute(task);
        } catch (RuntimeException e) {
            taskMap.remove(taskId, task);
            task.cancel(false);
            log.error("提交任务失败，taskId：{}", taskId, e);
            return false;
        }
        log.debug("提交任务，taskId：{}，线程池键：{}，活动线程数：{}，队列大小：{}",
                taskId, threadPoolKey, getActiveCount(), getQueue().size());
        return true;
    }

    /**
     * 停止指定任务；排队任务立即移出队列，运行中任务等待真实退出。
     */
    public int stopTasks(List<String> taskIdList, boolean stopRunning) {
        if (CollUtil.isEmpty(taskIdList)) {
            return 0;
        }
        int stopCount = 0;
        for (String taskId : taskIdList) {
            T task = taskMap.get(taskId);
            if (task == null) {
                continue;
            }
            // 如果任务还没有开始
            if (!task.started && getQueue().remove(task)) {
                if (task.requestStop()) {
                    stopCount++;
                }
                task.markExited();
                completeTask(task);
                // 如果任务已经开始执行，则尝试停止任务
            } else if (stopRunning && task.requestStop()) {
                stopCount++;
            }
        }
        return stopCount;
    }

    /**
     * 查询指定任务的实时状态。
     */
    public void collectTaskStates(List<String> taskIdList, Map<String, String> stateMap) {
        if (CollUtil.isEmpty(taskIdList)) {
            return;
        }
        for (String taskId : taskIdList) {
            T task = taskMap.get(taskId);
            if (task != null) {
                stateMap.put(taskId, task.resolveState());
            }
        }
    }

    /** 返回当前真实运行中的任务，供具体业务做心跳等附加处理。 */
    protected final List<T> getRunningTasks() {
        return taskMap.values().stream()
                .filter(task -> task.started && !task.exited && !task.isStopRequested())
                .collect(Collectors.toList());
    }

    /**
     * 返回尚未退出的任务快照，供执行器进程关闭时补写终态事件。
     */
    protected final List<T> getUnexitedTasks() {
        return taskMap.values().stream()
                .filter(task -> !task.exited)
                .collect(Collectors.toList());
    }

    protected final String getThreadPoolKey() {
        return threadPoolKey;
    }

    /**
     * 任务开始前执行的动作。
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void beforeExecute(Thread thread, Runnable runnable) {
        super.beforeExecute(thread, runnable);
        if (runnable instanceof ManagedTask) {
            T task = (T) runnable;
            task.markStarted();
            if (!task.isStopRequested()) {
                try {
                    onTaskStateChanged(task, TaskEvent.STARTED, null);
                } catch (Exception e) {
                    log.error("任务开始回调失败，taskId：{}", task.getTaskId(), e);
                }
            }
        }
    }

    /**
     * 任务结束后执行的动作。
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        try {
            if (runnable instanceof ManagedTask) {
                T task = (T) runnable;
                task.markExited();
                completeTask(task);
            }
        } finally {
            super.afterExecute(runnable, throwable);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void shutdown() {
        for (T task : taskMap.values()) {
            task.requestStop();
        }
        for (Runnable waitingTask : super.shutdownNow()) {
            if (waitingTask instanceof ManagedTask) {
                ManagedTask task = (ManagedTask) waitingTask;
                task.markExited();
                completeTask((T) task);
            }
        }
    }

    /**
     * 由具体业务决定是否接收任务；activeTasks 包含排队和运行中的任务。
     */
    protected boolean canSubmit(T task, List<T> activeTasks) {
        return true;
    }

    /**
     * 由具体业务消费任务生命周期，例如写事件、刷新监控。
     */
    protected void onTaskStateChanged(T task, TaskEvent event, Throwable failure) {
    }

    /**
     * 任务完成后处理，包括成功、失败、停止
     */
    private void completeTask(T task) {
        if (!task.completionHandled.compareAndSet(false, true)) {
            return;
        }
        Throwable failure = null;
        TaskEvent event = task.isStopRequested() || task.isCancelled() ? TaskEvent.STOPPED : TaskEvent.SUCCEEDED;
        if (event == TaskEvent.SUCCEEDED) {
            try {
                task.get();
            } catch (CancellationException e) {
                event = TaskEvent.STOPPED;
            } catch (ExecutionException e) {
                event = TaskEvent.FAILED;
                failure = e.getCause() == null ? e : e.getCause();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                event = TaskEvent.FAILED;
                failure = e;
            }
        }
        try {
            onTaskStateChanged(task, event, failure);
        } catch (Exception e) {
            log.error("任务完成回调失败，taskId：{}", task.getTaskId(), e);
        } finally {
            taskMap.remove(task.getTaskId(), task);
        }
    }
}
