package com.meowflow.user.service;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.user.dto.RoleCreateRequest;
import com.meowflow.user.entity.Permission;
import com.meowflow.user.entity.Role;
import com.meowflow.user.repository.PermissionRepository;
import com.meowflow.user.repository.RolePermissionRepository;
import com.meowflow.user.repository.RoleRepository;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoleServiceTest {

    private RoleRepository roleRepository;
    private RolePermissionRepository rolePermissionRepository;
    private PermissionRepository permissionRepository;

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleRepository = mock(RoleRepository.class);
        rolePermissionRepository = mock(RolePermissionRepository.class);
        permissionRepository = mock(PermissionRepository.class);

        roleService = new RoleService(
            roleRepository,
            rolePermissionRepository,
            permissionRepository
        );
    }

    @Test
    void createRole_shouldThrowExceptionWhenCodeExists() {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setName("Admin");
        request.setCode("existing-admin");

        when(roleRepository.existsByCode("existing-admin")).thenReturn(true);

        BizException exception = assertThrows(BizException.class, () -> {
            roleService.createRole(request);
        });

        assertEquals(ResultCode.DATA_ALREADY_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void getRoleById_shouldThrowExceptionWhenRoleNotFound() {
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        BizException exception = assertThrows(BizException.class, () -> {
            roleService.getRoleById(999L);
        });

        assertEquals(ResultCode.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void deleteRole_shouldThrowExceptionWhenRoleNotFound() {
        when(roleRepository.existsById(999L)).thenReturn(false);

        BizException exception = assertThrows(BizException.class, () -> {
            roleService.deleteRole(999L);
        });

        assertEquals(ResultCode.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void assignPermissions_shouldThrowExceptionWhenPermissionNotFound() {
        when(permissionRepository.existsById(999L)).thenReturn(false);

        BizException exception = assertThrows(BizException.class, () -> {
            roleService.assignPermissions(1L, Set.of(999L));
        });

        assertEquals(ResultCode.DATA_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void getRolePermissions_shouldReturnPermissionIds() {
        when(roleRepository.findPermissionIdsByRoleId(1L)).thenReturn(Set.of(1L, 2L, 3L));

        Set<Long> result = roleService.getRolePermissions(1L);

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains(1L));
        assertTrue(result.contains(2L));
        assertTrue(result.contains(3L));
    }

    private Role createTestRole() {
        Role role = new Role();
        role.setName("Test Role");
        role.setCode("test");
        role.setSort(1);
        role.setDataScope("1");
        role.setMenuCheckStrictly(true);
        role.setDeptCheckStrictly(true);
        role.setCreateTime(LocalDateTime.now());
        return role;
    }
}
