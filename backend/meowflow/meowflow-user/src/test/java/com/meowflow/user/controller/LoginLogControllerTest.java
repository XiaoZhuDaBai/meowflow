package com.meowflow.user.controller;

import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.user.dto.LoginLogQuery;
import com.meowflow.user.service.LoginLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LoginLogController HTTP 层覆盖。
 *
 * <p>使用 standalone MockMvc + Jackson + Validator。</p>
 */
@DisplayName("LoginLogController HTTP 层")
class LoginLogControllerTest extends ControllerTestSupport {

    private MockMvc mockMvc;
    private LoginLogService loginLogService;

    @BeforeEach
    void loginAdmin() {
        loginLogService = mock(LoginLogService.class);
        LoginLogController controller = new LoginLogController(loginLogService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(jacksonConverter())
                .setValidator(buildValidator())
                .build();

        SaTokenMockHelper.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        SaTokenMockHelper.clear();
    }

    @Test
    @DisplayName("GET /api/v1/login-logs — 分页")
    void page_invokesService() throws Exception {
        when(loginLogService.pageQuery(any(LoginLogQuery.class))).thenReturn(null);

        mockMvc.perform(get("/api/v1/login-logs?pageNum=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(loginLogService).pageQuery(any(LoginLogQuery.class));
    }

    @Test
    @DisplayName("GET /api/v1/login-logs/user/{username}")
    void byUser_invokesService() throws Exception {
        when(loginLogService.getLogsByUsername("alice")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/login-logs/user/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/login-logs/recent — 调用 service.getRecentLogs")
    void recent_invokesService() throws Exception {
        when(loginLogService.getRecentLogs(anyInt())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/login-logs/recent?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/login-logs/fail-count — 调用 service.countFailedLogins")
    void failCount_invokesService() throws Exception {
        when(loginLogService.countFailedLogins(anyString(), anyInt())).thenReturn(0);

        mockMvc.perform(get("/api/v1/login-logs/fail-count?username=alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("DELETE /api/v1/login-logs/clean — 调用 service.clean")
    void clean_invokesService() throws Exception {
        mockMvc.perform(delete("/api/v1/login-logs/clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(loginLogService).clean();
    }
}