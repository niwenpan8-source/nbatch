package com.nbatch.job.admin.core.util;

import com.nbatch.job.admin.core.conf.JobAdminConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * i18n util
 *
 * @author Mr.ni
 */
@Slf4j
public class I18nUtil {

    private static Properties prop = null;

    public static Properties loadI18nProp() {
        if (prop != null) {
            return prop;
        }
        try {
            // build i18n prop
            String i18n = JobAdminConfig.getAdminConfig().getI18n();
            String i18nFile = MessageFormat.format("i18n/message_{0}.properties", i18n);

            // load prop
            Resource resource = new ClassPathResource(i18nFile);
            EncodedResource encodedResource = new EncodedResource(resource, "UTF-8");
            prop = PropertiesLoaderUtils.loadProperties(encodedResource);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return prop;
    }

    /**
     * get val of i18n key
     *
     * @param key i18n key
     */
    public static String getString(String key) {
        return loadI18nProp().getProperty(key);
    }

    /**
     * get mult val of i18n mult key, as json,该方法用于前端取配置
     *
     * @param keys i18n keys
     */
    public static String getMultString(String... keys) {
        Map<String, String> map = new HashMap<>();

        Properties prop = loadI18nProp();
        if (keys!=null && keys.length>0) {
            for (String key: keys) {
                map.put(key, prop.getProperty(key));
            }
        } else {
            for (String key: prop.stringPropertyNames()) {
                map.put(key, prop.getProperty(key));
            }
        }

        return JacksonUtil.writeValueAsString(map);
    }


}
