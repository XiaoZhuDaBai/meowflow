package com.meowflow.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meowflow.common.result.Result;
import com.meowflow.user.dto.RoleCreateRequest;
import com.meowflow.user.dto.RoleDTO;
import com.meowflow.user.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Tag(name = "角色管理", description = "角色CRUD、权限分配等接口")
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "创建角色", description = "创建新角色")
    @PostMapping
    public Result<RoleDTO> create(@Valid @RequestBody RoleCreateRequest request) {
        RoleDTO role = roleService.createRole(request);
        return Result.success(role);
    }

    @Operation(summary = "更新角色", description = "更新已有角色信息")
    @PutMapping("/{id}")
    public Result<RoleDTO> update(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @RequestBody RoleCreateRequest request) {
        RoleDTO role = roleService.updateRole(id, request);
        return Result.success(role);
    }

    @Operation(summary = "删除角色", description = "删除指定角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "角色ID") @PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }

    @Operation(summary = "获取角色详情", description = "根据ID获取角色详细信息")
    @GetMapping("/{id}")
    public Result<RoleDTO> getById(
            @Parameter(description = "角色ID") @PathVariable Long id) {
        RoleDTO role = roleService.getRoleById(id);
        return Result.success(role);
    }

    @Operation(summary = "分页查询角色", description = "支持关键词搜索、状态筛选")
    @GetMapping
    public Result<IPage<RoleDTO>> pageQuery(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Long pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Long pageSize) {
        IPage<RoleDTO> page = roleService.pageQuery(keyword, status, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "获取所有角色", description = "获取角色下拉列表")
    @GetMapping("/all")
    public Result<List<RoleDTO>> getAllRoles() {
        List<RoleDTO> roles = roleService.getAllRoles();
        return Result.success(roles);
    }

    @Operation(summary = "分配权限", description = "为角色分配菜单/按钮权限")
    @PutMapping("/{id}/permissions")
    public Result<Void> assignPermissions(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @RequestBody Set<Long> permissionIds) {
        roleService.assignPermissions(id, permissionIds);
        return Result.success();
    }

    @Operation(summary = "获取角色权限", description = "获取指定角色的权限ID列表")
    @GetMapping("/{id}/permissions")
    public Result<Set<Long>> getRolePermissions(
            @Parameter(description = "角色ID") @PathVariable Long id) {
        Set<Long> permissions = roleService.getRolePermissions(id);
        return Result.success(permissions);
    }

    @Operation(summary = "分配菜单权限", description = "为角色分配菜单/按钮权限")
    @PutMapping("/{id}/menus")
    public Result<Void> assignMenus(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @RequestBody Set<Long> menuIds) {
        roleService.assignPermissions(id, menuIds);
        return Result.success();
    }

    @Operation(summary = "获取角色菜单权限", description = "获取指定角色的菜单权限列表")
    @GetMapping("/{id}/menus")
    public Result<Set<Long>> getRoleMenus(
            @Parameter(description = "角色ID") @PathVariable Long id) {
        Set<Long> menus = roleService.getRolePermissions(id);
        return Result.success(menus);
    }
}
