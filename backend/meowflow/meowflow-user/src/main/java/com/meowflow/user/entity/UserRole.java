package com.meowflow.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_sys_user_role")
public class UserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long roleId;
    private Long orgId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
