package com.nbatch.job.core.enums;

/**
 * 注册中心配置
 */
public class RegistryConfig {

    public static final int BEAT_TIMEOUT = 5;
    public static final int DEAD_TIMEOUT = BEAT_TIMEOUT * 5;

    public enum RegistType{ EXECUTOR, ADMIN }

}
