package com.meowflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户查询参数")
public class UserQuery {

    @Schema(description = "关键词(用户名/昵称/手机号)")
    private String keyword;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "组织ID")
    private Long orgId;

    @Schema(description = "角色ID")
    private String roleId;

    @Schema(description = "页码")
    private Long pageNum = 1L;

    @Schema(description = "每页数量")
    private Long pageSize = 10L;
}
