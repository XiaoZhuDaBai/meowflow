package com.meowflow.user.service;

import cn.dev33.satoken.stp.StpUtil;
import com.meowflow.common.context.UserContext;
import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.user.dto.LoginRequest;
import com.meowflow.user.dto.LoginResponse;
import com.meowflow.user.dto.RegisterRequest;
import com.meowflow.user.dto.ResetPasswordRequest;
import com.meowflow.user.dto.UserDTO;
import com.meowflow.user.dto.VerifyCodeRequest;
import com.meowflow.user.entity.LoginLog;
import com.meowflow.user.entity.Org;
import com.meowflow.user.entity.Role;
import com.meowflow.user.entity.User;
import com.meowflow.user.entity.UserRole;
import com.meowflow.user.repository.LoginLogRepository;
import com.meowflow.user.repository.OrgRepository;
import com.meowflow.user.repository.RoleRepository;
import com.meowflow.user.repository.UserRepository;
import com.meowflow.user.repository.UserRoleRepository;
import com.meowflow.user.security.PasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final OrgRepository orgRepository;
    private final LoginLogRepository loginLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CaptchaService captchaService;
    private final EmailCodeService emailCodeService;

    /** 账号正常状态：与 mf_sys_user.status 的 active 字面值保持一致 */
    private static final String USER_STATUS_ACTIVE = "active";
    private static final String LOGIN_FAIL_KEY = "login:fail:";
    private static final String REFRESH_TOKEN_KEY = "refresh:token:";
    private static final String REFRESH_INDEX_KEY = "refresh:index:";
    private static final int MAX_LOGIN_FAIL = 5;
    private static final int ACCESS_TOKEN_TIMEOUT = 7200;
    private static final int REFRESH_TOKEN_TIMEOUT = 604800;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();

        checkLoginFailCount(username);

        // 图形验证码（可选）
        if (StringUtils.hasText(request.getUuid())) {
            if (!captchaService.verify(request.getUuid(), request.getCode())) {
                recordLoginFail(username, request, "验证码错误");
                throw new BizException(ResultCode.CAPTCHA_INVALID);
            }
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    recordLoginFail(username, request, "用户名不存在");
                    return new BizException(ResultCode.USER_NOT_FOUND);
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordLoginFail(username, request, "密码错误");
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }

        if (!USER_STATUS_ACTIVE.equals(user.getStatus())) {
            recordLoginFail(username, request, "账号已禁用");
            throw new BizException(ResultCode.ACCOUNT_LOCKED);
        }

        clearLoginFailCount(username);

        Set<String> permissions = userRepository.findPermissionsByUserId(user.getId());
        Set<String> roles = userRepository.findRoleCodesByUserId(user.getId());
        Set<Long> orgIds = userRepository.findOrgIdsByUserId(user.getId());
        user.setRoles(roles);
        user.setPermissions(permissions);
        user.setOrgIds(orgIds);

        issueTokenPair(user, roles, permissions);

        updateUserLoginInfo(user);

        LoginLog successLog = buildSuccessLoginLog(username);
        loginLogRepository.insert(successLog);

        return buildLoginResponse(user, roles, permissions);
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }

        String userIdValue = redisTemplate.opsForValue().get(REFRESH_INDEX_KEY + refreshToken) instanceof String value
                ? value
                : null;
        if (userIdValue == null) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }
        Long userId = Long.parseLong(userIdValue);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));

        // 校验：refreshToken 必须与 Redis 中的最新值一致
        Object stored = redisTemplate.opsForValue().get(REFRESH_TOKEN_KEY + userId);
        if (stored == null || !stored.toString().equals(refreshToken)) {
            throw new BizException(ResultCode.TOKEN_INVALID);
        }

        Set<String> permissions = userRepository.findPermissionsByUserId(userId);
        Set<String> roles = userRepository.findRoleCodesByUserId(userId);

        redisTemplate.delete(REFRESH_INDEX_KEY + refreshToken);
        issueTokenPair(user, roles, permissions);

        return buildLoginResponse(user, roles, permissions);
    }

    public void logout() {
        Long userId = UserContextHolder.getUserId();
        if (userId != null) {
            Object refreshToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_KEY + userId);
            if (refreshToken != null) {
                redisTemplate.delete(REFRESH_INDEX_KEY + refreshToken);
            }
            redisTemplate.delete(REFRESH_TOKEN_KEY + userId);
        }
        StpUtil.logout();
    }

    public UserDTO getCurrentUser() {
        Long userId = UserContextHolder.getUserId();
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

    /** 用户注册：图形验证码 → 邮箱验证码 → 创建用户 → 创建个人组织 → 绑定 admin 角色到个人组织 → 自动登录。 */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (!Boolean.TRUE.equals(request.getAgree())) {
            throw new BizException(ResultCode.BAD_REQUEST, "请先同意用户协议");
        }

        if (!captchaService.verify(request.getUuid(), request.getCaptchaCode())) {
            throw new BizException(ResultCode.CAPTCHA_INVALID);
        }

        if (!emailCodeService.verify(request.getEmail(), request.getEmailCode(), "REGISTER")) {
            throw new BizException(ResultCode.EMAIL_CODE_INVALID);
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BizException(ResultCode.USERNAME_EXISTS);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BizException(ResultCode.BAD_REQUEST, "该邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickName(request.getNickName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(USER_STATUS_ACTIVE);
        user.setSex(request.getSex());
        user.setLoginCount(0);
        user.setCreateBy(0L);
        user.setCreateTime(LocalDateTime.now());
        userRepository.insert(user);

        // 创建个人组织（每个用户都拥有自己的个人空间，code 唯一）
        String personalOrgCode = "personal_" + request.getUsername();
        if (orgRepository.existsByCode(personalOrgCode)) {
            throw new BizException(ResultCode.BIZ_ERROR, "个人组织 code 冲突，请更换用户名");
        }
        Org personalOrg = new Org();
        personalOrg.setParentId(0L);
        personalOrg.setName(request.getNickName() + " 的个人空间");
        personalOrg.setCode(personalOrgCode);
        personalOrg.setLeaderUserId(user.getId());
        personalOrg.setStatus("active");
        personalOrg.setSort(0);
        personalOrg.setCreateBy(user.getId());
        personalOrg.setCreateTime(LocalDateTime.now());
        orgRepository.insert(personalOrg);
        Long personalOrgId = personalOrg.getId();

        user.setOrgId(personalOrgId);
        userRepository.updateById(user);

        // 在个人组织下绑定 admin 角色（单用户即个人组织管理员）
        Role adminRole = roleRepository.findByCode("admin").orElseGet(() -> {
            Role role = new Role();
            role.setCode("admin");
            role.setName("管理员");
            role.setDataScope("org");
            role.setSort(1);
            role.setStatus("active");
            role.setCreateBy(0L);
            role.setCreateTime(LocalDateTime.now());
            roleRepository.insert(role);
            return role;
        });
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(adminRole.getId());
        userRole.setOrgId(personalOrgId);
        userRole.setCreateTime(LocalDateTime.now());
        userRoleRepository.insert(userRole);

        Set<String> roles = Set.of("admin");
        Set<String> permissions = userRepository.findPermissionsByUserId(user.getId());
        Set<Long> orgIds = userRepository.findOrgIdsByUserId(user.getId());
        user.setRoles(roles);
        user.setPermissions(permissions);
        user.setOrgIds(orgIds);

        issueTokenPair(user, roles, permissions);

        loginLogRepository.insert(buildSuccessLoginLog(user.getUsername()));

        return buildLoginResponse(user, roles, permissions);
    }

    public void sendEmailCode(VerifyCodeRequest request) {
        String email = request.getEmail();
        String type = request.getType();

        if ("REGISTER".equals(type)) {
            if (userRepository.existsByEmail(email)) {
                throw new BizException(ResultCode.BAD_REQUEST, "该邮箱已被注册");
            }
            emailCodeService.sendCode(email, type, null);
        } else {
            if (!userRepository.existsByEmail(email)) {
                throw new BizException(ResultCode.USER_NOT_FOUND, "该邮箱尚未注册");
            }
            userRepository.findByEmail(email).ifPresent(u ->
                    emailCodeService.sendCode(email, type, u.getUsername())
            );
        }
    }

    @Transactional
    public void resetPasswordByEmail(ResetPasswordRequest request) {
        if (!emailCodeService.verify(request.getEmail(), request.getCode(), "RESET_PWD")) {
            throw new BizException(ResultCode.EMAIL_CODE_INVALID);
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BizException(ResultCode.USER_NOT_FOUND));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdateBy(0L);
        user.setUpdateTime(LocalDateTime.now());
        userRepository.updateById(user);
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    /**
     * 颁发 access + refresh 双 token，并写入 Redis 让 refresh 可校验可吊销。
     */
    private void issueTokenPair(User user, Set<String> roles, Set<String> permissions) {
        // 1) access token —— SaToken 默认空间
        StpUtil.login(user.getId());
        String accessToken = StpUtil.getTokenValue();

        // 2) refresh token —— 使用独立随机值，并通过 Redis 建立 token -> userId 索引
        String refreshToken = UUID.randomUUID().toString().replace("-", "");

        // 3) 持久化：refresh 落 Redis，TTL = 7 天
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_KEY + user.getId(),
                refreshToken,
                REFRESH_TOKEN_TIMEOUT,
                TimeUnit.SECONDS
        );
        redisTemplate.opsForValue().set(
                REFRESH_INDEX_KEY + refreshToken,
                String.valueOf(user.getId()),
                REFRESH_TOKEN_TIMEOUT,
                TimeUnit.SECONDS
        );

        setUserContext(user, roles, permissions);
    }

    private LoginResponse buildLoginResponse(User user, Set<String> roles, Set<String> permissions) {
        user.setRoles(roles);
        user.setPermissions(permissions);
        UserDTO userDTO = convertToUserDTO(user);
        return LoginResponse.builder()
                .accessToken(StpUtil.getTokenValue())
                .refreshToken((String) redisTemplate.opsForValue().get(REFRESH_TOKEN_KEY + user.getId()))
                .tokenType("Bearer")
                .expiresIn((long) ACCESS_TOKEN_TIMEOUT)
                .user(userDTO)
                .build();
    }

    private void checkLoginFailCount(String username) {
        String key = LOGIN_FAIL_KEY + username;
        Object count = redisTemplate.opsForValue().get(key);
        if (count != null && Integer.parseInt(count.toString()) >= MAX_LOGIN_FAIL) {
            throw new BizException(ResultCode.ACCOUNT_LOCKED, "登录失败次数过多，请1小时后再试");
        }
    }

    private void recordLoginFail(String username, LoginRequest request, String reason) {
        String key = LOGIN_FAIL_KEY + username;
        Long failCount = redisTemplate.opsForValue().increment(key);
        if (failCount != null && failCount == 1) {
            redisTemplate.expire(key, Duration.ofHours(1));
        }
        loginLogRepository.insert(buildFailLoginLog(username, reason));
    }

    private void clearLoginFailCount(String username) {
        redisTemplate.delete(LOGIN_FAIL_KEY + username);
    }

    private void updateUserLoginInfo(User user) {
        user.setLoginIp(getClientIp());
        user.setLoginAt(LocalDateTime.now());
        user.setLoginCount(user.getLoginCount() == null ? 1 : user.getLoginCount() + 1);
        userRepository.updateById(user);
    }

    private void setUserContext(User user, Set<String> roles, Set<String> permissions) {
        UserContext context = UserContext.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickName())
                .orgId(user.getOrgId())
                .email(user.getEmail())
                .roles(roles.stream().toList())
                .permissions(permissions.stream().toList())
                .build();
        UserContextHolder.set(context);
    }

    private LoginLog buildSuccessLoginLog(String username) {
        LoginLog log = new LoginLog();
        log.setUsername(username);
        log.setIp(getClientIp());
        log.setStatus("success");
        log.setMessage("登录成功");
        log.setLoginAt(LocalDateTime.now());
        log.setUserAgent(getBrowser());
        return log;
    }

    private LoginLog buildFailLoginLog(String username, String reason) {
        LoginLog log = new LoginLog();
        log.setUsername(username);
        log.setIp(getClientIp());
        log.setStatus("fail");
        log.setMessage(reason);
        log.setLoginAt(LocalDateTime.now());
        log.setUserAgent(getBrowser());
        return log;
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs == null ? null : attrs.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) return "127.0.0.1";
        String header = req.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(header)) {
            return header.split(",")[0].trim();
        }
        String real = req.getHeader("X-Real-IP");
        if (StringUtils.hasText(real)) return real;
        return req.getRemoteAddr();
    }

    private String getBrowser() {
        HttpServletRequest req = currentRequest();
        if (req == null) return "Unknown";
        String ua = req.getHeader("User-Agent");
        if (ua == null) return "Unknown";
        if (ua.contains("Edg")) return "Edge";
        if (ua.contains("Chrome")) return "Chrome";
        if (ua.contains("Firefox")) return "Firefox";
        if (ua.contains("Safari")) return "Safari";
        return "Unknown";
    }

    private String getOs() {
        HttpServletRequest req = currentRequest();
        if (req == null) return "Unknown";
        String ua = req.getHeader("User-Agent");
        if (ua == null) return "Unknown";
        if (ua.contains("Windows")) return "Windows";
        if (ua.contains("Mac")) return "macOS";
        if (ua.contains("Linux")) return "Linux";
        if (ua.contains("Android")) return "Android";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS";
        return "Unknown";
    }

    private UserDTO convertToUserDTO(User user) {
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
                .loginIp(user.getLoginIp())
                .loginAt(user.getLoginAt())
                .loginCount(user.getLoginCount())
                .roles(user.getRoles())
                .permissions(user.getPermissions())
                .orgIds(user.getOrgIds())
                .createTime(user.getCreateTime())
                .remark(user.getRemark())
                .build();
    }
}
