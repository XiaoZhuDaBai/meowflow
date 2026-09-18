package com.meowflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户信息响应")
public class UserDTO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "性别")
    private String sex;

    @Schema(description = "状态: active / disabled")
    private String status;

    @Schema(description = "组织ID")
    private Long orgId;

    @Schema(description = "组织名称")
    private String orgName;

    @Schema(description = "岗位ID")
    private Long postId;

    @Schema(description = "岗位名称")
    private String postName;

    @Schema(description = "最后登录IP")
    private String loginIp;

    @Schema(description = "最后登录时间")
    private LocalDateTime loginAt;

    @Schema(description = "登录次数")
    private Integer loginCount;

    @Schema(description = "角色列表")
    private Set<String> roles;

    @Schema(description = "权限列表")
    private Set<String> permissions;

    @Schema(description = "用户所属组织ID集合（个人组织 + 加入的团队）")
    private Set<Long> orgIds;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "备注")
    private String remark;
}
