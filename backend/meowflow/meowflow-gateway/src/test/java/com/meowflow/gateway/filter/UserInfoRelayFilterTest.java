package com.meowflow.gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserInfoRelayFilter 的单元测试 —— 验证 SKIP_PATHS / X-User-Id 透传。
 *
 * <p>由于 Sa-Token 依赖全局 StpUtil 状态，本测试只覆盖过滤路径跳过的纯逻辑分支。
 */
@DisplayName("UserInfoRelayFilter 单测")
class UserInfoRelayFilterTest {

    private final UserInfoRelayFilter filter = new UserInfoRelayFilter();

    @Test
    @DisplayName("HEADER 常量定义保持稳定")
    void headerConstants_areStable() {
        assertThat(UserInfoRelayFilter.HEADER_USER_ID).isEqualTo("X-User-Id");
        assertThat(UserInfoRelayFilter.HEADER_USER_NAME).isEqualTo("X-User-Name");
        assertThat(UserInfoRelayFilter.HEADER_LOGIN_TOKEN).isEqualTo("X-Login-Token");
        assertThat(UserInfoRelayFilter.HEADER_TENANT_ID).isEqualTo("X-Tenant-Id");
    }

    @Test
    @DisplayName("公开接口按网关原始路径跳过用户透传")
    void shouldSkip_usesGatewayOriginalPaths() {
        assertThat(filter.shouldSkip("/user/api/v1/auth/login")).isTrue();
        assertThat(filter.shouldSkip("/user/api/v1/captcha/generate")).isTrue();
        assertThat(filter.shouldSkip("/workflow/api/webhook/demo")).isTrue();
        assertThat(filter.shouldSkip("/api/v1/auth/login")).isFalse();
        assertThat(filter.shouldSkip("/workflow/api/workflow/page")).isFalse();
    }

    @Test
    @DisplayName("getOrder — 高优先级（HIGHEST_PRECEDENCE+10）")
    void getOrder_isHighPrecedence() {
        assertThat(filter.getOrder())
                .isEqualTo(java.util.concurrent.ConcurrentSkipListMap.class.getName() == null
                        ? Integer.MIN_VALUE + 10
                        : org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 10);
    }
}
