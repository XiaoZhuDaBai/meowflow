package com.meowflow.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.user.dto.*;
import com.meowflow.user.entity.User;
import com.meowflow.user.entity.UserRole;
import com.meowflow.user.repository.RoleRepository;
import com.meowflow.user.repository.UserRepository;
import com.meowflow.user.repository.UserRoleRepository;
import com.meowflow.user.security.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDTO createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BizException(ResultCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickName(request.getNickName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setSex(request.getSex());
        user.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "active");
        user.setOrgId(request.getOrgId());
        user.setPostId(request.getPostId());
        user.setLoginCount(0);
        user.setCreateBy(currentUserId());
        user.setCreateTime(LocalDateTime.now());
        user.setRemark(request.getRemark());

        userRepository.insert(user);

        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            assignRoles(user.getId(), request.getRoleIds());
        }

        return convertToUserDTO(user);
    }

    @Transactional
    public UserDTO updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));

        if (StringUtils.hasText(request.getNickName())) {
            user.setNickName(request.getNickName());
        }
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(request.getPhone());
        }
        if (StringUtils.hasText(request.getSex())) {
            user.setSex(request.getSex());
        }
        if (StringUtils.hasText(request.getAvatar())) {
            user.setAvatar(request.getAvatar());
        }
        if (StringUtils.hasText(request.getStatus())) {
            user.setStatus(request.getStatus());
        }
        if (request.getOrgId() != null) {
            user.setOrgId(request.getOrgId());
        }
        if (request.getPostId() != null) {
            user.setPostId(request.getPostId());
        }
        if (StringUtils.hasText(request.getRemark())) {
            user.setRemark(request.getRemark());
        }

        user.setUpdateBy(currentUserId());
        user.setUpdateTime(LocalDateTime.now());

        userRepository.updateById(user);

        if (request.getRoleIds() != null) {
            userRoleRepository.deleteByUserId(userId);
            assignRoles(userId, request.getRoleIds());
        }

        return convertToUserDTO(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        userRoleRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);
    }

    public UserDTO getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));

        Set<String> roleCodes = userRepository.findRoleCodesByUserId(userId);
        Set<String> permissions = userRepository.findPermissionsByUserId(userId);
        Set<Long> orgIds = userRepository.findOrgIdsByUserId(userId);
        user.setRoles(roleCodes);
        user.setPermissions(permissions);
        user.setOrgIds(orgIds);

        return convertToUserDTO(user);
    }

    public IPage<UserDTO> pageQuery(UserQuery query) {
        Page<User> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<User> result = userRepository.pageQuery(
                page,
                query.getKeyword(),
                query.getStatus(),
                query.getOrgId()
        );

        return result.convert(this::convertToUserDTO);
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateBy(currentUserId());
        user.setUpdateTime(LocalDateTime.now());
        userRepository.updateById(user);
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateBy(currentUserId());
        user.setUpdateTime(LocalDateTime.now());
        userRepository.updateById(user);
    }

    @Transactional
    public void updateStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));

        user.setStatus(status);
        user.setUpdateBy(currentUserId());
        user.setUpdateTime(LocalDateTime.now());
        userRepository.updateById(user);
    }

    @Transactional
    public void assignRoles(Long userId, Set<Long> roleIds) {
        userRoleRepository.deleteByUserId(userId);
        for (Long roleId : roleIds) {
            if (!roleRepository.existsById(roleId)) {
                throw new BizException(ResultCode.DATA_NOT_FOUND, "角色不存在: " + roleId);
            }
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRole.setCreateTime(LocalDateTime.now());
            userRoleRepository.insert(userRole);
        }
    }

    public List<UserDTO> getUsersByRoleId(Long roleId) {
        List<User> users = userRepository.findByRoleId(roleId);
        return users.stream()
                .map(this::convertToUserDTO)
                .collect(Collectors.toList());
    }

    private UserDTO convertToUserDTO(User user) {
        Set<String> roleCodes = user.getRoles();
        if (roleCodes == null || roleCodes.isEmpty()) {
            roleCodes = new HashSet<>();
        }

        Set<String> permissions = user.getPermissions();
        if (permissions == null || permissions.isEmpty()) {
            permissions = new HashSet<>();
        }

        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickName(user.getNickName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .sex(user.getSex())
                .status(user.getStatus())
                .orgId(user.getOrgId())
                .postId(user.getPostId())
                .loginIp(user.getLoginIp())
                .loginAt(user.getLoginAt())
                .loginCount(user.getLoginCount())
                .roles(roleCodes)
                .permissions(permissions)
                .createTime(user.getCreateTime())
                .remark(user.getRemark())
                .build();
    }

    private Long currentUserId() {
        return UserContextHolder.getUserId();
    }
}


