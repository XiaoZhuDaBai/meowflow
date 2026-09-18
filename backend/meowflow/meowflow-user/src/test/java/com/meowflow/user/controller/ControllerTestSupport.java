package com.meowflow.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

/**
 * 所有 Controller 独立测试的共同配置基类。
 *
 * <p>仅提供工具方法，不持有对 common 模块的静态引用（避免 Maven 多模块 reactor classpath 问题）。</p>
 */
abstract class ControllerTestSupport {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    protected static LocalValidatorFactoryBean buildValidator() {
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.afterPropertiesSet();
        return factory;
    }

    protected static MappingJackson2HttpMessageConverter jacksonConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(MAPPER);
        converter.setSupportedMediaTypes(List.of(
                org.springframework.http.MediaType.APPLICATION_JSON,
                org.springframework.http.MediaType.APPLICATION_JSON_UTF8
        ));
        return converter;
    }
}
