package com.meowflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "权限信息响应")
public class PermissionDTO {

    @Schema(description = "权限ID")
    private Long id;

    @Schema(description = "权限名称")
    private String name;

    @Schema(description = "父权限ID")
    private Long pid;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "组件名称")
    private String componentName;

    @Schema(description = "权限类型 M-目录 C-菜单 F-按钮")
    private String type;

    @Schema(description = "是否显示")
    private String visible;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "权限标识")
    private String code;

    @Schema(description = "权限类型标识")
    private String permsType;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "子权限")
    private List<PermissionDTO> children;

    @Schema(description = "是否选中")
    private Boolean selected;

    @Schema(description = "是否禁用")
    private Boolean disabled;
}
