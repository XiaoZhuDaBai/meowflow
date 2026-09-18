package com.meowflow.common.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 全局配置。
 *
 * <p>注意：不要在此类中显式声明 {@code @Bean ObjectMapper}，否则会与 Spring Boot
 * 自动配置的 {@code jacksonObjectMapper} 同时被注册为 {@code @Primary}，导致
 * {@code NoUniqueBeanDefinitionException}。所有定制应通过
 * {@link Jackson2ObjectMapperBuilderCustomizer} 注入到 Spring Boot 的构建器里。</p>
 */
@Configuration
public class JacksonConfig {

    private static final String DATE_TIME = "yyyy-MM-dd HH:mm:ss";
    private static final String DATE = "yyyy-MM-dd";
    private static final String TIME = "HH:mm:ss";

    /**
     * 自定义 Jackson 构建器：注入到 Spring Boot 的 {@code Jackson2ObjectMapperBuilder}。
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            // 序列化时忽略 null 字段
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // Java 8 时间模块 + 自定义日期格式
            JavaTimeModule timeModule = new JavaTimeModule();
            timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME)));
            timeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DATE)));
            timeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(TIME)));
            timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME)));
            timeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DATE)));
            timeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(TIME)));
            builder.modulesToInstall(timeModule);

            // Long 类型序列化为 String（避免前端 JS 精度丢失）
            SimpleModule longModule = new SimpleModule();
            longModule.addSerializer(Long.class, ToStringSerializer.instance);
            longModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
            builder.modulesToInstall(longModule);
        };
    }
}