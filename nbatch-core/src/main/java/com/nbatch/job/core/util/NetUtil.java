package com.nbatch.job.core.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * net util
 *
 * @author Mr.ni 2017-11-29 17:00:25
 */
@Slf4j
public class NetUtil {

    /**
     * find avaliable port
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
     * check port used
     *
     * @param port  端口
     */
    public static boolean isPortUsed(int port) {
        boolean used;
        try (ServerSocket ignored = new ServerSocket(port)) {
            used = false;
        } catch (IOException e) {
            log.info(">>>>>>>>>>> job, port[{}] is in use.", port);
            used = true;
        }
        return used;
    }

}
