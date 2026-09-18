package com.meowflow.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_sys_role_permission")
public class RolePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long roleId;
    private Long permissionId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
