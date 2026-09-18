package com.meowflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "菜单树")
public class MenuTree {

    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "父菜单ID")
    private Long pid;

    @Schema(description = "路由地址")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "组件名称")
    private String componentName;

    @Schema(description = "菜单类型 M-目录 C-菜单 F-按钮")
    private String menuType;

    @Schema(description = "是否显示")
    private String visible;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "权限标识")
    private String perms;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "子菜单")
    private List<MenuTree> children;

    @Schema(description = "是否选中")
    private Boolean selected;

    @Schema(description = "是否禁用")
    private Boolean disabled;
}
