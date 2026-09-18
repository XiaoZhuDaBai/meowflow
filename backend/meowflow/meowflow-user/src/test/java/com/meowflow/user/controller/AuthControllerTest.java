package com.meowflow.user.controller;

import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.user.dto.LoginRequest;
import com.meowflow.user.dto.LoginResponse;
import com.meowflow.user.dto.RegisterRequest;
import com.meowflow.user.dto.ResetPasswordRequest;
import com.meowflow.user.dto.UserDTO;
import com.meowflow.user.dto.VerifyCodeRequest;
import com.meowflow.user.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController HTTP 层覆盖：URL 绑定 + @Valid 触发 + JSON shape。
 *
 * <p>LoginResponse 已改为嵌套 user 结构，测试验证 data.user 存在。</p>
 */
@DisplayName("AuthController HTTP 层")
class AuthControllerTest extends ControllerTestSupport {

    private MockMvc mockMvc;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        LocalValidatorFactoryBean validator = buildValidator();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(jacksonConverter())
                .setValidator(validator)
                .build();
        SaTokenMockHelper.loginAsAdmin();
    }

    @AfterEach
    void tearDown() {
        SaTokenMockHelper.clear();
    }

    @Test
    @DisplayName("POST /login - 成功返回 token（含嵌套 user）")
    void login_returnsToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("P@ssw0rd!");

        UserDTO userDTO = UserDTO.builder()
                .id(1L)
                .username("alice")
                .nickName("Alice")
                .email("alice@test.com")
                .roles(Set.of("member"))
                .permissions(Set.of("workflow:view"))
                .build();
        LoginResponse resp = LoginResponse.builder()
                .accessToken("access-xxx")
                .refreshToken("refresh-xxx")
                .tokenType("Bearer")
                .expiresIn(7200L)
                .user(userDTO)
                .build();
        when(authService.login(any(LoginRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("access-xxx"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-xxx"))
                .andExpect(jsonPath("$.data.user.username").value("alice"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("member"));
    }

    @Test
    @DisplayName("POST /login - 缺 password 也走 service")
    void login_missingPassword_returnsOk() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        LoginResponse resp = LoginResponse.builder()
                .accessToken("a")
                .refreshToken("r")
                .user(UserDTO.builder().id(1L).username("alice").build())
                .build();
        when(authService.login(any(LoginRequest.class))).thenReturn(resp);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /refresh - 返回新 token（含嵌套 user）")
    void refresh_returnsNewToken() throws Exception {
        UserDTO userDTO = UserDTO.builder()
                .id(1L)
                .username("alice")
                .nickName("Alice")
                .email("alice@test.com")
                .roles(Set.of("member"))
                .permissions(Set.of())
                .build();
        LoginResponse resp = LoginResponse.builder()
                .accessToken("rotated-access")
                .refreshToken("rotated-refresh")
                .tokenType("Bearer")
                .expiresIn(7200L)
                .user(userDTO)
                .build();
        when(authService.refreshToken("old-token")).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .param("refreshToken", "old-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("rotated-access"))
                .andExpect(jsonPath("$.data.user.username").value("alice"));
    }

    @Test
    @DisplayName("POST /logout - 调用 service.logout()")
    void logout_invokesService() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk());
        verify(authService).logout();
    }

    @Test
    @DisplayName("GET /me - 调用 service.getCurrentUser()")
    void me_returnsCurrentUser() throws Exception {
        UserDTO dto = UserDTO.builder()
                .id(1L)
                .username("admin")
                .nickName("Admin")
                .email("admin@test.com")
                .roles(Set.of("admin"))
                .permissions(Set.of("workflow:view"))
                .build();
        when(authService.getCurrentUser()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.roles[0]").value("admin"));
    }

    @Test
    @DisplayName("POST /register - 公开接口，字段含 emailCode/captchaCode")
    void register_returnsLogin() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newbie42");
        req.setPassword("Init@1234");
        req.setNickName("Newbie");
        req.setEmail("newbie@meow.local");
        req.setCaptchaCode("abcd");
        req.setUuid("captcha-uuid-1");
        req.setEmailCode("123456");
        req.setAgree(true);

        UserDTO userDTO = UserDTO.builder()
                .id(2L)
                .username("newbie42")
                .nickName("Newbie")
                .email("newbie@meow.local")
                .roles(Set.of("member"))
                .permissions(Set.of())
                .build();
        LoginResponse resp = LoginResponse.builder()
                .accessToken("access-newbie")
                .refreshToken("refresh-newbie")
                .tokenType("Bearer")
                .expiresIn(7200L)
                .user(userDTO)
                .build();
        when(authService.register(any(RegisterRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("access-newbie"))
                .andExpect(jsonPath("$.data.user.username").value("newbie42"));
    }

    @Test
    @DisplayName("POST /email-code - 公开接口")
    void sendEmailCode_invokesService() throws Exception {
        VerifyCodeRequest req = new VerifyCodeRequest();
        req.setEmail("alice@meow.local");
        req.setType("REGISTER");

        mockMvc.perform(post("/api/v1/auth/email-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(req)))
                .andExpect(status().isOk());
        verify(authService).sendEmailCode(any(VerifyCodeRequest.class));
    }

    @Test
    @DisplayName("POST /password/reset - 通过邮箱重置")
    void resetPassword_byEmail() throws Exception {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setEmail("alice@meow.local");
        req.setCode("123456");
        req.setNewPassword("NewP@ss1");

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MAPPER.writeValueAsString(req)))
                .andExpect(status().isOk());
        verify(authService).resetPasswordByEmail(any(ResetPasswordRequest.class));
    }
}
