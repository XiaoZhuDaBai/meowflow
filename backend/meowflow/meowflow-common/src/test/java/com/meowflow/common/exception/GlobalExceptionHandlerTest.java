package com.meowflow.common.exception;

import com.meowflow.common.result.ResultCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 {@link GlobalExceptionHandler} 正确把：
 * <ul>
 *   <li>{@link BizException} → 携带原始 code/message 的 Result.error</li>
 *   <li>{@link MethodArgumentNotValidException} → BAD_REQUEST (400)</li>
 * </ul>
 *
 * <p>通过 @WebMvcTest 启动 minimal Spring MVC slice，覆盖完整 advice 链路。
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.DummyController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.DummyController.class})
@ContextConfiguration(classes = {GlobalExceptionHandler.class, GlobalExceptionHandlerTest.DummyController.class})
@DisplayName("GlobalExceptionHandler WebMvc 切片")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("BizException — 200 ? 401：HTTP 应为 200，body.code 为 BizException.code")
    void bizException_returnsResultWithOriginalCode() throws Exception {
        mockMvc.perform(get("/dummy/biz-error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("资源不存在"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("未知 RuntimeException — 走兜底，返回 code=500")
    void fallback_returnsInternalError() throws Exception {
        mockMvc.perform(get("/dummy/runtime-error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("服务器内部错误")));
    }

    @Test
    @DisplayName("@Valid 失败 — 返回 BAD_REQUEST(400) 与拼装消息")
    void validationError_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/dummy/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("name 不能为空")));
    }

    // ============================================================
    // 辅助 controller —— 仅用于触发 GlobalExceptionHandler
    // ============================================================
    @org.springframework.web.bind.annotation.RestController
    @org.springframework.web.bind.annotation.RequestMapping("/dummy")
    static class DummyController {

        @org.springframework.web.bind.annotation.GetMapping("/biz-error")
        public void bizError() {
            throw new BizException(com.meowflow.common.result.ResultCode.NOT_FOUND, "资源不存在");
        }

        @org.springframework.web.bind.annotation.GetMapping("/runtime-error")
        public void runtimeError() {
            throw new RuntimeException("boom");
        }

        @org.springframework.web.bind.annotation.PostMapping("/validate")
        public String validate(@Valid @org.springframework.web.bind.annotation.RequestBody Req req) {
            return req.getName();
        }

        public static class Req {
            @NotBlank(message = "name 不能为空")
            private String name;

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
        }
    }
}
