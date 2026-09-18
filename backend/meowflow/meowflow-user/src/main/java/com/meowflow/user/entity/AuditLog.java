package com.meowflow.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_sys_audit_log")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String module;
    private String method;
    private String requestMethod;
    private String requestUrl;
    private String requestParams;
    private String responseResult;
    private String ip;
    private Long costTime;
    private String status;
    private String errorMsg;
    private LocalDateTime operateTime;
}
