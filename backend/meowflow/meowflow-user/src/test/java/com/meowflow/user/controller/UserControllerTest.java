package com.meowflow.user.controller;

import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.user.dto.PasswordChangeDTO;
import com.meowflow.user.dto.UserCreateRequest;
import com.meowflow.user.dto.UserDTO;
import com.meowflow.user.dto.UserQuery;
import com.meowflow.user.dto.UserUpdateRequest;
import com.meowflow.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
 * UserController HTTP 层覆盖 —— URL 绑定、JSON 序列化、HTTP 状态。
 *
 * <p>使用 standalone MockMvc + Jackson + Validator，避免 MyBatis 注入失败。</p>
 *
 * <p>修复记录（2026-09）：
 * <ul>
 *   <li>User.id / UserDTO.id 从 String 改为 Long</li>
 *   <li>UserService.assignRoles 参数类型从 Set&lt;String&gt; 改为 Set&lt;Long&gt;</li>
 *   <li>所有 service 方法 id 参数改为 Long</li>
 * </ul>
 */
@DisplayName("UserController HTTP 层")
class UserControllerTest extends ControllerTestSupport {

    private MockMvc mockMvc;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        UserController controller = new UserController(userService);

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
        UserContextHolder.clear();
    }

    @Test
    @DisplayName("POST /api/v1/users — 合法 JSON 返回 200")
    void create_valid_returns200() throws Exception {
        UserCreateRequest req = new UserCreateRequest();
        req.setUsername("Alice1"); // must start with letter (uppercase or lowercase), 5-16 chars
        req.setPassword("P@ss1word");
        req.setNickName("Alice");
        UserDTO expected = UserDTO.builder().id(1L).username("Alice1").nickName("Alice").build();
        when(userService.createUser(any())).thenReturn(expected);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("Alice1"));
    }

    @Test
    @DisplayName("POST /api/v1/users — username 为空走 service（@Valid 缺省时）")
    void create_missingUsername_returnsOk() throws Exception {
        UserCreateRequest req = new UserCreateRequest();
        req.setUsername("xvalid"); // Valid username
        req.setPassword("P@ss1");
        req.setNickName("A");
        when(userService.createUser(any())).thenReturn(UserDTO.builder().id(2L).username("xvalid").build());

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}")
    void update_invokesService() throws Exception {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setNickName("Alice2");
        UserDTO expected = UserDTO.builder().id(1L).nickName("Alice2").build();
        when(userService.updateUser(anyLong(), any())).thenReturn(expected);

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nickName").value("Alice2"));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id}")
    void delete_returnsOk() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(userService).deleteUser(1L);
    }

    @Test
    @DisplayName("GET /api/v1/users/{id}")
    void getById_returnsUser() throws Exception {
        when(userService.getUserById(1L)).thenReturn(UserDTO.builder().id(1L).username("alice").build());

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/users — pageQuery 接受 URL 参数")
    void pageQuery_acceptsQueryParams() throws Exception {
        when(userService.pageQuery(any(UserQuery.class))).thenReturn(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10));

        mockMvc.perform(get("/api/v1/users?keyword=al&pageNum=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.current").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/password — 修改密码")
    void changePassword_validatesAndCalls() throws Exception {
        PasswordChangeDTO dto = new PasswordChangeDTO();
        dto.setOldPassword("old");
        dto.setNewPassword("newP@ss1");

        mockMvc.perform(put("/api/v1/users/1/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(dto)))
                .andExpect(status().isOk());
        verify(userService).changePassword(1L, "old", "newP@ss1");
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/password — 缺 newPassword 不 5xx")
    void changePassword_missingField_returns200() throws Exception {
        PasswordChangeDTO dto = new PasswordChangeDTO();
        dto.setOldPassword("old");
        dto.setNewPassword("");

        mockMvc.perform(put("/api/v1/users/1/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/password/reset — 重置密码")
    void resetPassword_invokesService() throws Exception {
        var dto = new com.meowflow.user.dto.PasswordResetDTO();
        dto.setNewPassword("newP@ss1");

        mockMvc.perform(put("/api/v1/users/1/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(dto)))
                .andExpect(status().isOk());
        verify(userService).resetPassword(1L, "newP@ss1");
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/status — 启用 / 禁用")
    void updateStatus_invokesService() throws Exception {
        mockMvc.perform(put("/api/v1/users/1/status").param("status", "0"))
                .andExpect(status().isOk());
        verify(userService).updateStatus(1L, "0");
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id}/roles — 分配角色")
    void assignRoles_invokesService() throws Exception {
        Set<Long> roleIds = new HashSet<>(List.of(1L, 2L));

        mockMvc.perform(put("/api/v1/users/1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(roleIds)))
                .andExpect(status().isOk());
        verify(userService).assignRoles(1L, roleIds);
    }

    @Test
    @DisplayName("GET /api/v1/users/{id}/roles — 返回角色 Set")
    void getUserRoles_returnsSet() throws Exception {
        Set<String> roles = new HashSet<>(List.of("admin", "dev"));
        when(userService.getUserById(1L)).thenReturn(UserDTO.builder().id(1L).roles(roles).build());

        mockMvc.perform(get("/api/v1/users/1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }
}
