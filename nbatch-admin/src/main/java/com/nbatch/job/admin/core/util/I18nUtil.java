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
 * 国际化工具类
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
            // 构建国际化属性文件路径
            String i18n = JobAdminConfig.getAdminConfig().getI18n();
            String i18nFile = MessageFormat.format("i18n/message_{0}.properties", i18n);

            // 加载属性文件
            Resource resource = new ClassPathResource(i18nFile);
            EncodedResource encodedResource = new EncodedResource(resource, "UTF-8");
            prop = PropertiesLoaderUtils.loadProperties(encodedResource);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return prop;
    }

    /**
     * 根据国际化键获取文本
     *
     * @param key i18n key
     */
    public static String getString(String key) {
        return loadI18nProp().getProperty(key);
    }

    /**
     * 根据国际化多语言键获取 JSON 文本，供前端读取配置
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
