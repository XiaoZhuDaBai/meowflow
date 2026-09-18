package com.meowflow.user.service;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.user.dto.UserCreateRequest;
import com.meowflow.user.entity.User;
import com.meowflow.user.repository.RoleRepository;
import com.meowflow.user.repository.UserRepository;
import com.meowflow.user.repository.UserRoleRepository;
import com.meowflow.user.security.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UserService 单元测试。
 *
 * <p>修复记录（2026-09）：
 * <ul>
 *   <li>User.id 从 String 改为 Long（数据库自增）</li>
 *   <li>UserService 改用 @RequiredArgsConstructor，移除 IdGeneratorFactory</li>
 *   <li>assignRoles 参数类型从 Set&lt;String&gt; 改为 Set&lt;Long&gt;</li>
 *   <li>所有 service 方法 id 参数改为 Long</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    private UserRepository userRepository;
    private UserRoleRepository userRoleRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userRoleRepository = mock(UserRoleRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);

        userService = new UserService(
            userRepository,
            userRoleRepository,
            roleRepository,
            passwordEncoder
        );
    }

    @Test
    void createUser_shouldThrowExceptionWhenUsernameExists() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("existinguser");
        request.setPassword("password123");
        request.setNickName("Test User");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        BizException exception = assertThrows(BizException.class, () -> {
            userService.createUser(request);
        });

        assertEquals(ResultCode.USERNAME_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void getUserById_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        BizException exception = assertThrows(BizException.class, () -> {
            userService.getUserById(999L);
        });

        assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void deleteUser_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.existsById(999L)).thenReturn(false);

        BizException exception = assertThrows(BizException.class, () -> {
            userService.deleteUser(999L);
        });

        assertEquals(ResultCode.USER_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void changePassword_shouldThrowExceptionWhenOldPasswordIncorrect() {
        User user = createTestUser();
        user.setId(1L);
        user.setPassword("$2a$10$hashedPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "$2a$10$hashedPassword")).thenReturn(false);

        BizException exception = assertThrows(BizException.class, () -> {
            userService.changePassword(1L, "wrongPassword", "newPassword");
        });

        assertEquals(ResultCode.PASSWORD_ERROR.getCode(), exception.getCode());
    }

    @Test
    void assignRoles_shouldThrowExceptionWhenRoleNotFound() {
        when(roleRepository.existsById(999L)).thenReturn(false);

        BizException exception = assertThrows(BizException.class, () -> {
            userService.assignRoles(1L, Set.of(999L));
        });

        assertEquals(ResultCode.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    private User createTestUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setNickName("Test User");
        user.setStatus("1");
        user.setCreateTime(LocalDateTime.now());
        return user;
    }
}
