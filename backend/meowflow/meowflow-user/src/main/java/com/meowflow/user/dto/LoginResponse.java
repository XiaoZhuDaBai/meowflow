package com.meowflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录/刷新/注册成功后统一返回的认证响应")
public class LoginResponse {

    @Schema(description = "短期访问令牌（Sa-Token, 2 小时）")
    private String accessToken;

    @Schema(description = "独立刷新令牌（7 天）")
    private String refreshToken;

    @Schema(description = "令牌类型，固定 Bearer")
    private String tokenType;

    @Schema(description = "accessToken 过期秒数")
    private Long expiresIn;

    @Schema(description = "当前用户完整信息（含角色与权限）")
    private UserDTO user;
}
