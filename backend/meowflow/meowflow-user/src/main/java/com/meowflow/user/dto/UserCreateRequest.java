package com.meowflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Set;

@Data
@Schema(description = "创建用户请求")
public class UserCreateRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{4,15}$", message = "用户名以字母开头，长度5-16位")
    @Schema(description = "用户名")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "密码长度6-20位")
    @Schema(description = "密码")
    private String password;

    @NotBlank(message = "昵称不能为空")
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

    @Schema(description = "岗位ID")
    private Long postId;

    @Schema(description = "角色ID列表")
    private Set<Long> roleIds;

    @Schema(description = "备注")
    private String remark;
}
