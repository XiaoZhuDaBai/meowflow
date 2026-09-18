package com.meowflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "审计日志响应")
public class AuditLogDTO {

    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "操作用户")
    private String username;

    @Schema(description = "操作模块")
    private String module;

    @Schema(description = "操作方法")
    private String method;

    @Schema(description = "请求方式")
    private String requestMethod;

    @Schema(description = "请求URL")
    private String requestUrl;

    @Schema(description = "请求参数")
    private String requestParams;

    @Schema(description = "响应结果")
    private String responseResult;

    @Schema(description = "操作IP")
    private String ip;

    @Schema(description = "消耗时间(毫秒)")
    private Long costTime;

    @Schema(description = "操作状态")
    private String status;

    @Schema(description = "错误消息")
    private String errorMsg;

    @Schema(description = "操作时间")
    private LocalDateTime operateTime;
}
