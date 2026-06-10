package com.nbatch.job.core.executor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.AdminBiz;
import com.nbatch.job.core.biz.client.AdminBizClient;
import com.nbatch.job.core.handler.IJobHandler;
import com.nbatch.job.core.handler.annotation.BatchJob;
import com.nbatch.job.core.handler.impl.MethodJobHandler;
import com.nbatch.job.core.log.JobFileAppender;
import com.nbatch.job.core.log.RunNodeEventDataPath;
import com.nbatch.job.core.server.EmbedServer;
import com.nbatch.job.core.thread.JobLogFileCleanThread;
import com.nbatch.job.core.thread.JobThread;
import com.nbatch.job.core.thread.RunNodeLogEventLog;
import com.nbatch.job.core.thread.RunNodeLogDetailCallbackThread;
import com.nbatch.job.core.thread.TriggerCallbackThread;
import com.nbatch.job.core.util.IpUtil;
import com.nbatch.job.core.util.NetUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * job executor
 * @author Mr.ni
 */
@Slf4j
public class BatchJobExecutor {

    // ---------------------- param ----------------------
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
    private String logPath;
    @Setter
    private String dataPath;
    @Setter
    private int logRetentionDays;


    // ---------------------- start + stop ----------------------
    public void start() throws Exception {

        // init logpath
        JobFileAppender.initLogPath(logPath);

        // init data path
        RunNodeEventDataPath.initDataPath(dataPath);

        // init invoker, admin-client
        initAdminBizList(adminAddresses, accessToken, timeout);

        // init JobLogFileCleanThread
        JobLogFileCleanThread.getInstance().start(logRetentionDays);

        // init run node event log
        RunNodeLogEventLog.getInstance().start();

        // init TriggerCallbackThread
        TriggerCallbackThread.getInstance().start();

        // init RunNodeLogDetailCallbackThread
        RunNodeLogDetailCallbackThread.getInstance().start();

        // init executor-server
        initEmbedServer(address, ip, port, appName, accessToken);
    }

    public void destroy() {
        // destroy executor-server
        stopEmbedServer();

        // destroy jobThreadRepository
        if (CollUtil.isNotEmpty(JOB_THREAD_REPOSITORY)) {
            for (Map.Entry<String, JobThread> item : JOB_THREAD_REPOSITORY.entrySet()) {
                JobThread oldJobThread = removeJobThread(item.getKey(), "web container destroy and kill the job.");
                // wait for job thread push result to callback queue
                if (oldJobThread != null) {
                    try {
                        oldJobThread.join();
                    } catch (InterruptedException e) {
                        log.error(">>>>>>>>>>> job, JobThread destroy(join) error, jobId:{}", item.getKey(), e);
                    }
                }
            }
            JOB_THREAD_REPOSITORY.clear();
        }
        JOB_HANDLER_REPOSITORY.clear();

        // destroy JobLogFileCleanThread
        JobLogFileCleanThread.getInstance().toStop();

        // destroy TriggerCallbackThread
        TriggerCallbackThread.getInstance().toStop();

        // destroy RunNodeLogDetailCallbackThread
        RunNodeLogDetailCallbackThread.getInstance().toStop();

    }


    // ---------------------- admin-client (rpc invoker) ----------------------
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

    // ---------------------- executor-server (rpc provider) ----------------------
    private EmbedServer embedServer = null;

    private void initEmbedServer(String address, String ip, int port, String appName, String accessToken) {

        // fill ip port
        port = port > 0 ? port : NetUtil.findAvailablePort(9999);
        ip = StrUtil.isNotBlank(ip) ? ip : IpUtil.getIp();

        // generate address
        if (StrUtil.isBlank(address)) {
            // registry-address：default use address to registry , otherwise use ip:port if address is null
            String ipPortAddress = IpUtil.getIpPort(ip, port);
            address = "http://{ip_port}/".replace("{ip_port}", ipPortAddress);
        }

        // accessToken
        if (StrUtil.isBlank(accessToken)) {
            log.warn(">>>>>>>>>>> job accessToken is empty. To ensure system security, please set the accessToken.");
        }

        // start
        embedServer = new EmbedServer();
        embedServer.start(address, port, appName, accessToken);
    }

    private void stopEmbedServer() {
        // stop provider factory
        if (embedServer != null) {
            try {
                embedServer.stop();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }


    // ---------------------- job handler repository ----------------------
    private static final ConcurrentMap<String, IJobHandler> JOB_HANDLER_REPOSITORY = new ConcurrentHashMap<>();

    public static IJobHandler loadJobHandler(String name) {
        return JOB_HANDLER_REPOSITORY.get(name);
    }

    public static IJobHandler registerJobHandler(String name, IJobHandler jobHandler) {
        log.info(">>>>>>>>>>> job register jobHandler success, name:{}, jobHandler:{}", name, jobHandler);
        return JOB_HANDLER_REPOSITORY.put(name, jobHandler);
    }

    protected void registerJobHandler(BatchJob batchJob, Object bean, Method executeMethod) {
        if (batchJob == null) {
            return;
        }

        String name = batchJob.value();
        //make and simplify the variables since they'll be called several times later
        Class<?> clazz = bean.getClass();
        String methodName = executeMethod.getName();
        if (StrUtil.isBlank(name)) {
            throw new RuntimeException("job method-jobHandler name invalid, for[" + clazz + "#" + methodName + "] .");
        }
        if (loadJobHandler(name) != null) {
            throw new RuntimeException("job jobHandler[" + name + "] naming conflicts.");
        }

        executeMethod.setAccessible(true);

        // init and destroy
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

        // registry jobHandler
        registerJobHandler(name, new MethodJobHandler(bean, executeMethod, initMethod, destroyMethod));

    }


    // ---------------------- job thread repository ----------------------
    private static final ConcurrentMap<String, JobThread> JOB_THREAD_REPOSITORY = new ConcurrentHashMap<>();

    public static JobThread registerJobThread(String jobId, IJobHandler handler, String removeOldReason) {
        JobThread newJobThread = new JobThread(jobId, handler);
        newJobThread.start();
        log.info(">>>>>>>>>>> job regist JobThread success, jobId:{}, handler:{}", jobId, handler);
        // putIfAbsent | oh my god, map's put method return the old value!!!
        JobThread oldJobThread = JOB_THREAD_REPOSITORY.put(jobId, newJobThread);
        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();
        }

        return newJobThread;
    }

    public static JobThread removeJobThread(String jobId, String removeOldReason) {
        JobThread oldJobThread = JOB_THREAD_REPOSITORY.remove(jobId);
        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();

            return oldJobThread;
        }
        return null;
    }

    public static JobThread loadJobThread(String jobId) {
        return JOB_THREAD_REPOSITORY.get(jobId);
    }
}
