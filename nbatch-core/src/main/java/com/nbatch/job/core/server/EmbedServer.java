package com.nbatch.job.core.server;

import cn.hutool.core.util.StrUtil;
import com.nbatch.job.core.biz.ExecutorBiz;
import com.nbatch.job.core.biz.impl.ExecutorBizImpl;
import com.nbatch.job.core.biz.model.ExecuteWorkParam;
import com.nbatch.job.core.biz.model.ReturnT;
import com.nbatch.job.core.biz.model.RunNodeLogAckParam;
import com.nbatch.job.core.biz.model.RunNodeLogPullParam;
import com.nbatch.job.core.biz.model.RunNodeStatusQueryParam;
import com.nbatch.job.core.biz.model.StopRunNodeParam;
import com.nbatch.job.core.constant.HandleCodeConstant;
import com.nbatch.job.core.thread.ExecutorRegistryThread;
import com.nbatch.job.core.util.GsonTool;
import com.nbatch.job.core.util.JobRemotingUtil;
import com.nbatch.job.core.util.ThrowableUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于netty实现的通信服务端
 */
@Slf4j
public class EmbedServer {

    private static final int BIZ_THREAD_CORE_POOL_SIZE = Math.max(4, Runtime.getRuntime().availableProcessors());
    private static final int BIZ_THREAD_MAX_POOL_SIZE = Math.max(BIZ_THREAD_CORE_POOL_SIZE, BIZ_THREAD_CORE_POOL_SIZE * 2);
    private static final int BIZ_THREAD_QUEUE_CAPACITY = 2000;

    private ExecutorBiz executorBiz;
    private Thread thread;

    public void start(final String address, final int port, final String appName, final String accessToken) {
        executorBiz = new ExecutorBizImpl();
        thread = new Thread(() -> {
            // 读取请求参数
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            ThreadPoolExecutor bizThreadPool = new ThreadPoolExecutor(
                    BIZ_THREAD_CORE_POOL_SIZE,
                    BIZ_THREAD_MAX_POOL_SIZE,
                    60L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(BIZ_THREAD_QUEUE_CAPACITY),
                    new EmbedServerThreadFactory(),
                    (r, executor) -> {
                        throw new RuntimeException("job, EmbedServer bizThreadPool is EXHAUSTED!");
                    });
            // 让业务线程池的核心线程在服务器启动时就全部就绪，确保后续到达的请求无需等待线程创建即可立即被处理。
            bizThreadPool.prestartAllCoreThreads();
            try {
            // 启动服务
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel channel) {
                                channel.pipeline()
                                        .addLast(new IdleStateHandler(0, 0, 30 * 3, TimeUnit.SECONDS))  // beat 3N, close if idle
                                        .addLast(new HttpServerCodec())
                                        .addLast(new HttpObjectAggregator(5 * 1024 * 1024))  // merge request & reponse to FULL
                                        .addLast(new EmbedHttpServerHandler(executorBiz, accessToken, bizThreadPool));
                            }
                        })
                        .childOption(ChannelOption.SO_KEEPALIVE, true);

                // 绑定端口
                ChannelFuture future = bootstrap.bind(port).sync();

                log.info(">>>>>>>>>>> 作业远程服务启动成功，网络类型 = {}，端口 = {}", EmbedServer.class, port);

            // 启动注册线程
                startRegistry(appName, address);

                // 等待服务停止
                future.channel().closeFuture().sync();

            } catch (InterruptedException e) {
                log.info(">>>>>>>>>>> 作业远程服务停止。");
            } catch (Throwable e) {
                log.error(">>>>>>>>>>> 作业远程服务发生错误。", e);
            } finally {
            // 停止服务
                try {
                    workerGroup.shutdownGracefully();
                    bossGroup.shutdownGracefully();
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
        });
        // 设置为守护线程，业务线程结束后由 JVM 自动退出
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() throws Exception {
        // 停止服务线程
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }

