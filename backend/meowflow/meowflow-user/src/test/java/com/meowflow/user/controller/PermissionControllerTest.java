package com.meowflow.user.controller;

import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.user.dto.MenuTree;
import com.meowflow.user.dto.PermissionDTO;
import com.meowflow.user.service.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
 * PermissionController HTTP 层覆盖。
 *
 * <p>使用 standalone MockMvc + Jackson + Validator。</p>
 */
@DisplayName("PermissionController HTTP 层")
class PermissionControllerTest extends ControllerTestSupport {

    private MockMvc mockMvc;
    private PermissionService permissionService;

    @BeforeEach
    void loginAdmin() {
        permissionService = mock(PermissionService.class);
        PermissionController controller = new PermissionController(permissionService);

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
    @DisplayName("POST /api/v1/permissions — 创建权限")
    void create_invokesService() throws Exception {
        PermissionDTO dto = new PermissionDTO();
        dto.setCode("user:list");
        when(permissionService.createPermission(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(permissionService).createPermission(any());
    }

    @Test
    @DisplayName("PUT /api/v1/permissions/{id} — 更新权限")
    void update_invokesService() throws Exception {
        mockMvc.perform(put("/api/v1/permissions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"菜单 1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(permissionService).updatePermission(eq(1L), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/permissions/{id}")
    void delete_invokesService() throws Exception {
        mockMvc.perform(delete("/api/v1/permissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(permissionService).deletePermission(1L);
    }

    @Test
    @DisplayName("GET /api/v1/permissions/{id}")
    void getById_invokesService() throws Exception {
        when(permissionService.getPermissionById(1L)).thenReturn(new PermissionDTO());

        mockMvc.perform(get("/api/v1/permissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/permissions/tree — 权限树")
    void tree_invokesService() throws Exception {
        when(permissionService.getTree()).thenReturn(List.of(new PermissionDTO()));

        mockMvc.perform(get("/api/v1/permissions/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/permissions/menu-tree — 菜单树")
    void menuTree_invokesService() throws Exception {
        when(permissionService.getMenuTree()).thenReturn(List.of(new MenuTree()));

        mockMvc.perform(get("/api/v1/permissions/menu-tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/permissions/menus — 别名")
    void menus_invokesService() throws Exception {
        when(permissionService.getMenuTree()).thenReturn(List.of(new MenuTree()));

        mockMvc.perform(get("/api/v1/permissions/menus"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/permissions/user-menus — 按 userId 过滤")
    void userMenus_invokesService() throws Exception {
        when(permissionService.getUserMenus(anyLong(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/permissions/user-menus").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/permissions/user-buttons")
    void userButtons_invokesService() throws Exception {
        when(permissionService.getUserButtonPerms(any())).thenReturn(Set.of("user:add"));

        mockMvc.perform(get("/api/v1/permissions/user-buttons")
                        .param("permissions", "user:add"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0]").value("user:add"));
    }

    @Test
    @DisplayName("GET /api/v1/permissions — 分页查询")
    void page_invokesService() throws Exception {
        when(permissionService.pageQuery(any(), any(), any(), anyLong(), anyLong())).thenReturn(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));

        mockMvc.perform(get("/api/v1/permissions?keyword=ad&pageNum=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/permissions/role/{roleId}")
    void byRole_invokesService() throws Exception {
        when(permissionService.getPermissionsByRoleId(1L)).thenReturn(List.of(new PermissionDTO()));

        mockMvc.perform(get("/api/v1/permissions/role/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("GET /api/v1/permissions/user/{userId}/codes")
    void userCodes_invokesService() throws Exception {
        when(permissionService.getPermissionsByUserId(1L)).thenReturn(Set.of("user:list"));

        mockMvc.perform(get("/api/v1/permissions/user/1/codes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0]").value("user:list"));
    }

    @Test
    @DisplayName("GET /api/v1/permissions/user — 当前用户")
    void currentUserCodes_invokesService() throws Exception {
        mockMvc.perform(get("/api/v1/permissions/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}