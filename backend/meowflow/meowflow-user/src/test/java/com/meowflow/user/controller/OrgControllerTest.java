package com.meowflow.user.controller;

import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.user.entity.Org;
import com.meowflow.user.dto.OrgTree;
import com.meowflow.user.service.OrgService;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * OrgController HTTP 层覆盖。
 *
 * <p>使用 standalone MockMvc + Jackson + Validator。</p>
 */
@DisplayName("OrgController HTTP 层")
class OrgControllerTest extends ControllerTestSupport {

    private MockMvc mockMvc;
    private OrgService orgService;

    @BeforeEach
    void loginAdmin() {
        orgService = mock(OrgService.class);
        OrgController controller = new OrgController(orgService);

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
    @DisplayName("POST /api/v1/orgs — 创建组织")
    void create_invokesService() throws Exception {
        Org org = new Org();
        org.setName("技术部");
        org.setCode("tech");
        when(orgService.createOrg(any())).thenReturn(new OrgTree());

        mockMvc.perform(post("/api/v1/orgs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(org)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(orgService).createOrg(any());
    }

    @Test
    @DisplayName("PUT /api/v1/orgs/{id}")
    void update_invokesService() throws Exception {
        when(orgService.updateOrg(anyLong(), any())).thenReturn(new OrgTree());

        mockMvc.perform(put("/api/v1/orgs/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"产品部\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(orgService).updateOrg(anyLong(), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/orgs/{id}")
    void delete_invokesService() throws Exception {
        mockMvc.perform(delete("/api/v1/orgs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(orgService).deleteOrg(1L);
    }

    @Test
    @DisplayName("GET /api/v1/orgs/{id}")
    void getById_invokesService() throws Exception {
        when(orgService.getOrgById(1L)).thenReturn(new OrgTree());

        mockMvc.perform(get("/api/v1/orgs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/orgs/tree")
    void tree_invokesService() throws Exception {
        when(orgService.getOrgTree()).thenReturn(List.of(new OrgTree()));

        mockMvc.perform(get("/api/v1/orgs/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0]").exists());
    }

    @Test
    @DisplayName("GET /api/v1/orgs/{id}/children")
    void children_invokesService() throws Exception {
        when(orgService.getChildren(1L)).thenReturn(List.of(new Org()));

        mockMvc.perform(get("/api/v1/orgs/1/children"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/orgs — 分页")
    void page_invokesService() throws Exception {
        when(orgService.pageQuery(any(), any(), anyLong(), anyLong())).thenReturn(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));

        mockMvc.perform(get("/api/v1/orgs?keyword=tech&pageNum=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}