package com.meowflow.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meowflow.common.result.Result;
import com.meowflow.user.dto.MenuTree;
import com.meowflow.user.dto.PermissionDTO;
import com.meowflow.user.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Tag(name = "权限管理", description = "菜单/按钮权限CRUD、用户权限查询等接口")
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "创建权限", description = "创建菜单或按钮权限")
    @PostMapping
    public Result<PermissionDTO> create(@RequestBody PermissionDTO dto) {
        PermissionDTO permission = permissionService.createPermission(dto);
        return Result.success(permission);
    }

    @Operation(summary = "更新权限", description = "更新菜单或按钮权限")
    @PutMapping("/{id}")
    public Result<PermissionDTO> update(
            @Parameter(description = "权限ID") @PathVariable Long id,
            @RequestBody PermissionDTO dto) {
        PermissionDTO permission = permissionService.updatePermission(id, dto);
        return Result.success(permission);
    }

    @Operation(summary = "删除权限", description = "删除菜单或按钮权限")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "权限ID") @PathVariable Long id) {
        permissionService.deletePermission(id);
        return Result.success();
    }

    @Operation(summary = "获取权限详情", description = "根据ID获取权限详细信息")
    @GetMapping("/{id}")
    public Result<PermissionDTO> getById(
            @Parameter(description = "权限ID") @PathVariable Long id) {
        PermissionDTO permission = permissionService.getPermissionById(id);
        return Result.success(permission);
    }

    @Operation(summary = "获取权限树", description = "获取完整的权限树")
    @GetMapping("/tree")
    public Result<List<PermissionDTO>> getTree() {
        List<PermissionDTO> tree = permissionService.getTree();
        return Result.success(tree);
    }

    @Operation(summary = "获取菜单树", description = "获取完整的菜单权限树")
    @GetMapping("/menu-tree")
    public Result<List<MenuTree>> getMenuTree() {
        List<MenuTree> menus = permissionService.getMenuTree();
        return Result.success(menus);
    }

    @Operation(summary = "获取菜单树", description = "获取完整的菜单树")
    @GetMapping("/menus")
    public Result<List<MenuTree>> getMenus() {
        List<MenuTree> menus = permissionService.getMenuTree();
        return Result.success(menus);
    }

    @Operation(summary = "获取用户菜单", description = "根据用户权限获取可访问的菜单树")
    @GetMapping("/user-menus")
    public Result<List<MenuTree>> getUserMenus(
            @Parameter(description = "用户ID") @RequestParam Long userId,
            @Parameter(description = "用户权限列表") @RequestParam(required = false) Set<String> permissions) {
        List<MenuTree> menus = permissionService.getUserMenus(userId, permissions);
        return Result.success(menus);
    }

    @Operation(summary = "获取用户按钮权限", description = "获取用户可操作的按钮权限标识")
    @GetMapping("/user-buttons")
    public Result<Set<String>> getUserButtonPerms(
            @Parameter(description = "用户权限列表") @RequestParam Set<String> permissions) {
        Set<String> buttons = permissionService.getUserButtonPerms(permissions);
        return Result.success(buttons);
    }

    @Operation(summary = "分页查询权限", description = "支持关键词、状态、类型筛选")
    @GetMapping
    public Result<IPage<PermissionDTO>> pageQuery(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "菜单类型") @RequestParam(required = false) String menuType,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Long pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Long pageSize) {
        IPage<PermissionDTO> page = permissionService.pageQuery(keyword, status, menuType, pageNum, pageSize)
                .convert(permissionService::convertToDTO);
        return Result.success(page);
    }

    @Operation(summary = "获取角色权限", description = "获取指定角色的所有权限列表")
    @GetMapping("/role/{roleId}")
    public Result<List<PermissionDTO>> getByRoleId(
            @Parameter(description = "角色ID") @PathVariable Long roleId) {
        List<PermissionDTO> permissions = permissionService.getPermissionsByRoleId(roleId);
        return Result.success(permissions);
    }

    @Operation(summary = "获取用户权限编码", description = "获取指定用户的所有权限编码")
    @GetMapping("/user/{userId}/codes")
    public Result<Set<String>> getUserPermissionCodes(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        Set<String> codes = permissionService.getPermissionsByUserId(userId);
        return Result.success(codes);
    }

    @Operation(summary = "获取当前用户权限", description = "获取当前登录用户的所有权限编码")
    @GetMapping("/user")
    public Result<Set<String>> getCurrentUserPermissions() {
        var ctx = com.meowflow.common.context.UserContextHolder.get();
        if (ctx == null || ctx.getPermissions() == null) {
            return Result.success(Set.of());
        }
        return Result.success(Set.copyOf(ctx.getPermissions()));
    }
}
