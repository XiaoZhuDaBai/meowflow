package com.meowflow.common.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserContextHolderTest {

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void setAndGet_shouldWorkCorrectly() {
        UserContext context = UserContext.builder()
            .userId(123L)
            .username("testuser")
            .nickname("测试用户")
            .orgId(1L)
            .build();

        UserContextHolder.set(context);
        UserContext retrieved = UserContextHolder.get();

        assertNotNull(retrieved);
        assertEquals(123L, retrieved.getUserId());
        assertEquals("testuser", retrieved.getUsername());
        assertEquals("测试用户", retrieved.getNickname());
        assertEquals(1L, retrieved.getOrgId());
    }

    @Test
    void get_shouldReturnNullWhenNotSet() {
        assertNull(UserContextHolder.get());
    }

    @Test
    void getUserId_shouldReturnNullWhenNotSet() {
        assertNull(UserContextHolder.getUserId());
    }

    @Test
    void getUsername_shouldReturnNullWhenNotSet() {
        assertNull(UserContextHolder.getUsername());
    }

    @Test
    void getOrgId_shouldReturnNullWhenNotSet() {
        assertNull(UserContextHolder.getOrgId());
    }

    @Test
    void getRoles_shouldReturnNullWhenNotSet() {
        assertNull(UserContextHolder.getRoles());
    }

    @Test
    void remove_shouldClearContext() {
        UserContext context = UserContext.builder()
            .userId(123L)
            .build();
        UserContextHolder.set(context);
        assertNotNull(UserContextHolder.get());

        UserContextHolder.remove();
        assertNull(UserContextHolder.get());
    }

    @Test
    void clear_shouldClearContext() {
        UserContext context = UserContext.builder()
            .userId(123L)
            .build();
        UserContextHolder.set(context);
        assertNotNull(UserContextHolder.get());

        UserContextHolder.clear();
        assertNull(UserContextHolder.get());
    }

    @Test
    void isAnonymous_shouldReturnTrueWhenNotSet() {
        assertTrue(UserContextHolder.isAnonymous());
    }

    @Test
    void isAnonymous_shouldReturnTrueWhenUserIdIsNull() {
        UserContext context = UserContext.builder().build();
        UserContextHolder.set(context);
        assertTrue(UserContextHolder.isAnonymous());
    }

    @Test
    void isAnonymous_shouldReturnFalseWhenUserIdIsSet() {
        UserContext context = UserContext.builder().userId(123L).build();
        UserContextHolder.set(context);
        assertFalse(UserContextHolder.isAnonymous());
    }

    @Test
    void isAdmin_shouldReturnFalseWhenNotSet() {
        assertFalse(UserContextHolder.isAdmin());
    }

    @Test
    void isAdmin_shouldReturnFalseWhenRolesIsNull() {
        UserContext context = UserContext.builder().userId(123L).build();
        UserContextHolder.set(context);
        assertFalse(UserContextHolder.isAdmin());
    }

    @Test
    void isAdmin_shouldReturnFalseWhenRolesDoesNotContainAdmin() {
        UserContext context = UserContext.builder()
            .userId(123L)
            .roles(List.of("user", "editor"))
            .build();
        UserContextHolder.set(context);
        assertFalse(UserContextHolder.isAdmin());
    }

    @Test
    void isAdmin_shouldReturnTrueWhenRolesContainsAdmin() {
        UserContext context = UserContext.builder()
            .userId(123L)
            .roles(List.of("admin", "user"))
            .build();
        UserContextHolder.set(context);
        assertTrue(UserContextHolder.isAdmin());
    }

    @Test
    void context_shouldSupportExtensions() {
        Map<String, Object> ext = Map.of("department", "IT", "level", 3);
        UserContext context = UserContext.builder()
            .userId(123L)
            .ext(ext)
            .build();

        UserContextHolder.set(context);
        UserContext retrieved = UserContextHolder.get();

        assertNotNull(retrieved.getExt());
        assertEquals("IT", retrieved.getExt().get("department"));
        assertEquals(3, retrieved.getExt().get("level"));
    }

    @Test
    void context_shouldSupportRolesAndPermissions() {
        List<String> roles = List.of("admin", "manager");
        List<String> permissions = List.of("user:read", "user:write", "workflow:execute");

        UserContext context = UserContext.builder()
            .userId(123L)
            .roles(roles)
            .permissions(permissions)
            .build();

        UserContextHolder.set(context);

        assertEquals(roles, UserContextHolder.getRoles());
        assertEquals(roles, UserContextHolder.get().getRoles());
        assertEquals(permissions, UserContextHolder.get().getPermissions());
    }
}