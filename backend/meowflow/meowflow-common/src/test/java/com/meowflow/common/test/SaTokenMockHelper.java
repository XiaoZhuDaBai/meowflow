package com.meowflow.common.test;

import com.meowflow.common.context.UserContext;
import com.meowflow.common.context.UserContextHolder;

import java.util.List;

/**
 * 在 MockMvc / 集成测试中模拟"已登录"上下文。所有方法成对出现：begin() / end()。
 */
public final class SaTokenMockHelper {

    private SaTokenMockHelper() {
    }

    /**
     * 模拟一个系统管理员登录：userId=1, username=admin, role=admin
     */
    public static void loginAsAdmin() {
        UserContext ctx = UserContext.builder()
                .userId(1L)
                .username("admin")
                .nickname("Admin")
                .orgId(1L)
                .roles(List.of("admin"))
                .permissions(List.of("*"))
                .build();
        UserContextHolder.set(ctx);
    }

    /**
     * 模拟指定 userId 的普通用户登录
     */
    public static void loginAs(String userId, String username, List<String> roles) {
        Long uid = Long.parseLong(userId);
        UserContext ctx = UserContext.builder()
                .userId(uid)
                .username(username)
                .nickname(username)
                .orgId(uid)
                .roles(roles)
                .build();
        UserContextHolder.set(ctx);
    }

    /**
     * 清除上下文
     */
    public static void clear() {
        UserContextHolder.clear();
    }
}
