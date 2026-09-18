package com.meowflow.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.user.dto.RoleCreateRequest;
import com.meowflow.user.dto.RoleDTO;
import com.meowflow.user.entity.Role;
import com.meowflow.user.entity.RolePermission;
import com.meowflow.user.repository.PermissionRepository;
import com.meowflow.user.repository.RolePermissionRepository;
import com.meowflow.user.repository.RoleRepository;
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
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;

    @Transactional
    public RoleDTO createRole(RoleCreateRequest request) {
        if (roleRepository.existsByCode(request.getCode())) {
            throw new BizException(ResultCode.DATA_ALREADY_EXISTS, "角色编码已存在");
        }

        Role role = new Role();
        role.setName(request.getName());
        role.setCode(request.getCode());
        role.setSort(request.getRoleSort());
        role.setDataScope(StringUtils.hasText(request.getDataScope()) ? request.getDataScope() : "all");
        role.setMenuCheckStrictly(request.getMenuCheckStrictly() != null ? request.getMenuCheckStrictly() : true);
        role.setDeptCheckStrictly(request.getDeptCheckStrictly() != null ? request.getDeptCheckStrictly() : true);
        role.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "active");
        role.setCreateBy(currentUserId());
        role.setCreateTime(LocalDateTime.now());
        role.setRemark(request.getRemark());

        roleRepository.insert(role);

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            assignPermissions(role.getId(), request.getPermissionIds());
        }

        return convertToRoleDTO(role);
    }

    @Transactional
    public RoleDTO updateRole(Long roleId, RoleCreateRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BizException(ResultCode.DATA_NOT_FOUND, "角色不存在"));

        if (StringUtils.hasText(request.getName())) {
            role.setName(request.getName());
        }
        if (request.getRoleSort() != null) {
            role.setSort(request.getRoleSort());
        }
        if (StringUtils.hasText(request.getDataScope())) {
            role.setDataScope(request.getDataScope());
        }
        if (request.getMenuCheckStrictly() != null) {
            role.setMenuCheckStrictly(request.getMenuCheckStrictly());
        }
        if (request.getDeptCheckStrictly() != null) {
            role.setDeptCheckStrictly(request.getDeptCheckStrictly());
        }
        if (StringUtils.hasText(request.getStatus())) {
            role.setStatus(request.getStatus());
        }
        if (StringUtils.hasText(request.getRemark())) {
            role.setRemark(request.getRemark());
        }

        role.setUpdateBy(currentUserId());
        role.setUpdateTime(LocalDateTime.now());

        roleRepository.updateById(role);

        if (request.getPermissionIds() != null) {
            rolePermissionRepository.deleteByRoleId(roleId);
            assignPermissions(roleId, request.getPermissionIds());
        }

        return convertToRoleDTO(role);
    }

    @Transactional
    public void deleteRole(Long roleId) {
        if (!roleRepository.existsById(roleId)) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }
        rolePermissionRepository.deleteByRoleId(roleId);
        roleRepository.deleteById(roleId);
    }

    public RoleDTO getRoleById(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BizException(ResultCode.DATA_NOT_FOUND, "角色不存在"));

        Set<String> permissionIds = roleRepository.findPermissionIdsByRoleId(roleId)
                .stream()
                .map(String::valueOf)
                .collect(Collectors.toSet());
        role.setPermissionIds(permissionIds);

        return convertToRoleDTO(role);
    }

    public IPage<RoleDTO> pageQuery(String keyword, String status, Long pageNum, Long pageSize) {
        Page<Role> page = new Page<>(pageNum, pageSize);
        IPage<Role> result = roleRepository.pageQuery(page, keyword, status);

        return result.convert(this::convertToRoleDTO);
    }

    @Transactional
    public void assignPermissions(Long roleId, Set<Long> permissionIds) {
        for (Long permissionId : permissionIds) {
            if (!permissionRepository.existsById(permissionId)) {
                throw new BizException(ResultCode.DATA_NOT_FOUND, "权限不存在: " + permissionId);
            }
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(permissionId);
            rp.setCreateTime(LocalDateTime.now());
            rolePermissionRepository.insert(rp);
        }
    }

    public Set<Long> getRolePermissions(Long roleId) {
        return roleRepository.findPermissionIdsByRoleId(roleId);
    }

    public List<RoleDTO> getAllRoles() {
        List<Role> roles = roleRepository.selectList(null);
        return roles.stream()
                .map(this::convertToRoleDTO)
                .collect(Collectors.toList());
    }

    private RoleDTO convertToRoleDTO(Role role) {
        Set<String> permissionIds = role.getPermissionIds();
        if (permissionIds == null) {
            permissionIds = new HashSet<>();
        }

        return RoleDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .code(role.getCode())
                .sort(role.getSort())
                .dataScope(role.getDataScope())
                .menuCheckStrictly(role.getMenuCheckStrictly())
                .deptCheckStrictly(role.getDeptCheckStrictly())
                .status(role.getStatus())
                .permissionIds(permissionIds)
                .createTime(role.getCreateTime())
                .remark(role.getRemark())
                .build();
    }

    private Long currentUserId() {
        return UserContextHolder.getUserId();
    }
}
