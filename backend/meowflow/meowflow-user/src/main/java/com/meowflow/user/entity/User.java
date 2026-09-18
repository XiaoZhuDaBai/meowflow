package com.meowflow.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户实体。与 `mf_sys_user` 表字段对齐（驼峰 ↔ 下划线）。
 *  - 表列：login_ip / login_at / create_by / update_by / create_time / update_time
 *  - 实体：loginIp / loginAt / createBy / updateBy / createTime / updateTime
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_sys_user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    @TableField("nickname")
    private String nickName;
    private String email;
    private String phone;
    private String avatar;
    @TableField(exist = false)
    private String sex;
    private String status;
    @TableField("last_login_ip")
    private String loginIp;

    @TableField("last_login_at")
    private LocalDateTime loginAt;

    @TableField(exist = false)
    private Integer loginCount;
    private Long orgId;

    @TableField(exist = false)
    private Long postId;

    @TableField(exist = false)
    private String createDept;

    @TableField(value = "create_by")
    private Long createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_by")
    private Long updateBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String remark;

    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;

    @TableField(exist = false)
    private Set<String> roles = new HashSet<>();

    @TableField(exist = false)
    private Set<String> roleIds = new HashSet<>();

    @TableField(exist = false)
    private Set<String> permissions = new HashSet<>();

    @TableField(exist = false)
    private Set<Long> orgIds = new HashSet<>();
}
