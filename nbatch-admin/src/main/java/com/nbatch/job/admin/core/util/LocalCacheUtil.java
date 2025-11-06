package com.nbatch.job.admin.core.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * local cache tool
 *
 * @author Mr.ni 2018-01-22 21:37:34
 */
public class LocalCacheUtil {

    // 类型建议用抽象父类，兼容性更好；
    private static final ConcurrentMap<String, LocalCacheData> CACHE_REPOSITORY = new ConcurrentHashMap<>();

    @Data
    private static class LocalCacheData {
        private String key;
        private Object val;
        private long timeoutTime;

        public LocalCacheData(String key, Object val, long timeoutTime) {
            this.key = key;
            this.val = val;
            this.timeoutTime = timeoutTime;
        }

    }


    /**
     * set cache
     *
     * @param key key
     * @param val value
     * @param cacheTime cacheTime
     */
    public static boolean set(String key, Object val, long cacheTime) {

        // clean timeout cache, before set new cache (avoid cache too much)
        cleanTimeoutCache();

        // set new cache
        if (StrUtil.isBlank(key)) {
            return false;
        }
        if (val == null) {
            remove(key);
        }
        if (cacheTime <= 0) {
            remove(key);
        }
        long timeoutTime = System.currentTimeMillis() + cacheTime;
        LocalCacheData localCacheData = new LocalCacheData(key, val, timeoutTime);
        CACHE_REPOSITORY.put(localCacheData.getKey(), localCacheData);
        return true;
    }

    /**
     * remove cache
     *
     * @param key cache key
     */
    public static boolean remove(String key) {
        if (StrUtil.isBlank(key)) {
            return false;
        }
        CACHE_REPOSITORY.remove(key);
        return true;
    }

    /**
     * get cache
     *
     * @param key cache key
     */
    public static Object get(String key) {
        if (StrUtil.isBlank(key)) {
            return null;
        }
        LocalCacheData localCacheData = CACHE_REPOSITORY.get(key);
        if (localCacheData != null && System.currentTimeMillis() < localCacheData.getTimeoutTime()) {
            return localCacheData.getVal();
        } else {
            remove(key);
            return null;
        }
    }

    /**
     * clean timeout cache
     */
    public static boolean cleanTimeoutCache() {
        if (CollUtil.isNotEmpty(CACHE_REPOSITORY)) {
            for (String key : CACHE_REPOSITORY.keySet()) {
                LocalCacheData localCacheData = CACHE_REPOSITORY.get(key);
                if (localCacheData != null && System.currentTimeMillis() >= localCacheData.getTimeoutTime()) {
                    CACHE_REPOSITORY.remove(key);
                }
            }
        }
        return true;
    }

}
