package com.meowflow.user.aspectj;

import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.test.BaseIntegrationTest;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.user.dto.RoleCreateRequest;
import com.meowflow.user.dto.UserCreateRequest;
import com.meowflow.user.dto.UserDTO;
import com.meowflow.user.repository.RoleRepository;
import com.meowflow.user.service.RoleService;
import com.meowflow.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RBAC 链路集成：建两个角色 + 两个用户 → 校验每个用户的 permission 集合正确隔离。
 *
 * <p>修复记录（2026-09）：
 * <ul>
 *   <li>Role.id 从 String 改为 Long（数据库自增）</li>
 *   <li>RoleService.createRole 返回 RoleDTO（含 auto-generated id）</li>
 *   <li>userService.assignRoles 参数从 Set&lt;String&gt; 改为 Set&lt;Long&gt;</li>
 *   <li>userService.getUsersByRoleId 参数从 String 改为 Long</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("RBAC 授权链路集成")
class RBACAuthorizationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JdbcTemplate jdbc;

    /** 捕获创建的角色 ID（由数据库 auto-generate） */
    private static final String ADMIN_ROLE_CODE = "rbac_admin_" + System.currentTimeMillis();
    private static final String GUEST_ROLE_CODE = "rbac_guest_" + System.currentTimeMillis();

    private static Long roleAdminId;
    private static Long roleGuestId;

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
        jdbc.update("DELETE FROM mf_sys_user_role WHERE user_id IN (SELECT id FROM mf_sys_user WHERE username = ?)", username);
        jdbc.update("DELETE FROM mf_sys_user WHERE username = ?", username);
    }

    private void cleanRoleByCode(String code) {
        jdbc.update("DELETE FROM mf_sys_role WHERE code = ?", code);
    }

    @Test
    @Order(1)
    @DisplayName("setup — 建两个角色（admin / guest），记录 auto-generated id")
    void setupRoles() {
        cleanRoleByCode(ADMIN_ROLE_CODE);
        cleanRoleByCode(GUEST_ROLE_CODE);

        RoleCreateRequest adminReq = new RoleCreateRequest();
        adminReq.setName("RBAC 管理员");
        adminReq.setCode(ADMIN_ROLE_CODE);
        adminReq.setStatus("active");
        var adminDto = roleService.createRole(adminReq);
        roleAdminId = Long.parseLong(String.valueOf(adminDto.getId()));

        RoleCreateRequest guestReq = new RoleCreateRequest();
        guestReq.setName("RBAC 访客");
        guestReq.setCode(GUEST_ROLE_CODE);
        guestReq.setStatus("active");
        var guestDto = roleService.createRole(guestReq);
        roleGuestId = Long.parseLong(String.valueOf(guestDto.getId()));

        assertThat(roleAdminId).isNotNull();
        assertThat(roleGuestId).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("建两个用户，分别绑不同角色")
    void setupUsers() {
        String adminUser = "rbac_admin_user";
        String guestUser = "rbac_guest_user";
        cleanUser(adminUser);
        cleanUser(guestUser);

        UserCreateRequest adminReq = new UserCreateRequest();
        adminReq.setUsername(adminUser);
        adminReq.setPassword("Init@123");
        adminReq.setNickName("RBAC Admin");
        adminReq.setEmail(adminUser + "@meow.local");
        adminReq.setRoleIds(new HashSet<>(List.of(roleAdminId)));
        userService.createUser(adminReq);

        UserCreateRequest guestReq = new UserCreateRequest();
        guestReq.setUsername(guestUser);
        guestReq.setPassword("Init@123");
        guestReq.setNickName("RBAC Guest");
        guestReq.setEmail(guestUser + "@meow.local");
        guestReq.setRoleIds(new HashSet<>(List.of(roleGuestId)));
        userService.createUser(guestReq);
    }

    @Test
    @Order(3)
    @DisplayName("admin 应能查到角色码 admin，guest 应能查到 guest")
    void rolesAreIsolated() {
        List<UserDTO> adminList = userService.getUsersByRoleId(roleAdminId);
        assertThat(adminList).isNotEmpty();
        UserDTO admin = userService.getUserById(adminList.get(0).getId());
        assertThat(admin.getRoles()).contains(ADMIN_ROLE_CODE);

        List<UserDTO> guestList = userService.getUsersByRoleId(roleGuestId);
        assertThat(guestList).isNotEmpty();
        UserDTO guest = userService.getUserById(guestList.get(0).getId());
        assertThat(guest.getRoles()).contains(GUEST_ROLE_CODE);
    }

    @Test
    @Order(4)
    @DisplayName("assignRoles — 改分配后 user.roles 正确反映")
    void assignRoles_replaces() {
        List<UserDTO> adminList = userService.getUsersByRoleId(roleAdminId);
        assertThat(adminList).isNotEmpty();
        var adminUser = adminList.get(0);

        // 把 admin 用户也加 guest 角色
        Set<Long> both = new HashSet<>(List.of(roleAdminId, roleGuestId));
        userService.assignRoles(adminUser.getId(), both);

        UserDTO fetched = userService.getUserById(adminUser.getId());
        assertThat(fetched.getRoles()).contains(ADMIN_ROLE_CODE, GUEST_ROLE_CODE);
    }
}




