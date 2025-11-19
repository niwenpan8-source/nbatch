package com.nbatch.job.handler.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * @description: 数据源配置
 * @author: Mr.ni
 * @date: 2025/11/19
 */
@Data
@Component
@PropertySource("classpath:application.yml")
@ConfigurationProperties("spring.datasource")
public class DBBaseConfigConstant {

    public String url;

    public String username;

    public String password;

    public String driverClassName;

}
