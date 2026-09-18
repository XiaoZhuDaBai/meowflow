package com.meowflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "更新用户请求")
public class UserUpdateRequest {

    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{4,15}$", message = "用户名以字母开头，长度5-16位")
    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickName;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "性别")
    private String sex;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "组织ID")
    private Long orgId;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "岗位ID")
    private Long postId;

    @Schema(description = "角色ID列表")
    private Set<Long> roleIds;

    @Schema(description = "备注")
    private String remark;
}
