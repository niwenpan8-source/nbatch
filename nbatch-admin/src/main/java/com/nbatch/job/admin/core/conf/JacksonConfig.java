package com.nbatch.job.admin.core.conf;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

import static cn.hutool.core.date.DatePattern.NORM_DATETIME_PATTERN;
import static cn.hutool.core.date.DatePattern.NORM_DATE_PATTERN;
import static cn.hutool.core.date.DatePattern.NORM_TIME_PATTERN;

/**
 * Jackson的默认配置
 *
 * @author: zzy
 * @date: 2025-12-10
 */
@Configuration
public class JacksonConfig {

    private final static String TIME_ZONE = "GMT+8";



    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        // 创建并配置ObjectMapper实例
        // 配置项说明：
        // 1. 禁用FAIL_ON_UNKNOWN_PROPERTIES：反序列化时忽略未知属性，避免因JSON中存在额外字段而抛出异常
        // 2. 禁用FAIL_ON_EMPTY_BEANS：序列化空对象时不抛出异常，允许序列化没有getter方法的空对象
        // 3. 禁用AUTO_DETECT_GETTERS：禁用自动检测getter方法，只序列化显式标记的属性
        ObjectMapper mapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(MapperFeature.AUTO_DETECT_GETTERS, true)
                .build();

        // 创建 JavaTimeModule（用于支持 Java 8 时间类型）
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // ===== 序列化（Java → JSON）=====
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(NORM_DATETIME_PATTERN);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(NORM_DATE_PATTERN);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(NORM_TIME_PATTERN);

        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));

        // ===== 反序列化（JSON → Java）=====
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));


        // 注册模块
        mapper.registerModule(javaTimeModule);

        // 设置时区为 GMT+8
        mapper.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));

        // 禁用日期序列化为时间戳的功能，使日期以字符串格式输出而不是数字时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 启用数组序列化特性（通常默认开启）
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);


        // 设置JSON序列化时只包含非null值的属性
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return mapper;
    }
}