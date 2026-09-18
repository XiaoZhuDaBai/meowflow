package com.meowflow.user.controller;

import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.user.dto.RoleCreateRequest;
import com.meowflow.user.dto.RoleDTO;
import com.meowflow.user.service.RoleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
 * RoleController HTTP 层覆盖。
 *
 * <p>使用 standalone MockMvc + Jackson + Validator。</p>
 */
@DisplayName("RoleController HTTP 层")
class RoleControllerTest extends ControllerTestSupport {

    private MockMvc mockMvc;
    private RoleService roleService;

    @BeforeEach
    void loginAdmin() {
        roleService = mock(RoleService.class);
        RoleController controller = new RoleController(roleService);

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
    @DisplayName("POST /api/v1/roles — 创建角色")
    void create_returnsRole() throws Exception {
        RoleCreateRequest req = new RoleCreateRequest();
        req.setCode("admin");
        req.setName("管理员");
        when(roleService.createRole(any())).thenReturn(RoleDTO.builder().id(1L).code("admin").name("管理员").build());

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.code").value("admin"));
    }

    @Test
    @DisplayName("PUT /api/v1/roles/{id} — 更新角色")
    void update_invokesService() throws Exception {
        when(roleService.updateRole(eq(1L), any())).thenReturn(
                RoleDTO.builder().id(1L).name("管理员 v2").build());

        mockMvc.perform(put("/api/v1/roles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"管理员 v2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("管理员 v2"));
    }

    @Test
    @DisplayName("DELETE /api/v1/roles/{id}")
    void delete_invokesService() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(roleService).deleteRole(1L);
    }

    @Test
    @DisplayName("GET /api/v1/roles/{id}")
    void getById_invokesService() throws Exception {
        when(roleService.getRoleById(1L)).thenReturn(
                RoleDTO.builder().id(1L).name("管理员").build());

        mockMvc.perform(get("/api/v1/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/roles — 分页查询")
    void pageQuery_returnsPage() throws Exception {
        when(roleService.pageQuery(any(), any(), eq(1L), eq(10L))).thenReturn(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));

        mockMvc.perform(get("/api/v1/roles?keyword=ad&pageNum=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/roles/all — 全部角色")
    void all_returnsList() throws Exception {
        when(roleService.getAllRoles()).thenReturn(List.of(RoleDTO.builder().id(1L).build()));

        mockMvc.perform(get("/api/v1/roles/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/roles/{id}/permissions — 分配权限")
    void assignPermissions_invokesService() throws Exception {
        Set<Long> ids = Set.of(1L, 2L);

        mockMvc.perform(put("/api/v1/roles/1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(ids)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(roleService).assignPermissions(eq(1L), eq(ids));
    }

    @Test
    @DisplayName("GET /api/v1/roles/{id}/permissions — 取角色权限")
    void getRolePermissions_invokesService() throws Exception {
        when(roleService.getRolePermissions(1L)).thenReturn(Set.of(1L, 2L));

        mockMvc.perform(get("/api/v1/roles/1/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("PUT /api/v1/roles/{id}/menus — 分配菜单")
    void assignMenus_invokesService() throws Exception {
        Set<Long> menuIds = Set.of(1L);

        mockMvc.perform(put("/api/v1/roles/1/menus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(menuIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(roleService).assignPermissions(eq(1L), eq(menuIds));
    }

    @Test
    @DisplayName("GET /api/v1/roles/{id}/menus")
    void getRoleMenus_invokesService() throws Exception {
        when(roleService.getRolePermissions(1L)).thenReturn(Set.of(1L));

        mockMvc.perform(get("/api/v1/roles/1/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }
}