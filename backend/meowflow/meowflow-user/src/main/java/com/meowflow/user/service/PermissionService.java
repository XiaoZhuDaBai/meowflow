package com.meowflow.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.user.dto.MenuTree;
import com.meowflow.user.dto.PermissionDTO;
import com.meowflow.user.entity.Permission;
import com.meowflow.user.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    private static final Set<String> MENU_TYPES = Set.of("M", "C");

    @Transactional
    public PermissionDTO createPermission(PermissionDTO dto) {
        if (permissionRepository.existsByCode(dto.getCode())) {
            throw new BizException(ResultCode.DATA_ALREADY_EXISTS, "权限编码已存在");
        }

        Permission permission = new Permission();
        permission.setName(dto.getName());
        permission.setPid(dto.getPid() != null ? dto.getPid() : 0L);
        permission.setPath(dto.getPath());
        permission.setComponent(dto.getComponent());
        permission.setComponentName(dto.getComponentName());
        permission.setMenuType(dto.getType());
        permission.setVisible(StringUtils.hasText(dto.getVisible()) ? dto.getVisible() : "1");
        permission.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "active");
        permission.setPerms(dto.getCode());
        permission.setPermsType(dto.getPermsType());
        permission.setIcon(dto.getIcon());
        permission.setSort(dto.getSort());
        permission.setRemark(dto.getRemark());
        permission.setCreateBy(null);
        permission.setCreateTime(LocalDateTime.now());

        permissionRepository.insert(permission);

        return convertToDTO(permission);
    }

    @Transactional
    public PermissionDTO updatePermission(Long permissionId, PermissionDTO dto) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new BizException(ResultCode.DATA_NOT_FOUND, "权限不存在"));

        if (StringUtils.hasText(dto.getName())) {
            permission.setName(dto.getName());
        }
        if (dto.getPid() != null) {
            permission.setPid(dto.getPid());
        }
        if (StringUtils.hasText(dto.getPath())) {
            permission.setPath(dto.getPath());
        }
        if (StringUtils.hasText(dto.getComponent())) {
            permission.setComponent(dto.getComponent());
        }
        if (StringUtils.hasText(dto.getComponentName())) {
            permission.setComponentName(dto.getComponentName());
        }
        if (StringUtils.hasText(dto.getType())) {
            permission.setMenuType(dto.getType());
        }
        if (StringUtils.hasText(dto.getVisible())) {
            permission.setVisible(dto.getVisible());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            permission.setStatus(dto.getStatus());
        }
        if (StringUtils.hasText(dto.getCode())) {
            permission.setPerms(dto.getCode());
        }
        if (StringUtils.hasText(dto.getPermsType())) {
            permission.setPermsType(dto.getPermsType());
        }
        if (StringUtils.hasText(dto.getIcon())) {
            permission.setIcon(dto.getIcon());
        }
        if (dto.getSort() != null) {
            permission.setSort(dto.getSort());
        }
        if (StringUtils.hasText(dto.getRemark())) {
            permission.setRemark(dto.getRemark());
        }

        permission.setUpdateBy(null);
        permission.setUpdateTime(LocalDateTime.now());

        permissionRepository.updateById(permission);

        return convertToDTO(permission);
    }

    @Transactional
    public void deletePermission(Long permissionId) {
        if (!permissionRepository.existsById(permissionId)) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "权限不存在");
        }

        List<Permission> children = permissionRepository.findByPid(permissionId);
        if (!children.isEmpty()) {
            throw new BizException(ResultCode.BIZ_ERROR, "请先删除子菜单");
        }

        permissionRepository.deleteById(permissionId);
    }

    public PermissionDTO getPermissionById(Long permissionId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new BizException(ResultCode.DATA_NOT_FOUND, "权限不存在"));
        return convertToDTO(permission);
    }

    public List<PermissionDTO> getTree() {
        List<Permission> all = permissionRepository.selectList(null);
        return buildTree(all);
    }

    public List<MenuTree> getMenuTree() {
        List<Permission> all = permissionRepository.findAllMenus();
        return buildMenuTree(all);
    }

    public List<PermissionDTO> getPermissionsByRoleId(Long roleId) {
        List<Permission> permissions = permissionRepository.findByRoleId(roleId);
        return permissions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Set<String> getPermissionsByUserId(Long userId) {
        return permissionRepository.findCodesByUserId(userId);
    }

    public List<MenuTree> getUserMenus(Long userId, Set<String> userPermissions) {
        List<Permission> menus = permissionRepository.findByMenuTypesAndStatusAndPermsIn(
                MENU_TYPES, "active", userPermissions);
        return buildMenuTree(menus);
    }

    public List<MenuTree> getAllMenus() {
        List<Permission> menus = permissionRepository.findAllMenus();
        return buildMenuTree(menus);
    }

    public Set<String> getUserButtonPerms(Set<String> userPermissions) {
        List<Permission> buttons = permissionRepository.findByMenuTypesAndStatusAndPermsIn(
                Set.of("F"), "active", userPermissions);
        return buttons.stream()
                .map(Permission::getPerms)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public IPage<Permission> pageQuery(String keyword, String status, String menuType,
                                       Long pageNum, Long pageSize) {
        Page<Permission> page = new Page<>(pageNum, pageSize);
        return permissionRepository.pageQuery(page, keyword, status, menuType);
    }

    public PermissionDTO convertToDTO(Permission permission) {
        return PermissionDTO.builder()
                .id(permission.getId())
                .name(permission.getName())
                .pid(permission.getPid())
                .path(permission.getPath())
                .component(permission.getComponent())
                .componentName(permission.getComponentName())
                .type(permission.getMenuType())
                .visible(permission.getVisible())
                .status(permission.getStatus())
                .code(permission.getPerms())
                .permsType(permission.getPermsType())
                .icon(permission.getIcon())
                .sort(permission.getSort())
                .createTime(permission.getCreateTime())
                .remark(permission.getRemark())
                .build();
    }

    private List<PermissionDTO> buildTree(List<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<Permission>> grouped = permissions.stream()
                .collect(Collectors.groupingBy(p -> p.getPid() != null ? p.getPid() : 0L));

        List<PermissionDTO> result = new ArrayList<>();
        for (Permission p : permissions) {
            Long pid = p.getPid() != null ? p.getPid() : 0L;
            if (pid == 0L) {
                PermissionDTO dto = convertToDTO(p);
                dto.setChildren(buildChildren(p.getId(), grouped));
                result.add(dto);
            }
        }
        result.sort(Comparator.comparing(PermissionDTO::getSort, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    private List<PermissionDTO> buildChildren(Long pid, Map<Long, List<Permission>> grouped) {
        List<Permission> children = grouped.get(pid);
        if (children == null || children.isEmpty()) {
            return Collections.emptyList();
        }
        return children.stream()
                .map(p -> {
                    PermissionDTO dto = convertToDTO(p);
                    dto.setChildren(buildChildren(p.getId(), grouped));
                    return dto;
                })
                .sorted(Comparator.comparing(PermissionDTO::getSort, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    private List<MenuTree> buildMenuTree(List<Permission> menus) {
        if (menus == null || menus.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, List<Permission>> grouped = menus.stream()
                .collect(Collectors.groupingBy(p -> p.getPid() != null ? p.getPid() : 0L));

        List<MenuTree> result = new ArrayList<>();
        for (Permission menu : menus) {
            Long pid = menu.getPid() != null ? menu.getPid() : 0L;
            if (pid == 0L) {
                MenuTree tree = convertToMenuTree(menu);
                tree.setChildren(buildMenuChildren(menu.getId(), grouped));
                result.add(tree);
            }
        }
        result.sort(Comparator.comparing(MenuTree::getSort, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    private List<MenuTree> buildMenuChildren(Long pid, Map<Long, List<Permission>> grouped) {
        List<Permission> children = grouped.get(pid);
        if (children == null || children.isEmpty()) {
            return Collections.emptyList();
        }
        return children.stream()
                .map(menu -> {
                    MenuTree tree = convertToMenuTree(menu);
                    tree.setChildren(buildMenuChildren(menu.getId(), grouped));
                    return tree;
                })
                .sorted(Comparator.comparing(MenuTree::getSort, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    private MenuTree convertToMenuTree(Permission permission) {
        return MenuTree.builder()
                .id(permission.getId())
                .name(permission.getName())
                .pid(permission.getPid())
                .path(permission.getPath())
                .component(permission.getComponent())
                .componentName(permission.getComponentName())
                .menuType(permission.getMenuType())
                .visible(permission.getVisible())
                .status(permission.getStatus())
                .perms(permission.getPerms())
                .icon(permission.getIcon())
                .sort(permission.getSort())
                .build();
    }
}
