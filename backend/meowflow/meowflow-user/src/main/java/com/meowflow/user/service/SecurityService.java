package com.meowflow.user.service;

import com.meowflow.common.context.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 安全工具服务，提供权限和角色校验能力。
 * <p>
 * 注册为 Spring Bean，名称 "ss"，用于支持
 * {@code @PreAuthorize("@ss.hasPermi('xxx')")} 等注解式权限校验。
 */
@Slf4j
@Component("ss")
public class SecurityService {

    private final PermissionService permissionService;

    public SecurityService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 检查当前用户是否拥有指定权限标识。
     *
     * @param permission 权限标识，如 "system:user:list"
     * @return true 表示有权限
     */
    public boolean hasPermi(String permission) {
        Set<String> userPermissions = getCurrentPermissions();
        return userPermissions.contains(permission);
    }

    /**
     * 检查当前用户是否拥有指定权限标识。
     */
    public boolean hasPermission(String permission) {
        return hasPermi(permission);
    }

    /**
     * 检查当前用户是否拥有任意一个指定权限。
     *
     * @param permissions 权限标识数组
     * @return true 表示至少拥有其中之一
     */
    public boolean hasAnyPermi(String... permissions) {
        Set<String> userPermissions = getCurrentPermissions();
        for (String permission : permissions) {
            if (userPermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查当前用户是否拥有所有指定权限。
     *
     * @param permissions 权限标识数组
     * @return true 表示全部拥有
     */
    public boolean hasAllPermi(String... permissions) {
        Set<String> userPermissions = getCurrentPermissions();
        for (String permission : permissions) {
            if (!userPermissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查当前用户是否具有指定角色。
     *
     * @param role 角色编码，如 "admin"
     * @return true 表示拥有该角色
     */
    public boolean hasRole(String role) {
        List<String> userRoles = UserContextHolder.getRoles();
        return userRoles != null && userRoles.contains(role);
    }

    /**
     * 检查当前用户是否具有任意一个指定角色。
     *
     * @param roles 角色编码数组
     * @return true 表示至少拥有其中之一
     */
    public boolean hasAnyRole(String... roles) {
        List<String> userRoles = UserContextHolder.getRoles();
        if (userRoles == null) {
            return false;
        }
        for (String role : roles) {
            if (userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查当前用户是否为超级管理员。
     */
    public boolean isAdmin() {
        return hasRole("admin");
    }

    /**
     * 检查当前用户是否拥有全部数据权限。
     */
    public boolean isDataScopeAll() {
        return isAdmin();
    }

    /**
     * 检查当前用户是否拥有本部门数据权限。
     */
    public boolean isDataScopeDept() {
        return hasRole("data_scope_dept");
    }

    /**
     * 获取当前用户的组织 ID。
     */
    public String getDataScopeOrgId() {
        Long orgId = UserContextHolder.getOrgId();
        return orgId == null ? null : String.valueOf(orgId);
    }

    /**
     * 获取当前用户的权限集合。
     */
    private Set<String> getCurrentPermissions() {
        List<String> perms = UserContextHolder.get() != null
                ? UserContextHolder.get().getPermissions()
                : null;
        if (perms == null || perms.isEmpty()) {
            return Collections.emptySet();
        }
        return perms.stream().collect(Collectors.toSet());
    }
}
