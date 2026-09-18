package com.meowflow.user.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.meowflow.common.result.Result;
import com.meowflow.user.dto.*;
import com.meowflow.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Tag(name = "用户管理", description = "用户CRUD、密码管理、角色分配等接口")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "创建用户", description = "创建新用户")
    @PostMapping
    public Result<UserDTO> create(@Valid @RequestBody UserCreateRequest request) {
        UserDTO user = userService.createUser(request);
        return Result.success(user);
    }

    @Operation(summary = "更新用户", description = "更新已有用户信息")
    @PutMapping("/{id}")
    public Result<UserDTO> update(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody UserUpdateRequest request) {
        UserDTO user = userService.updateUser(id, request);
        return Result.success(user);
    }

    @Operation(summary = "删除用户", description = "删除指定用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息")
    @GetMapping("/{id}")
    public Result<UserDTO> getById(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return Result.success(user);
    }

    @Operation(summary = "分页查询用户", description = "支持关键词搜索、状态筛选、组织筛选")
    @GetMapping
    public Result<IPage<UserDTO>> pageQuery(UserQuery query) {
        IPage<UserDTO> page = userService.pageQuery(query);
        return Result.success(page);
    }

    @Operation(summary = "修改密码", description = "用户修改自己的密码")
    @PutMapping("/{id}/password")
    public Result<Void> changePassword(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody PasswordChangeDTO dto) {
        userService.changePassword(id, dto.getOldPassword(), dto.getNewPassword());
        return Result.success();
    }

    @Operation(summary = "重置密码", description = "管理员重置用户密码")
    @PutMapping("/{id}/password/reset")
    public Result<Void> resetPassword(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Valid @RequestBody PasswordResetDTO dto) {
        userService.resetPassword(id, dto.getNewPassword());
        return Result.success();
    }

    @Operation(summary = "修改用户状态", description = "启用或禁用用户")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @Parameter(description = "状态 1-启用 0-禁用") @RequestParam String status) {
        userService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "分配角色", description = "为用户分配角色")
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody Set<Long> roleIds) {
        userService.assignRoles(id, roleIds);
        return Result.success();
    }

    @Operation(summary = "获取用户角色", description = "获取指定用户的角色列表")
    @GetMapping("/{id}/roles")
    public Result<Set<String>> getUserRoles(
            @Parameter(description = "用户ID") @PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return Result.success(user.getRoles());
    }
}
