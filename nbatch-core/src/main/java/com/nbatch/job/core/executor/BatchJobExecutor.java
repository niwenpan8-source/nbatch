package com.nbatch.job.core.executor;

import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.AdminBiz;
import com.nbatch.job.core.biz.client.AdminBizClient;
import com.nbatch.job.core.handler.IJobHandler;
import com.nbatch.job.core.handler.RunNodeStopHandler;
import com.nbatch.job.core.handler.annotation.BatchJob;
import com.nbatch.job.core.handler.impl.MethodJobHandler;
import com.nbatch.job.core.files.RunNodeFilePath;
import com.nbatch.job.core.server.EmbedServer;
import com.nbatch.job.core.files.RunNodeLogEventLog;
import com.nbatch.job.core.files.RunNodeLogDetailLog;
import com.nbatch.job.core.util.IpUtil;
import com.nbatch.job.core.util.NetUtil;
import com.nbatch.job.core.util.SpringUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 作业执行器
 */
@Slf4j
public class BatchJobExecutor {

    // ---------------------- 参数 ----------------------
    @Setter
    private String adminAddresses;
    @Setter
    private String accessToken;
    @Setter
    private int timeout;
    @Setter
    private String appName;
    @Setter
    private String address;
    @Setter
    private String ip;
    @Setter
    private int port;
    @Setter
    private String dataPath;

    /** 启动阶段保存关闭处理器，避免销毁阶段再从 Spring 容器查找 Bean。 */
    private RunNodeStopHandler runNodeStopHandler;


    // ---------------------- start + stop ----------------------
    public void start() throws Exception {

        // 初始化运行节点数据与脚本目录
        RunNodeFilePath.initDataPath(dataPath);

        // 初始化管理端客户端
        initAdminBizList(adminAddresses, accessToken, timeout);

        initRunNodeStopHandler();

        // 初始化运行节点事件日志
        RunNodeLogEventLog.getInstance().start();

        // 初始化运行节点详细日志本地队列
        RunNodeLogDetailLog.getInstance().start();

        // 初始化执行器服务
        initEmbedServer(address, ip, port, appName, accessToken);
    }

    public void destroy() {
        // 先停止节点任务；RPC 服务保持可用，管理端可以继续查询节点是否已退出。
        shutdownRunNodes();

        // 所有停止请求已发出并完成有限等待后，再停止执行器服务。
        stopEmbedServer();

        JOB_HANDLER_REPOSITORY.clear();

        // 停止运行节点详细日志本地队列
        RunNodeLogDetailLog.getInstance().toStop();

        // 关闭事件日志刷盘线程，确保剩余闭环事件落盘。
        RunNodeLogEventLog.getInstance().toStop();

    }

    private void shutdownRunNodes() {
        try {
            if (runNodeStopHandler != null) {
                runNodeStopHandler.shutdownRunNodes();
            }
        } catch (Throwable e) {
            log.error("停止运行节点时发生错误。", e);
        } finally {
            runNodeStopHandler = null;
        }
    }

    private void initRunNodeStopHandler() {
        try {
            runNodeStopHandler = SpringUtil.getBean(RunNodeStopHandler.class);
        } catch (Throwable e) {
            // 未启用作业节点模块时不存在该 Bean，不影响普通任务执行器启动。
            log.debug("运行节点停止处理器不可用。", e);
        }
    }


    // ---------------------- 管理端客户端 ----------------------
    @Getter
    private static List<AdminBiz> adminBizList;

    private void initAdminBizList(String adminAddresses, String accessToken, int timeout) {
        if (StrUtil.isNotBlank(adminAddresses)) {
            for (String address : adminAddresses.trim().split(StrPool.COMMA)) {
                if (StrUtil.isNotBlank(address)) {

                    AdminBiz adminBiz = new AdminBizClient(address.trim(), accessToken, timeout);

                    if (adminBizList == null) {
                        adminBizList = new ArrayList<>();
                    }
                    adminBizList.add(adminBiz);
                }
            }
        }
    }

    // ---------------------- 执行器服务 ----------------------
    private EmbedServer embedServer = null;

    private void initEmbedServer(String address, String ip, int port, String appName, String accessToken) {

        // 补充 IP 和端口
        port = port > 0 ? port : NetUtil.findAvailablePort(9999);
        ip = StrUtil.isNotBlank(ip) ? ip : IpUtil.getIp();

        // 生成服务地址
        if (StrUtil.isBlank(address)) {
            // 未配置地址时使用 IP 和端口生成注册地址
            String ipPortAddress = IpUtil.getIpPort(ip, port);
            address = "http://{ip_port}/".replace("{ip_port}", ipPortAddress);
        }

        // 校验访问令牌
        if (StrUtil.isBlank(accessToken)) {
            log.warn(">>>>>>>>>>> 作业 accessToken 为空，为保证系统安全，请配置 accessToken。");
        }

        // 启动服务
        embedServer = new EmbedServer();
        embedServer.start(address, port, appName, accessToken);
    }

    private void stopEmbedServer() {
        // 停止服务
        if (embedServer != null) {
            try {
                embedServer.stop();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }


    // ---------------------- 作业处理器仓库 ----------------------
    private static final ConcurrentMap<String, IJobHandler> JOB_HANDLER_REPOSITORY = new ConcurrentHashMap<>();

    public static IJobHandler loadJobHandler(String name) {
        return JOB_HANDLER_REPOSITORY.get(name);
    }

    public static IJobHandler registerJobHandler(String name, IJobHandler jobHandler) {
        log.info(">>>>>>>>>>> 作业处理器注册成功，名称：{}，处理器：{}", name, jobHandler);
        return JOB_HANDLER_REPOSITORY.put(name, jobHandler);
    }

    protected void registerJobHandler(BatchJob batchJob, Object bean, Method executeMethod) {
        if (batchJob == null) {
            return;
        }

        String name = batchJob.value();
        // 提取后续会多次使用的变量
        Class<?> clazz = bean.getClass();
        String methodName = executeMethod.getName();
        if (StrUtil.isBlank(name)) {
            throw new RuntimeException("job method-jobHandler name invalid, for[" + clazz + "#" + methodName + "] .");
        }
        if (loadJobHandler(name) != null) {
            throw new RuntimeException("job jobHandler[" + name + "] naming conflicts.");
        }

        executeMethod.setAccessible(true);

        // 查找初始化和销毁方法
        Method initMethod = null;
        Method destroyMethod = null;

        if (StrUtil.isNotBlank(batchJob.init())) {
            try {
                initMethod = clazz.getDeclaredMethod(batchJob.init());
                initMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("job method-jobHandler initMethod invalid, for[" + clazz + "#" + methodName + "] .");
            }
        }
        if (StrUtil.isNotBlank(batchJob.destroy())) {
            try {
                destroyMethod = clazz.getDeclaredMethod(batchJob.destroy());
                destroyMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("job method-jobHandler destroyMethod invalid, for[" + clazz + "#" + methodName + "] .");
            }
        }

        // 注册作业处理器
        registerJobHandler(name, new MethodJobHandler(bean, executeMethod, initMethod, destroyMethod));

    }


}
