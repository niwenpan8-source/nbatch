package com.nbatch.job.core.util;

import cn.hutool.core.text.StrPool;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * IP 地址工具类
 *
 * @author Mr.ni 2016-5-22 11:38:05
 */
@Slf4j
public class IpUtil {

    private static final String ANYHOST_VALUE = "0.0.0.0";
    private static final String LOCALHOST_VALUE = "127.0.0.1";
    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");

    private static volatile InetAddress LOCAL_ADDRESS = null;

    // ---------------------- valid ----------------------

    private static InetAddress toValidAddress(InetAddress address) {
        if (address instanceof Inet6Address) {
            Inet6Address v6Address = (Inet6Address) address;
            if (isPreferIPV6Address()) {
                return normalizeV6Address(v6Address);
            }
        }
        if (isValidV4Address(address)) {
            return address;
        }
        return null;
    }

    private static boolean isPreferIPV6Address() {
        return Boolean.getBoolean("java.net.preferIPv6Addresses");
    }

    /**
     * 校验 IPv4 地址
     *
     * @param address address
     */
    private static boolean isValidV4Address(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }
        String name = address.getHostAddress();
        return (name != null
                && IP_PATTERN.matcher(name).matches()
                && !ANYHOST_VALUE.equals(name)
                && !LOCALHOST_VALUE.equals(name));
    }


    /**
     * 规范化 IPv6 地址，将作用域名称转换为作用域 ID。
     * 示例：
     * 转换前
     * fe80:0:0:0:894:aeec:f37d:23e1%en0
     * 转换后
     * fe80:0:0:0:894:aeec:f37d:23e1%5
     * <p>
     * IPv6 地址后的 %5 称为作用域 ID，详细说明请参考 {@link Inet6Address}。
     *
     * @param address the input address
     * @return the normalized address, with scope id converted to int
     */
    private static InetAddress normalizeV6Address(Inet6Address address) {
        String addr = address.getHostAddress();
        int i = addr.lastIndexOf('%');
        if (i > 0) {
            try {
                return InetAddress.getByName(addr.substring(0, i) + '%' + address.getScopeId());
            } catch (UnknownHostException e) {
                // 忽略当前网卡
                log.debug("无法识别 IPv6 地址。", e);
            }
        }
        return address;
    }

    // ---------------------- find ip ----------------------


    private static InetAddress getLocalAddress0() {
        InetAddress localAddress = null;
        try {
            localAddress = InetAddress.getLocalHost();
            InetAddress addressItem = toValidAddress(localAddress);
            if (addressItem != null) {
                return addressItem;
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (null == interfaces) {
                return localAddress;
            }
            while (interfaces.hasMoreElements()) {
                try {
                    NetworkInterface network = interfaces.nextElement();
                    if (network.isLoopback() || network.isVirtual() || !network.isUp()) {
                        continue;
                    }
                    Enumeration<InetAddress> addresses = network.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        try {
                            InetAddress addressItem = toValidAddress(addresses.nextElement());
                            if (addressItem != null) {
                                try {
                                    if (addressItem.isReachable(100)) {
                                        return addressItem;
                                    }
                                } catch (IOException e) {
                                    // 忽略当前地址
                                }
                            }
                        } catch (Throwable e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                } catch (Throwable e) {
                    log.error(e.getMessage(), e);
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
        return localAddress;
    }


    // ---------------------- tool ----------------------

    /**
     * 从本地网卡中查找第一个有效 IP 地址
     *
     * @return first valid local IP
     */
    public static InetAddress getLocalAddress() {
        if (LOCAL_ADDRESS != null) {
            return LOCAL_ADDRESS;
        }
        InetAddress localAddress = getLocalAddress0();
        LOCAL_ADDRESS = localAddress;
        return localAddress;
    }

    /**
     * 获取 IP 地址
     *
     * @return String
     */
    public static String getIp() {
        return getLocalAddress().getHostAddress();
    }

    /**
     * 获取 IP 和端口
     *
     * @param port 端口
     * @return String
     */
    public static String getIpPort(int port) {
        String ip = getIp();
        return getIpPort(ip, port);
    }

    public static String getIpPort(String ip, int port) {
        if (ip == null) {
            return null;
        }
        return ip.concat(StrPool.COLON).concat(String.valueOf(port));
    }

    public static Object[] parseIpPort(String address) {
        String[] array = address.split(StrPool.COLON);

        String host = array[0];
        int port = Integer.parseInt(array[1]);

        return new Object[]{host, port};
    }


}
