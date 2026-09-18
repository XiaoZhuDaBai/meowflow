package com.meowflow.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "发布版本请求")
public class PublishVersionRequest {

    @NotBlank(message = "版本号不能为空")
    @Schema(description = "版本号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String version;

    @Schema(description = "变更日志")
    private String changelog;
}
