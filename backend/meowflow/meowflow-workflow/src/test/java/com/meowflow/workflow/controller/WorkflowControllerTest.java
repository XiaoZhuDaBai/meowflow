package com.meowflow.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.workflow.dto.PageResponse;
import com.meowflow.workflow.dto.WorkflowCreateRequest;
import com.meowflow.workflow.dto.WorkflowDefinitionRequest;
import com.meowflow.workflow.dto.WorkflowResponse;
import com.meowflow.workflow.dto.WorkflowUpdateRequest;
import com.meowflow.workflow.dto.WorkflowVersionResponse;
import com.meowflow.workflow.dto.PublishVersionRequest;
import com.meowflow.workflow.repository.WorkflowRepository;
import com.meowflow.workflow.repository.WorkflowVersionRepository;
import com.meowflow.workflow.service.WorkflowImportExportService;
import com.meowflow.workflow.service.WorkflowService;
import com.meowflow.workflow.trigger.TriggerManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WorkflowController HTTP 层覆盖所有业务端点。
 */
@WebMvcTest(controllers = WorkflowController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("WorkflowController HTTP 层")
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private WorkflowService workflowService;
    @MockBean
    private WorkflowImportExportService importExportService;
    @MockBean
    private TriggerManager triggerManager;
    @MockBean
    private WorkflowRepository workflowRepository;
    @MockBean
    private WorkflowVersionRepository versionRepository;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("POST /api/workflow — 创建")
    void create_invokesService() throws Exception {
        WorkflowCreateRequest req = new WorkflowCreateRequest();
        req.setName("My Workflow");
        WorkflowResponse resp = new WorkflowResponse();
        resp.setId(1L);
        resp.setName("My Workflow");
        when(workflowService.create(any(), anyLong())).thenReturn(resp);

        mockMvc.perform(post("/api/workflow")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/workflow/{id} — 更新")
    void update_invokesService() throws Exception {
        WorkflowUpdateRequest req = new WorkflowUpdateRequest();
        req.setName("Updated");
        WorkflowResponse resp = new WorkflowResponse();
        resp.setId(1L);
        when(workflowService.update(anyLong(), any(), anyLong())).thenReturn(resp);

        mockMvc.perform(put("/api/workflow/1")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("DELETE /api/workflow/{id}")
    void delete_invokesService() throws Exception {
        mockMvc.perform(delete("/api/workflow/1"))
                .andExpect(status().isOk());
        verify(workflowService).delete(1L);
    }

    @Test
    @DisplayName("GET /api/workflow/{id}")
    void getById_invokesService() throws Exception {
        WorkflowResponse resp = new WorkflowResponse();
        resp.setId(1L);
        resp.setName("X");
        when(workflowService.getById(1L)).thenReturn(resp);

        mockMvc.perform(get("/api/workflow/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("GET /api/workflow/page")
    void page_invokesService() throws Exception {
        PageResponse<WorkflowResponse> page = new PageResponse<>();
        page.setTotal(0L);
        page.setRecords(List.of());
        when(workflowService.page(any())).thenReturn(page);

        mockMvc.perform(get("/api/workflow/page?pageNum=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("POST /api/workflow/{id}/versions — 保存版本")
    void saveVersion_invokesService() throws Exception {
        WorkflowDefinitionRequest req = new WorkflowDefinitionRequest();
        WorkflowVersionResponse resp = new WorkflowVersionResponse();
        resp.setId(1L);
        when(workflowService.saveVersion(anyLong(), any(), anyLong())).thenReturn(resp);

        mockMvc.perform(post("/api/workflow/1/versions")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/workflow/{id}/versions/publish")
    void publish_invokesService() throws Exception {
        PublishVersionRequest req = new PublishVersionRequest();
        req.setVersion("v1");
        WorkflowVersionResponse resp = new WorkflowVersionResponse();
        resp.setId(1L);
        when(workflowService.publishVersion(anyLong(), any(), anyLong())).thenReturn(resp);

        mockMvc.perform(post("/api/workflow/1/versions/publish")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/workflow/{id}/versions")
    void listVersions_invokesService() throws Exception {
        when(workflowService.listVersions(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/workflow/1/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("POST /api/workflow/{id}/stop — 停止调度")
    void stop_invokesTrigger() throws Exception {
        WorkflowResponse wf = new WorkflowResponse();
        wf.setId(1L);
        when(workflowService.getById(1L)).thenReturn(wf);

        mockMvc.perform(post("/api/workflow/1/stop"))
                .andExpect(status().isOk());

        verify(triggerManager).cancelAllTasks(1L);
    }

    @Test
    @DisplayName("POST /api/workflow/{id}/stop — 工作流不存在返回 404 code")
    void stop_notFound() throws Exception {
        when(workflowService.getById(99L)).thenReturn(null);

        mockMvc.perform(post("/api/workflow/99/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("POST /api/workflow/{id}/rollback/{version}")
    void rollback_invokesService() throws Exception {
        WorkflowResponse wf = new WorkflowResponse();
        wf.setId(1L);
        when(workflowService.getById(1L)).thenReturn(wf);
        WorkflowVersionResponse ver = new WorkflowVersionResponse();
        ver.setId(2L);
        when(workflowService.publishVersion(anyLong(), any(), anyLong())).thenReturn(ver);
        when(workflowService.listVersions(1L)).thenReturn(List.of(ver));

        mockMvc.perform(post("/api/workflow/1/rollback/v1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/workflow/{id}/export")
    void export_invokesService() throws Exception {
        when(importExportService.export(1L)).thenReturn("{...}");

        mockMvc.perform(get("/api/workflow/1/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("{...}"));
    }

    @Test
    @DisplayName("GET /api/workflow/{id}/export-with-history")
    void exportWithHistory_invokesService() throws Exception {
        when(importExportService.export(1L, true)).thenReturn("{...}");

        mockMvc.perform(get("/api/workflow/1/export-with-history"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/workflow/import")
    void import_invokesService() throws Exception {
        mockMvc.perform(post("/api/workflow/import")
                        .contentType("application/json")
                        .content("{\"json\":\"{...}\"}"))
                .andExpect(status().isOk());
        verify(importExportService).importFromJson(any(), org.mockito.ArgumentMatchers.eq(true), anyLong());
    }

    @Test
    @DisplayName("POST /api/workflow/{id}/import-version")
    void importVersion_invokesService() throws Exception {
        mockMvc.perform(post("/api/workflow/1/import-version")
                        .contentType("application/json")
                        .content("{\"json\":\"{...}\"}"))
                .andExpect(status().isOk());
        verify(importExportService).importFromJson(any(), org.mockito.ArgumentMatchers.eq(false), anyLong());
    }
}
