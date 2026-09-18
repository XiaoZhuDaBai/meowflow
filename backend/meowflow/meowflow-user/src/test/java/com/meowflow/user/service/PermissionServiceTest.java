package com.meowflow.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.exception.BizException;
import com.meowflow.user.dto.PermissionDTO;
import com.meowflow.user.entity.Permission;
import com.meowflow.user.repository.PermissionRepository;
import com.meowflow.user.test.BaseUserUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PermissionService Tests")
class PermissionServiceTest extends BaseUserUnitTest {

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    @BeforeEach
    void setUp() {
        UserContextHolder.set(com.meowflow.common.context.UserContext.builder()
                .userId(1L).username("admin").build());
    }

    @Test
    @DisplayName("createPermission - 成功创建")
    void createPermission_valid_persists() {
        PermissionDTO dto = PermissionDTO.builder().name("用户查看").code("user:read").type("MENU").build();

        when(permissionRepository.existsByCode("user:read")).thenReturn(false);

        PermissionDTO result = permissionService.createPermission(dto);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("user:read");
        verify(permissionRepository).insert(any(Permission.class));
    }

    @Test
    @DisplayName("createPermission - 编码重复抛异常")
    void createPermission_duplicateCode_throws() {
        PermissionDTO dto = PermissionDTO.builder().code("user:read").build();

        when(permissionRepository.existsByCode("user:read")).thenReturn(true);

        assertThatThrownBy(() -> permissionService.createPermission(dto))
                .isInstanceOf(BizException.class);
        verify(permissionRepository, never()).insert(any(Permission.class));
    }

    @Test
    @DisplayName("updatePermission - 成功更新")
    void updatePermission_existing_updatesFields() {
        Permission existing = new Permission();
        existing.setId(1L);
        existing.setName("Old Name");

        when(permissionRepository.findById(1L)).thenReturn(Optional.of(existing));

        PermissionDTO dto = PermissionDTO.builder().name("New Name").code("perm:new").build();

        PermissionDTO result = permissionService.updatePermission(1L, dto);

        assertThat(result).isNotNull();
        assertThat(existing.getName()).isEqualTo("New Name");
        assertThat(existing.getPerms()).isEqualTo("perm:new");
        verify(permissionRepository).updateById(existing);
    }

    @Test
    @DisplayName("updatePermission - 不存在抛异常")
    void updatePermission_notFound_throws() {
        when(permissionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> permissionService.updatePermission(999L, PermissionDTO.builder().build()))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("deletePermission - 存在则删除")
    void deletePermission_existing_deletes() {
        when(permissionRepository.existsById(1L)).thenReturn(true);
        when(permissionRepository.findByPid(1L)).thenReturn(List.of());

        permissionService.deletePermission(1L);

        verify(permissionRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deletePermission - 不存在抛异常")
    void deletePermission_notFound_throws() {
        when(permissionRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> permissionService.deletePermission(999L))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("getPermissionById - 返回存在的权限")
    void getPermissionById_existing_returnsPermission() {
        Permission existing = new Permission();
        existing.setId(1L);
        existing.setName("X");

        when(permissionRepository.findById(1L)).thenReturn(Optional.of(existing));

        PermissionDTO result = permissionService.getPermissionById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("X");
    }

    @Test
    @DisplayName("getPermissionById - 不存在抛异常")
    void getPermissionById_notFound_throws() {
        when(permissionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> permissionService.getPermissionById(999L))
                .isInstanceOf(BizException.class);
    }

    @Test
    @DisplayName("getTree - 返回树形结构")
    void getTree_returnsTree() {
        Permission parent = new Permission();
        parent.setId(1L);
        parent.setPid(0L);
        parent.setName("System");

        Permission child = new Permission();
        child.setId(2L);
        child.setPid(1L);
        child.setName("User");

        when(permissionRepository.selectList(null)).thenReturn(List.of(parent, child));

        List<PermissionDTO> tree = permissionService.getTree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getChildren()).hasSize(1);
    }

    @Test
    @DisplayName("getPermissionsByRoleId - 返回角色对应权限")
    void getPermissionsByRoleId_returnsList() {
        Permission p1 = new Permission();
        p1.setId(1L);
        p1.setName("View");
        when(permissionRepository.findByRoleId(1L)).thenReturn(List.of(p1));

        List<PermissionDTO> result = permissionService.getPermissionsByRoleId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("View");
    }

    @Test
    @DisplayName("getPermissionsByUserId - 返回用户对应权限")
    void getPermissionsByUserId_returnsSet() {
        when(permissionRepository.findCodesByUserId(1L)).thenReturn(Set.of("user:read"));

        Set<String> result = permissionService.getPermissionsByUserId(1L);

        assertThat(result).contains("user:read");
    }
}
