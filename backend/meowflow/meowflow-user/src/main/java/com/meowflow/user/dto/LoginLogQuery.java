package com.meowflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "登录日志查询参数")
public class LoginLogQuery {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "登录状态")
    private String status;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "页码")
    private Long pageNum = 1L;

    @Schema(description = "每页数量")
    private Long pageSize = 10L;
}
