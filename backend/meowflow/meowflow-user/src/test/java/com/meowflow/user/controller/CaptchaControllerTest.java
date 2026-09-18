package com.meowflow.user.controller;

import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.user.service.CaptchaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CaptchaController HTTP 层覆盖。
 *
 * <p>使用 standalone MockMvc。</p>
 */
@DisplayName("CaptchaController HTTP 层")
class CaptchaControllerTest extends ControllerTestSupport {

    private MockMvc mockMvc;
    private CaptchaService captchaService;

    @BeforeEach
    void loginAdmin() {
        captchaService = mock(CaptchaService.class);
        CaptchaController controller = new CaptchaController(captchaService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(jacksonConverter())
                .build();

        SaTokenMockHelper.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        SaTokenMockHelper.clear();
    }

    @Test
    @DisplayName("GET /api/v1/captcha/generate — 调用 service.generate()")
    void generate_invokesService() throws Exception {
        when(captchaService.generate()).thenReturn(
                new CaptchaService.CaptchaVO("uuid-1", "base64-img"));

        mockMvc.perform(get("/api/v1/captcha/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.uuid").value("uuid-1"));

        verify(captchaService).generate();
    }
}