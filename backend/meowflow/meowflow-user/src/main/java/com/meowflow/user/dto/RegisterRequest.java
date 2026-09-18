package com.meowflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "注册请求")
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{4,15}$", message = "用户名以字母开头，长度5-16位，可包含下划线")
    @Schema(description = "用户名")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,20}$",
            message = "密码长度8-20位，必须包含字母和数字")
    @Schema(description = "密码")
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 20, message = "昵称最多20个字符")
    @Schema(description = "昵称")
    private String nickName;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号（可选）")
    private String phone;

    @Schema(description = "性别（可选）")
    private String sex;

    @NotBlank(message = "图形验证码不能为空")
    @Schema(description = "图形验证码答案")
    private String captchaCode;

    @NotBlank(message = "验证码UUID不能为空")
    @Schema(description = "图形验证码UUID")
    private String uuid;

    @NotBlank(message = "邮箱验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "邮箱验证码为6位数字")
    @Schema(description = "邮箱验证码")
    private String emailCode;

    @NotNull(message = "必须同意用户协议")
    @Schema(description = "是否同意用户协议")
    private Boolean agree;
}
