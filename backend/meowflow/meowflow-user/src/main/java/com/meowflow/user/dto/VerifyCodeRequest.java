package com.meowflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "发送邮箱验证码请求")
public class VerifyCodeRequest {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱")
    private String email;

    @NotBlank(message = "验证码类型不能为空")
    @Pattern(regexp = "REGISTER|RESET_PWD", message = "类型仅支持 REGISTER 或 RESET_PWD")
    @Schema(description = "验证码类型：REGISTER=注册，RESET_PWD=重置密码")
    private String type;
}
