package com.meowflow.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_sys_role")
public class Role implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String code;
    private Integer sort;
    private String dataScope;
    @TableField(exist = false)
    private Boolean menuCheckStrictly;

    @TableField(exist = false)
    private Boolean deptCheckStrictly;
    private String status;
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

    @TableField(exist = false)
    private Set<Permission> permissions = new HashSet<>();

    @TableField(exist = false)
    private Set<String> permissionIds = new HashSet<>();
}
