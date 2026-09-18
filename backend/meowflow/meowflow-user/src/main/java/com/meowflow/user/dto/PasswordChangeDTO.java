package com.meowflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "密码修改请求")
public class PasswordChangeDTO {

    @NotBlank(message = "旧密码不能为空")
    @Schema(description = "旧密码")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "密码长度6-20位")
    @Schema(description = "新密码")
    private String newPassword;
}
