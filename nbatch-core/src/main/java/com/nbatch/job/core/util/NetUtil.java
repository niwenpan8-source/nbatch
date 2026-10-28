package com.nbatch.job.core.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * 网络工具类
 */
@Slf4j
public class NetUtil {

    /**
     * 查找可用端口
     *
     * @param defaultPort default port
     */
    public static int findAvailablePort(int defaultPort) {
        int portTmp = defaultPort;
        while (portTmp < 65535) {
            if (!isPortUsed(portTmp)) {
                return portTmp;
            } else {
                portTmp++;
            }
        }
        portTmp = defaultPort--;
        while (portTmp > 0) {
            if (!isPortUsed(portTmp)) {
                return portTmp;
            } else {
                portTmp--;
            }
        }
        throw new RuntimeException("no available port.");
    }

    /**
     * 检查端口是否已被占用
     *
     * @param port  端口
     */
    public static boolean isPortUsed(int port) {
        boolean used;
        try (ServerSocket ignored = new ServerSocket(port)) {
            used = false;
        } catch (IOException e) {
            log.info(">>>>>>>>>>> 作业端口 [{}] 已被占用。", port);
            used = true;
        }
        return used;
    }

}
