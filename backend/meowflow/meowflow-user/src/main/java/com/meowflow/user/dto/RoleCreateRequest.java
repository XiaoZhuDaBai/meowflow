package com.meowflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建角色请求")
public class RoleCreateRequest {

    @NotBlank(message = "角色名称不能为空")
    @Schema(description = "角色名称")
    private String name;

    @NotBlank(message = "角色编码不能为空")
    @Schema(description = "角色编码")
    private String code;

    @Schema(description = "排序")
    private Integer roleSort;

    @Schema(description = "数据权限范围")
    private String dataScope;

    @Schema(description = "菜单树严格模式")
    private Boolean menuCheckStrictly;

    @Schema(description = "部门树严格模式")
    private Boolean deptCheckStrictly;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "权限ID列表")
    private java.util.Set<Long> permissionIds;

    @Schema(description = "备注")
    private String remark;
}
