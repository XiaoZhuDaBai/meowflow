package com.meowflow.gateway.filter;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sa-Token 统一认证配置
 *
 * <p>在网关层集中校验 Token，下游微服务无需重复鉴权。</p>
 *
 * <p>白名单（addExclude）放行所有公开 / 认证类接口，避免登录 / 注册 / 验证码等被拦。</p>
 */
@Configuration
public class SaTokenConfig {

    static final String[] PUBLIC_PATHS = {
            // 用户模块：网关接收到的是带服务前缀的原始路径
            "/user/api/v1/auth/login",
            "/user/api/v1/auth/refresh",
            "/user/api/v1/auth/register",
            "/user/api/v1/auth/email-code",
            "/user/api/v1/auth/password/reset",
            "/user/api/v1/captcha/**",
            // Swagger / OpenAPI
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/webjars/**",
            "/doc.html",
            "/doc.html/**",
            "/favicon.ico",
            "/error",
            // 对外触发器
            "/workflow/api/webhook/**",
            "/workflow/api/plugin/trigger/**"
    };

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public SaReactorFilter saReactorFilter() {
        SaReactorFilter filter = new SaReactorFilter();
        filter.addInclude("/**")
              .addExclude(PUBLIC_PATHS);
        filter.setError(this::buildUnauthorizedResponse);
        return filter;
    }

    Object buildUnauthorizedResponse(Throwable throwable) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 401);
        body.put("message", "未授权: " + (throwable != null ? throwable.getMessage() : "请先登录"));
        body.put("data", null);
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            return body;
        }
    }
}
