package com.meowflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录日志响应")
public class LoginLogDTO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "登录IP")
    private String ip;

    @Schema(description = "登录地点")
    private String region;

    @Schema(description = "浏览器")
    private String userAgent;

    @Schema(description = "操作系统")
    private String os;

    @Schema(description = "登录状态")
    private String status;

    @Schema(description = "提示消息")
    private String message;

    @Schema(description = "登录时间")
    private LocalDateTime loginAt;
}
