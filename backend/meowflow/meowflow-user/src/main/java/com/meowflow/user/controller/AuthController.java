package com.meowflow.user.controller;

import com.meowflow.common.result.Result;
import com.meowflow.user.dto.LoginRequest;
import com.meowflow.user.dto.LoginResponse;
import com.meowflow.user.dto.RegisterRequest;
import com.meowflow.user.dto.ResetPasswordRequest;
import com.meowflow.user.dto.UserDTO;
import com.meowflow.user.dto.VerifyCodeRequest;
import com.meowflow.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证管理", description = "用户登录、登出、Token刷新等认证接口")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录", description = "用户名密码登录，返回访问令牌和刷新令牌")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    @Operation(summary = "刷新Token", description = "使用刷新令牌获取新的访问令牌")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestParam String refreshToken) {
        LoginResponse response = authService.refreshToken(refreshToken);
        return Result.success(response);
    }

    @Operation(summary = "退出登录", description = "清除当前用户的登录状态")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @Operation(summary = "获取当前用户信息", description = "获取已登录用户的详细信息")
    @GetMapping("/me")
    public Result<UserDTO> getCurrentUser() {
        UserDTO user = authService.getCurrentUser();
        return Result.success(user);
    }

    @Operation(summary = "用户注册", description = "公开接口：填写用户名、密码、昵称、邮箱，通过邮箱验证码注册成功并自动登录")
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return Result.success(response);
    }

    @Operation(summary = "发送邮箱验证码", description = "公开接口：向指定邮箱发送验证码（用于注册或重置密码）")
    @PostMapping("/email-code")
    public Result<Void> sendEmailCode(@Valid @RequestBody VerifyCodeRequest request) {
        authService.sendEmailCode(request);
        return Result.success();
    }

    @Operation(summary = "通过邮箱重置密码", description = "公开接口：使用邮箱验证码重置密码")
    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPasswordByEmail(request);
        return Result.success();
    }
}
