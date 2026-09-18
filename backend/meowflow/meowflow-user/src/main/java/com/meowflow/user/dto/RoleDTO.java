package com.meowflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@Schema(description = "角色信息响应")
public class RoleDTO {

    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色编码")
    private String code;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "数据权限范围")
    private String dataScope;

    @Schema(description = "菜单树严格模式")
    private Boolean menuCheckStrictly;

    @Schema(description = "部门树严格模式")
    private Boolean deptCheckStrictly;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "权限ID列表")
    private Set<String> permissionIds;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "备注")
    private String remark;
}