        // 停止注册线程
        stopRegistry();
        log.info(">>>>>>>>>>> 作业远程服务销毁成功。");
    }


    // ---------------------- registry ----------------------

    /**
     * Netty HTTP 请求处理器
     */
    public static class EmbedHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private final ExecutorBiz executorBiz;
        private final String accessToken;
        private final ThreadPoolExecutor bizThreadPool;

        public EmbedHttpServerHandler(ExecutorBiz executorBiz, String accessToken, ThreadPoolExecutor bizThreadPool) {
            this.executorBiz = executorBiz;
            this.accessToken = accessToken;
            this.bizThreadPool = bizThreadPool;
        }

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, FullHttpRequest msg) {
            // 解析请求
            String requestData = msg.content().toString(CharsetUtil.UTF_8);
            String uri = msg.uri();
            HttpMethod httpMethod = msg.method();
            boolean keepAlive = HttpUtil.isKeepAlive(msg);
            String accessTokenReq = msg.headers().get(JobRemotingUtil.BATCH_JOB_ACCESS_TOKEN);

            // 异步调用业务方法
            bizThreadPool.execute(() -> {
                // 执行请求处理
                Object responseObj = process(httpMethod, uri, requestData, accessTokenReq);

                // 转换为 JSON
                String responseJson = GsonTool.toJson(responseObj);

                // 写回响应
                writeResponse(ctx, keepAlive, responseJson);
            });
        }

        private Object process(HttpMethod httpMethod, String uri, String requestData, String accessTokenReq) {
            // 校验请求
            if (HttpMethod.POST != httpMethod) {
                return new ReturnT<String>(HandleCodeConstant.HANDLE_CODE_FAIL, "无效请求，不支持该 HTTP 方法。");
            }
            if (StrUtil.isBlank(uri)) {
                return new ReturnT<String>(HandleCodeConstant.HANDLE_CODE_FAIL, "无效请求，URI 映射不能为空。");
            }
            if (StrUtil.isNotBlank(accessToken) && !accessToken.equals(accessTokenReq)) {
                return new ReturnT<String>(HandleCodeConstant.HANDLE_CODE_FAIL, "访问令牌错误。");
            }

            // 根据 URI 映射业务服务
            try {
                switch (uri) {
                    case "/beat":
                        return executorBiz.beat();
                    case "/runWork":
                        ExecuteWorkParam executeWorkParam = GsonTool.fromJson(requestData, ExecuteWorkParam.class);
                        return executorBiz.runWork(executeWorkParam);
                    case "/stopRunNode":
                        StopRunNodeParam stopRunNodeParam = GsonTool.fromJson(requestData, StopRunNodeParam.class);
                        return executorBiz.stopRunNode(stopRunNodeParam);
                    case "/queryRunNodeStatus":
                        // 状态查询只读取执行器内存，不等待业务任务执行完成。
                        RunNodeStatusQueryParam queryParam = GsonTool.fromJson(requestData, RunNodeStatusQueryParam.class);
                        return executorBiz.queryRunNodeStatus(queryParam);
                    case "/pullRunNodeLog":
                        RunNodeLogPullParam pullParam = GsonTool.fromJson(requestData, RunNodeLogPullParam.class);
                        return executorBiz.pullRunNodeLog(pullParam);
                    case "/ackRunNodeLog":
                        RunNodeLogAckParam ackParam = GsonTool.fromJson(requestData, RunNodeLogAckParam.class);
                        return executorBiz.ackRunNodeLog(ackParam);
                    case "/pullRunNodeLogDetail":
                        RunNodeLogPullParam detailPullParam = GsonTool.fromJson(requestData, RunNodeLogPullParam.class);
                        return executorBiz.pullRunNodeLogDetail(detailPullParam);
                    case "/ackRunNodeLogDetail":
                        RunNodeLogAckParam detailAckParam = GsonTool.fromJson(requestData, RunNodeLogAckParam.class);
                        return executorBiz.ackRunNodeLogDetail(detailAckParam);
                    default:
                        return new ReturnT<String>(HandleCodeConstant.HANDLE_CODE_FAIL, "无效请求，未找到 URI 映射：" + uri);
                }
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
                return new ReturnT<String>(HandleCodeConstant.HANDLE_CODE_FAIL, "请求处理异常：" + ThrowableUtil.toString(e));
            }
        }

        /**
         * 写回 HTTP 响应
         */
        private void writeResponse(ChannelHandlerContext ctx, boolean keepAlive, String responseJson) {
            // 写回响应内容
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(responseJson, CharsetUtil.UTF_8));   //  Unpooled.wrappedBuffer(responseJson)
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=UTF-8");       // HttpHeaderValues.TEXT_PLAIN.toString()
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            if (keepAlive) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }
            ctx.writeAndFlush(response);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error(">>>>>>>>>>> 作业提供端 Netty HTTP 服务捕获到异常。", cause);
            ctx.close();
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                ctx.channel().close();      // beat 3N, close if idle
                log.debug(">>>>>>>>>>> 作业提供端 Netty HTTP 服务关闭空闲连接。");
            } else {
                super.userEventTriggered(ctx, evt);
            }
        }
    }

    // ---------------------- registry ----------------------

    public void startRegistry(final String appName, final String address) {
        // 启动注册线程
        ExecutorRegistryThread.getInstance().start(appName, address);
    }

    public void stopRegistry() {
        // 停止注册线程
        ExecutorRegistryThread.getInstance().toStop();
    }

    private static final class EmbedServerThreadFactory implements java.util.concurrent.ThreadFactory {

        private final AtomicInteger threadIndex = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "job, EmbedServer bizThreadPool-" + threadIndex.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
