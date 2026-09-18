package com.meowflow.user.service;

import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.common.test.BaseIntegrationTest;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.user.dto.PasswordChangeDTO;
import com.meowflow.user.dto.UserCreateRequest;
import com.meowflow.user.dto.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UserService 真 DB + 真 Redis 集成测试。
 *
 * <p>每个 @Test 用 {@link JdbcTemplate} 直接清理相关表，保证顺序无关。
 * 集成测试依赖 docker-compose：{@code meowflow-postgres} + {@code meowflow-redis}。</p>
 *
 * <p>修复记录（2026-09）：
 * <ul>
 *   <li>User.id 从 String 改为 Long（数据库自增）</li>
 *   <li>userService.deleteUser 参数从 String 改为 Long</li>
 *   <li>UserDTO.id 类型同步更新</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("UserService 集成 — 创建 / 修改密码 / 启用禁用 全链路")
class UserApiIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void login() {
        SaTokenMockHelper.loginAsAdmin();
    }

    @AfterEach
    void logout() {
        SaTokenMockHelper.clear();
        UserContextHolder.clear();
    }

    private void cleanUser(String username) {
        jdbcTemplate.update("DELETE FROM mf_sys_user WHERE username = ?", username);
    }

    @Test
    @Order(1)
    @DisplayName("createUser — 新建用户，可立即通过 id 取回")
    void create_then_fetch() {
        try {
            UserCreateRequest req = new UserCreateRequest();
            req.setUsername("alice_it_1");
            req.setPassword("Init@123");
            req.setNickName("Alice IT");
            req.setEmail("alice1@meow.local");

            UserDTO created = userService.createUser(req);

            assertThat(created.getId()).isNotNull();
            assertThat(created.getUsername()).isEqualTo("alice_it_1");

            UserDTO fetched = userService.getUserById(created.getId());
            assertThat(fetched.getId()).isEqualTo(created.getId());
        } finally {
            cleanUser("alice_it_1");
        }
    }

    @Test
    @Order(2)
    @DisplayName("createUser — 重复 username 抛 USERNAME_EXISTS")
    void create_duplicateUsername_throws() {
        try {
            UserCreateRequest req = new UserCreateRequest();
            req.setUsername("alice_dup");
            req.setPassword("Init@123");
            req.setNickName("D");
            req.setEmail("dup@meow.local");

            userService.createUser(req);

            UserCreateRequest dup = new UserCreateRequest();
            dup.setUsername("alice_dup");
            dup.setPassword("Init@123");
            dup.setNickName("D2");
            dup.setEmail("dup2@meow.local");

            assertThatThrownBy(() -> userService.createUser(dup))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining(ResultCode.USERNAME_EXISTS.getMessage());
        } finally {
            cleanUser("alice_dup");
        }
    }

    @Test
    @Order(3)
    @DisplayName("changePassword — 旧密码正确则可改；旧密码错误抛 PASSWORD_ERROR")
    void changePassword_validatesOldPassword() {
        String username = "bob_pw_" + System.currentTimeMillis();
        try {
            UserCreateRequest req = new UserCreateRequest();
            req.setUsername(username);
            req.setPassword("Old@1234");
            req.setNickName("Bob");
            req.setEmail(username + "@meow.local");
            UserDTO u = userService.createUser(req);

            // 错误旧密码
            assertThatThrownBy(() -> userService.changePassword(u.getId(), "wrong", "New@1234"))
                    .isInstanceOf(BizException.class)
                    .hasMessageContaining(ResultCode.PASSWORD_ERROR.getMessage());

            // 正确旧密码
            userService.changePassword(u.getId(), "Old@1234", "New@5678");

            // 再用新密码登录式校验：changePassword 用错旧密码应失败
            assertThatThrownBy(() -> userService.changePassword(u.getId(), "Old@1234", "Any@9999"))
                    .isInstanceOf(BizException.class);
        } finally {
            cleanUser(username);
        }
    }

    @Test
    @Order(4)
    @DisplayName("updateStatus — 禁用 / 启用互转")
    void updateStatus_toggles() {
        String username = "carol_st_" + System.currentTimeMillis();
        try {
            UserCreateRequest req = new UserCreateRequest();
            req.setUsername(username);
            req.setPassword("Init@123");
            req.setNickName("Carol");
            req.setEmail(username + "@meow.local");
            UserDTO u = userService.createUser(req);

            userService.updateStatus(u.getId(), "0");
            UserDTO fetchedDisabled = userService.getUserById(u.getId());
            assertThat(fetchedDisabled.getStatus()).isEqualTo("0");

            userService.updateStatus(u.getId(), "1");
            UserDTO fetchedEnabled = userService.getUserById(u.getId());
            assertThat(fetchedEnabled.getStatus()).isEqualTo("1");
        } finally {
            cleanUser(username);
        }
    }

    @Test
    @Order(5)
    @DisplayName("deleteUser — 不存在 id 抛 USER_NOT_FOUND")
    void delete_missing_throws() {
        assertThatThrownBy(() -> userService.deleteUser(999999L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining(ResultCode.USER_NOT_FOUND.getMessage());
    }
}
