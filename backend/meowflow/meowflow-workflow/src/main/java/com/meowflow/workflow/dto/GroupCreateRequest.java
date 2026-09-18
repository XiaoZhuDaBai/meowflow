package com.meowflow.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建分组请求")
public class GroupCreateRequest {

    @NotBlank(message = "分组名称不能为空")
    @Size(max = 64, message = "分组名称长度不能超过 64")
    @Schema(description = "分组名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "排序")
    private Integer sort;
}
