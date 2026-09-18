package com.meowflow.workflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页响应")
public class PageResponse<T> {

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "当前页")
    private Integer current;

    @Schema(description = "每页大小")
    private Integer size;

    @Schema(description = "总页数")
    private Integer pages;

    @Schema(description = "数据列表")
    private List<T> records;

    public static <T> PageResponse<T> of(List<T> records, Long total, Integer current, Integer size) {
        return PageResponse.<T>builder()
                .records(records)
                .total(total)
                .current(current)
                .size(size)
                .pages((int) Math.ceil((double) total / size))
                .build();
    }
}
