package com.meowflow.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_sys_permission")
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    @TableField("parent_id")
    private Long pid;

    private String path;

    @TableField(exist = false)
    private String component;

    @TableField(exist = false)
    private String componentName;

    @TableField("type")
    private String menuType;

    @TableField(exist = false)
    private String visible;

    private String status;

    @TableField("code")
    private String perms;

    @TableField(exist = false)
    private String permsType;

    private String icon;

    private Integer sort;

    @TableField(exist = false)
    private String createDept;

    @TableField("create_by")
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField("update_by")
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private String remark;

    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;
}
