package com.meowflow.user.controller;

import com.meowflow.common.result.Result;
import com.meowflow.user.service.CaptchaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@Tag(name = "验证码", description = "图形验证码生成接口")
@RestController
@RequestMapping("/api/v1/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @Operation(summary = "生成验证码", description = "返回验证码图片的 base64 和 uuid，前端解码展示")
    @GetMapping(value = "/generate", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<CaptchaService.CaptchaVO> generate() throws IOException {
        CaptchaService.CaptchaVO vo = captchaService.generate();
        return Result.success(vo);
    }
}
