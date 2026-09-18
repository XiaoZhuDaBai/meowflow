package com.meowflow.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志。与 `mf_sys_login_log` 表字段对齐（PascalCase 字段 ↔ snake_case 列）。
 *  - 表列：user_id / username / ip / user_agent / region / status / message / login_at
 *  - 实体：userId / username / ip / userAgent / region / status / message / loginAt
 */
@Data
@TableName("mf_sys_login_log")
public class LoginLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String username;
    private String ip;
    private String userAgent;
    private String region;
    private String status;
    private String message;
    private LocalDateTime loginAt;
}
