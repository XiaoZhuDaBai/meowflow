package com.meowflow.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新分组请求")
public class GroupUpdateRequest {

    @Size(max = 64, message = "分组名称长度不能超过 64")
    @Schema(description = "分组名称")
    private String name;

    @Schema(description = "排序")
    private Integer sort;
}
