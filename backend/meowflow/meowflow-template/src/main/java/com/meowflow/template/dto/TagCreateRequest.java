package com.meowflow.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建标签请求")
public class TagCreateRequest {

    @NotBlank(message = "标签名称不能为空")
    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "颜色")
    private String color;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;
}
