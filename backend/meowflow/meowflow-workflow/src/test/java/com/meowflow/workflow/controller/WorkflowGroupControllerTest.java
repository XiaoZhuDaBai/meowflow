package com.meowflow.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.workflow.dto.GroupCreateRequest;
import com.meowflow.workflow.service.WorkflowGroupService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkflowGroupController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("WorkflowGroupController HTTP 层")
class WorkflowGroupControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private WorkflowGroupService groupService;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("POST /api/workflow/group")
    void create_invokesService() throws Exception {
        GroupCreateRequest req = new GroupCreateRequest();
        req.setName("我的分组");
        when(groupService.create(any(), anyLong())).thenReturn(new WorkflowGroupService.GroupResponse());

        mockMvc.perform(post("/api/workflow/group")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        verify(groupService).create(any(), anyLong());
    }

    @Test
    @DisplayName("PUT /api/workflow/group/{id}")
    void update_invokesService() throws Exception {
        when(groupService.update(any(), any(), anyLong())).thenReturn(new WorkflowGroupService.GroupResponse());

        mockMvc.perform(put("/api/workflow/group/1")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(new GroupCreateRequest())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/workflow/group/{id}")
    void delete_invokesService() throws Exception {
        mockMvc.perform(delete("/api/workflow/group/1"))
                .andExpect(status().isOk());
        verify(groupService).delete(any(), anyLong());
    }

    @Test
    @DisplayName("GET /api/workflow/group/{id}")
    void getById_invokesService() throws Exception {
        when(groupService.getById(any())).thenReturn(new WorkflowGroupService.GroupResponse());

        mockMvc.perform(get("/api/workflow/group/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/workflow/group/list")
    void list_invokesService() throws Exception {
        when(groupService.listByUser(anyLong())).thenReturn(List.of());

        mockMvc.perform(get("/api/workflow/group/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
