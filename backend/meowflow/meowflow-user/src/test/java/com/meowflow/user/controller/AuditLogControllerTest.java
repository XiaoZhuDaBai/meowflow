package com.meowflow.user.controller;

import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.user.dto.AuditLogDTO;
import com.meowflow.user.service.AuditLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuditLogController HTTP 层覆盖。
 *
 * <p>使用 standalone MockMvc + Jackson + Validator，避免 MyBatis 注入失败。</p>
 */
@DisplayName("AuditLogController HTTP 层")
class AuditLogControllerTest extends ControllerTestSupport {

    private MockMvc mockMvc;
    private AuditLogService auditLogService;

    @BeforeEach
    void loginAdmin() {
        auditLogService = mock(AuditLogService.class);
        AuditLogController controller = new AuditLogController(auditLogService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(jacksonConverter())
                .setValidator(buildValidator())
                .build();

        SaTokenMockHelper.loginAsAdmin();
    }

    @Test
    @DisplayName("GET /api/v1/audit-logs — 分页")
    void page_invokesService() throws Exception {
        when(auditLogService.pageQuery(any())).thenReturn(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));

        mockMvc.perform(get("/api/v1/audit-logs?keyword=login&pageNum=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(auditLogService).pageQuery(any());
    }

    @Test
    @DisplayName("GET /api/v1/audit-logs/{id}")
    void getById_invokesService() throws Exception {
        when(auditLogService.getById(1L)).thenReturn(
                AuditLogDTO.builder().id(1L).username("alice").build());

        mockMvc.perform(get("/api/v1/audit-logs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/audit-logs/user/{username}")
    void byUser_invokesService() throws Exception {
        when(auditLogService.getByUsername("alice")).thenReturn(List.of(
                AuditLogDTO.builder().id(1L).username("alice").build()));

        mockMvc.perform(get("/api/v1/audit-logs/user/alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }
}