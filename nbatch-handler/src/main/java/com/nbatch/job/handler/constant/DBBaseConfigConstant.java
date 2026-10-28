package com.nbatch.job.handler.constant;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * 数据源配置
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
