package com.meowflow.gateway.filter;

import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 SaTokenConfig bean 定义不挂掉。
 */
@DisplayName("SaTokenConfig 装载")
class SaTokenConfigTest {

    @Test
    @DisplayName("saReactorFilter — bean 可以被 Spring 装配")
    void saReactorFilter_loads() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SaTokenConfig.class);
        try {
            SaReactorFilter bean = ctx.getBean(SaReactorFilter.class);
            assertThat(bean).isNotNull();
        } finally {
            ctx.close();
        }
    }

    @Test
    @DisplayName("公开认证路径包含网关服务前缀")
    void publicPaths_includeGatewayServicePrefix() {
        assertThat(Arrays.asList(SaTokenConfig.PUBLIC_PATHS)).contains(
                "/user/api/v1/auth/login",
                "/user/api/v1/auth/refresh",
                "/user/api/v1/auth/register",
                "/user/api/v1/captcha/**"
        );
    }

    @Test
    @DisplayName("未授权响应包含可序列化的 null data")
    void unauthorizedResponse_containsNullData() {
        Object response = new SaTokenConfig().buildUnauthorizedResponse(null);
        assertThat(response).isInstanceOf(String.class);
        assertThat((String) response)
                .contains("\"code\":401")
                .contains("\"data\":null");
    }
}
