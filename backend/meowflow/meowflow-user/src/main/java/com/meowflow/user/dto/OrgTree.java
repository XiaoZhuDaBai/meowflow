package com.meowflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "组织树")
public class OrgTree {

    @Schema(description = "组织ID")
    private Long id;

    @Schema(description = "父组织ID")
    private Long parentId;

    @Schema(description = "组织名称")
    private String name;

    @Schema(description = "组织编码")
    private String code;

    @Schema(description = "负责人")
    private String leader;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "子组织")
    private List<OrgTree> children;
}
