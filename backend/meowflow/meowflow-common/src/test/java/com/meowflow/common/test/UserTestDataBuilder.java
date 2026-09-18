package com.meowflow.common.test;

import com.meowflow.common.context.UserContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 测试数据构造工具 - User 相关实体。
 */
public final class UserTestDataBuilder {

    private UserTestDataBuilder() {
    }

    public static UserContext createUserContext() {
        return createUserContext(1L, "alice");
    }

    public static UserContext createUserContext(Long userId, String username) {
        return UserContext.builder()
                .userId(userId)
                .username(username)
                .nickname("Alice")
                .orgId(1L)
                .email("alice@example.com")
                .roles(List.of("user", "admin"))
                .permissions(List.of("workflow:read", "workflow:write"))
                .build();
    }

    public static UserContext createUserContextWithRoles(Long userId, String username, List<String> roles) {
        return UserContext.builder()
                .userId(userId)
                .username(username)
                .nickname(username)
                .orgId(1L)
                .roles(roles)
                .build();
    }

    public static Set<String> permissionSet(String... perms) {
        Set<String> set = new HashSet<>();
        for (String p : perms) {
            set.add(p);
        }
        return set;
    }
}