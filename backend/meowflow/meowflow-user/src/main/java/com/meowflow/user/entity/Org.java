package com.meowflow.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mf_sys_org")
public class Org implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;

    @TableField(exist = false)
    private String ancestors;

    private String name;

    private String code;

    @TableField(exist = false)
    private String leader;

    @TableField("leader_user_id")
    private Long leaderUserId;

    private String phone;

    private String email;

    private Integer sort;

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
}
