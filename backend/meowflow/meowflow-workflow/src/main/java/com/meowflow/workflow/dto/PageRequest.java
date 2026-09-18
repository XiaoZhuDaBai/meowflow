package com.meowflow.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "分页请求")
public class PageRequest {

    @Schema(description = "当前页码", example = "1")
    private Integer current = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;

    @Schema(description = "排序字段")
    private String sort;

    @Schema(description = "排序方向")
    private String order;

    public int getOffset() {
        return (current - 1) * size;
    }
}
