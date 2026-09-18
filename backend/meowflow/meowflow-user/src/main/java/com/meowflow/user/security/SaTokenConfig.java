package com.meowflow.user.security;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.meowflow.common.context.UserContext;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 统一的 Sa-Token 拦截器：
 * 1. 校验登录（默认 StpUtil 空间 = access token）
 * 2. 登录态已建立后，将用户权限上下文写入 UserContextHolder
 *
 * <p>网关侧已对公开接口做白名单过滤，本层只对"鉴权失败"做兜底。</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SaTokenConfig implements WebMvcConfigurer {

    private final UserRepository userRepository;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            StpUtil.checkLogin();

            String userIdStr = StpUtil.getLoginIdAsString();
            if (userIdStr == null) {
                return;
            }
            if (UserContextHolder.get() != null) {
                return;
            }
            try {
                Long userId = Long.parseLong(userIdStr);
                userRepository.findById(userId).ifPresent(user -> {
                    var roles = userRepository.findRoleCodesByUserId(userId);
                    var perms = userRepository.findPermissionsByUserId(userId);
                    UserContext ctx = UserContext.builder()
                            .userId(user.getId())
                            .username(user.getUsername())
                            .nickname(user.getNickName())
                            .orgId(user.getOrgId())
                            .email(user.getEmail())
                            .roles(roles.stream().toList())
                            .permissions(perms.stream().toList())
                            .build();
                    UserContextHolder.set(ctx);
                });
            } catch (NumberFormatException ignore) {
                log.warn("非数字 loginId: {}", userIdStr);
            }
            // 防止sa-token的CSP检查告警
            SaHolder.getResponse();
        })).addPathPatterns("/**")
          .excludePathPatterns(
              "/api/v1/auth/login",
              "/api/v1/auth/refresh",
              "/api/v1/auth/register",
              "/api/v1/auth/email-code",
              "/api/v1/auth/password/reset",
              "/api/v1/captcha/**",
              "/doc.html",
              "/swagger-ui/**",
              "/swagger-ui.html",
              "/v3/api-docs/**",
              "/webjars/**",
              "/favicon.ico",
              "/error"
          );
    }
}
