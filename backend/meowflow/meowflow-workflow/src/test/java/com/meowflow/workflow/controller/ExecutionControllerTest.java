package com.meowflow.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.common.client.ExecutionLogResponse;
import com.meowflow.common.client.MonitorFeignClient;
import com.meowflow.common.result.Result;
import com.meowflow.workflow.dto.ExecutionRequest;
import com.meowflow.workflow.dto.ExecutionResponse;
import com.meowflow.workflow.dto.NodeExecutionDto;
import com.meowflow.workflow.dto.PageResponse;
import com.meowflow.workflow.service.ExecutionService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExecutionController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("ExecutionController HTTP 层")
class ExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ExecutionService executionService;
    @MockBean
    private MonitorFeignClient monitorFeignClient;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("POST /api/execution — 执行工作流")
    void execute_invokesService() throws Exception {
        ExecutionRequest req = new ExecutionRequest();
        req.setWorkflowId(1L);
        ExecutionResponse resp = new ExecutionResponse();
        resp.setExecutionId(100L);
        when(executionService.execute(any(), anyLong())).thenReturn(resp);

        mockMvc.perform(post("/api/execution")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.executionId").value(100));
    }

    @Test
    @DisplayName("POST /api/execution/{id}/cancel")
    void cancel_invokesService() throws Exception {
        mockMvc.perform(post("/api/execution/100/cancel"))
                .andExpect(status().isOk());
        verify(executionService).cancel(100L);
    }

    @Test
    @DisplayName("GET /api/execution/{id}")
    void getById_invokesService() throws Exception {
        ExecutionResponse resp = new ExecutionResponse();
        resp.setExecutionId(100L);
        when(executionService.getById(100L)).thenReturn(resp);

        mockMvc.perform(get("/api/execution/100"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/execution/page — 分页")
    void page_invokesService() throws Exception {
        PageResponse<ExecutionResponse> page = new PageResponse<>();
        page.setTotal(0L);
        when(executionService.page(any())).thenReturn(page);

        mockMvc.perform(get("/api/execution/page?pageNum=1&pageSize=10"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/execution/{id}/nodes")
    void nodes_invokesService() throws Exception {
        when(executionService.getNodeExecutions(100L)).thenReturn(List.of(new NodeExecutionDto()));

        mockMvc.perform(get("/api/execution/100/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/execution/{id}/logs — 全部")
    void logs_all_invokesRepo() throws Exception {
        when(monitorFeignClient.getExecutionLogs(100L)).thenReturn(Result.success(List.of()));

        mockMvc.perform(get("/api/execution/100/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
        verify(monitorFeignClient).getExecutionLogs(100L);
    }

    @Test
    @DisplayName("GET /api/execution/{id}/logs?nodeId=xxx — 按节点过滤")
    void logs_byNode_invokesRepo() throws Exception {
        when(monitorFeignClient.getNodeLogs(100L, "node-1")).thenReturn(Result.success(List.of()));

        mockMvc.perform(get("/api/execution/100/logs?nodeId=node-1"))
                .andExpect(status().isOk());
        verify(monitorFeignClient).getNodeLogs(100L, "node-1");
    }

    @Test
    @DisplayName("GET /api/execution/{id}/logs?level=error — 按级别过滤")
    void logs_byLevel_invokesRepo() throws Exception {
        when(monitorFeignClient.getExecutionLogs(100L)).thenReturn(Result.success(List.of()));

        mockMvc.perform(get("/api/execution/100/logs?level=error"))
                .andExpect(status().isOk());
        verify(monitorFeignClient).getExecutionLogs(100L);
    }

    @Test
    @DisplayName("GET /api/execution/workflow/{workflowId}")
    void listByWorkflow_invokesService() throws Exception {
        PageResponse<ExecutionResponse> page = new PageResponse<>();
        when(executionService.page(any())).thenReturn(page);

        mockMvc.perform(get("/api/execution/workflow/1?current=1&size=10"))
                .andExpect(status().isOk());
    }
}

