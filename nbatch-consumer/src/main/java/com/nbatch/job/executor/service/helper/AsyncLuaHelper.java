package com.nbatch.job.executor.service.helper;

import com.nbatch.job.handler.exception.HandlerException;
import lombok.extern.log4j.Log4j2;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.VarArgFunction;

import java.util.concurrent.*;

import static com.nbatch.job.handler.enums.ExceptionCodeEnum.LUA_SCRIPT_FAIL;

/**
 * 异步回调函数
 * @author Mr.ni
 */
@Log4j2
public class AsyncLuaHelper extends VarArgFunction {

    private final static ExecutorService EXECUTOR = new ThreadPoolExecutor(5, 5, 20,
            TimeUnit.MINUTES, new ArrayBlockingQueue<>(1000),
            new ThreadPoolExecutor.DiscardPolicy());

    /**
     * Lua 全局环境
     */
    private final Globals globals;

    public AsyncLuaHelper(Globals globals) {
        this.globals = globals;
    }

    /**
     * 异步执行任务，并在完成后回调 Lua 函数
     * @param taskName 任务名称（用于日志）
     * @param delayMs 模拟耗时（毫秒）
     * @param callback Lua 回调函数（LuaFunction 类型）
     */
    public void asyncCall(String taskName, long delayMs, LuaValue callback) {
        if (!callback.isfunction()) {
            throw new IllegalArgumentException("Callback must be a Lua function");
        }

        EXECUTOR.submit(() -> {
            try {
                // 模拟耗时操作（如网络请求、数据库查询）
                Thread.sleep(delayMs);
                String result = "Result from " + taskName;

                // 调度到主线程执行回调
                scheduleOnMainThread(() -> {
                    try {
                        callback.call(LuaValue.valueOf(result));
                    } catch (Exception e) {
                        log.error("Lua callback error: {}", e.getMessage());
                        throw new HandlerException(LUA_SCRIPT_FAIL.getCode(), e);
                    }
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // 在中断情况下也尝试调度回调，传递中断信息
                scheduleOnMainThread(() -> {
                    try {
                        callback.call(LuaValue.valueOf("Task interrupted: " + taskName));
                    } catch (Exception callbackException) {
                        log.error("Lua callback error after interruption: {}", callbackException.getMessage());
                        throw new HandlerException(LUA_SCRIPT_FAIL.getCode(), e);
                    }
                });
            }
        });
    }

    /**
     * 模拟"调度到主线程"（实际项目中可用 Handler、SwingUtilities 等）
     */
    private void scheduleOnMainThread(Runnable task) {
        // 使用同步机制确保线程安全，但避免长时间阻塞
        synchronized (globals) {
            // 注意：仅用于单线程测试！生产环境需更安全机制
            task.run();
        }
    }

    /**
     * 关闭线程池，释放资源
     */
    public static void shutdown() {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
                if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                    System.err.println("Thread pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
