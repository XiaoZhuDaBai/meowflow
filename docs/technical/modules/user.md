# MeowFlow User - 用户服务模块设计文档

> 本文档详细描述喵流平台的用户服务模块（MeowFlow-user）的设计与实现

---

## 一、模块概述

### 1.1 模块定位

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           MeowFlow-user 模块定位                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  MeowFlow-user 是整个平台的用户与权限管理模块，负责：                       │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                                                                   │   │
│  │   1. 用户管理                                                    │   │
│  │      • 用户注册、登录、注销                                       │   │
│  │      • 用户信息管理                                              │   │
│  │      • 组织架构管理                                              │   │
│  │                                                                   │   │
│  │   2. 权限管理                                                    │   │
│  │      • 角色定义与分配                                            │   │
│  │      • 权限定义与分配                                            │   │
│  │      • 数据权限控制                                              │   │
│  │                                                                   │   │
│  │   3. 认证授权                                                    │   │
│  │      • JWT Token 管理                                            │   │
│  │      • 单点登录                                                  │   │
│  │      • 第三方登录                                                │   │
│  │                                                                   │   │
│  │   4. 审计日志                                                    │   │
│  │      • 登录日志                                                  │   │
│  │      • 操作日志                                                  │   │
│  │                                                                   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 核心设计理念

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           核心设计理念                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  1. 安全优先                                                          │
│  ───────────────────────────────────────────────────────────────────────│
│  • 密码加密存储（BCrypt）                                             │
│  • SaToken 会话管理                                                   │
│  • 防暴力破解（登录失败次数限制）                                      │
│  • 操作审计                                                          │
│                                                                          │
│  2. 灵活权限                                                          │
│  ───────────────────────────────────────────────────────────────────────│
│  • RBAC 权限模型（角色-权限）                                        │
│  • 数据权限多级控制                                                   │
│  • 动态权限变更                                                      │
│                                                                          │
│  3. 高效认证                                                          │
│  ───────────────────────────────────────────────────────────────────────│
│  • SaToken 简化 Token 管理                                            │
│  • Redis 会话存储，支持分布式                                          │
│  • 自动续期机制                                                       │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 二、模块结构

### 2.1 目录结构

```
meowflow-user/
├── pom.xml
└── src/main/java/com/meowflow/user/
    │
    ├── controller/                      # 控制器层
    │   ├── AuthController.java        # 认证接口
    │   ├── UserController.java        # 用户管理
    │   ├── RoleController.java        # 角色管理
    │   ├── PermissionController.java   # 权限管理
    │   ├── OrgController.java         # 组织管理
    │   ├── LoginLogController.java    # 登录日志
    │   └── AuditLogController.java    # 审计日志
    │
    ├── service/                        # 业务逻辑层
    │   ├── AuthService.java           # 认证服务（SaToken）
    │   ├── UserService.java           # 用户服务
    │   ├── RoleService.java           # 角色服务
    │   ├── PermissionService.java     # 权限服务
    │   ├── OrgService.java            # 组织服务
    │   ├── LoginLogService.java       # 登录日志服务
    │   └── SecurityService.java       # 安全服务
    │
    ├── repository/                     # 数据访问层
    │   ├── UserRepository.java
    │   ├── RoleRepository.java
    │   ├── PermissionRepository.java
    │   ├── OrgRepository.java
    │   ├── PostRepository.java
    │   ├── UserRoleRepository.java
    │   ├── RolePermissionRepository.java
    │   ├── LoginLogRepository.java
    │   └── AuditLogRepository.java
    │
    ├── entity/                        # 实体类
    │   ├── User.java
    │   ├── Role.java
    │   ├── Permission.java
    │   ├── Org.java
    │   ├── Post.java
    │   ├── UserRole.java
    │   ├── RolePermission.java
    │   ├── LoginLog.java
    │   └── AuditLog.java
    │
    ├── dto/                           # 数据传输对象
    │   ├── LoginRequest.java
    │   ├── LoginResponse.java
    │   ├── UserDTO.java
    │   ├── UserCreateRequest.java
    │   ├── UserUpdateRequest.java
    │   ├── UserQuery.java
    │   ├── RoleDTO.java
    │   ├── RoleCreateRequest.java
    │   ├── MenuTree.java
    │   ├── OrgTree.java
    │   ├── PasswordChangeDTO.java
    │   ├── PasswordResetDTO.java
    │   ├── LoginLogDTO.java
    │   └── AuditLogDTO.java
    │
    ├── security/                      # 安全相关
    │   ├── PasswordEncoder.java      # 密码加密（BCrypt）
    │   ├── SaTokenConfig.java        # SaToken 配置
    │   └── SaExceptionHandler.java   # 认证异常处理
    │
    ├── config/                        # 配置类
    │   ├── SaTokenWebMvcConfigurer.java
    │   ├── OpenApiConfig.java
    │   └── MyMetaObjectHandler.java
    │
    ├── aspectj/                       # AOP 切面
    │   ├── DataScopeAspect.java      # 数据权限切面
    │   ├── DataScope.java            # 数据权限注解
    │   ├── DataScopeContext.java     # 数据权限上下文
    │   ├── OperationLogAspect.java   # 操作日志切面
    │   └── OperationLog.java         # 操作日志注解
    │
    └── MeowFlowUserApplication.java   # 启动类
```

---

## 三、用户管理

### 3.1 用户实体

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          用户实体设计                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  User 用户实体                                                   │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  @TableName("sys_user")                                         │    │
│  │  public class User implements UserDetails, Serializable {           │    │
│  │                                                                   │    │
│  │      @TableId                                                    │    │
│  │      private String id;                                          │    │
│  │                                                                   │    │
│  │      @Column(unique = true, nullable = false)                   │    │
│  │      private String username;                                    │    │
│  │                                                                   │    │
│  │      private String password;                                     │    │
│  │                                                                   │    │
│  │      private String nickName;                                     │    │
│  │                                                                   │    │
│  │      @Column(unique = true)                                      │    │
│  │      private String email;                                        │    │
│  │                                                                   │    │
│  │      @Column(unique = true)                                      │    │
│  │      private String phone;                                        │    │
│  │                                                                   │    │
│  │      private String avatar;                                       │    │
│  │                                                                   │    │
│  │      private String sex;        // unknown/male/female           │    │
│  │                                                                   │    │
│  │      private String status;     // 1:启用 0:禁用                 │    │
│  │                                                                   │    │
│  │      private String loginIp;                                      │    │
│  │                                                                   │    │
│  │      private LocalDateTime loginDate;                             │    │
│  │                                                                   │    │
│  │      private Integer loginCount;                                  │    │
│  │                                                                   │    │
│  │      private String createDept;                                   │    │
│  │                                                                   │    │
│  │      private String createBy;                                     │    │
│  │                                                                   │    │
│  │      private LocalDateTime createTime;                            │    │
│  │                                                                   │    │
│  │      private String updateBy;                                     │    │
│  │                                                                   │    │
│  │      private LocalDateTime updateTime;                            │    │
│  │                                                                   │    │
│  │      private String remark;                                       │    │
│  │                                                                   │    │
│  │      // Spring Security 接口实现                                  │    │
│  │      @Override                                                    │    │
│  │      public Collection<? extends GrantedAuthority>                 │    │
│  │              getAuthorities() {                                  │    │
│  │          return roles.stream()                                   │    │
│  │              .flatMap(role -> role.getPermissions().stream())   │    │
│  │              .map(p -> new SimpleGrantedAuthority(p.getPerms()))  │    │
│  │              .collect(Collectors.toList());                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @Override                                                    │    │
│  │      public boolean isAccountNonExpired() { return true; }      │    │
│  │                                                                   │    │
│  │      @Override                                                    │    │
│  │      public boolean isAccountNonLocked() {                       │    │
│  │          return !"0".equals(status);                             │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @Override                                                    │    │
│  │      public boolean isCredentialsNonExpired() { return true; }   │    │
│  │                                                                   │    │
│  │      @Override                                                    │    │
│  │      public boolean isEnabled() {                                │    │
│  │          return "1".equals(status);                              │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.2 用户服务

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          用户服务设计                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  UserService 用户服务                                             │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  @Transactional                                                 │    │
│  │  public class UserService implements UserDetailsManager {         │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private UserRepository userRepository;                        │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private PasswordEncoder passwordEncoder;                     │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 创建用户                                                │    │
│  │       */                                                        │    │
│  │      @Override                                                   │    │
│  │      public User createUser(User user) {                         │    │
│  │          // 验证用户名唯一                                       │    │
│  │          if (userRepository.existsByUsername(user.getUsername())) {│    │
│  │              throw new BizException(ResultCode.USERNAME_EXISTS); │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 加密密码                                             │    │
│  │          user.setPassword(passwordEncoder.encode(user.getPassword()));│ │
│  │                                                                   │    │
│  │          // 设置默认状态                                         │    │
│  │          if (StringUtils.isBlank(user.getStatus())) {           │    │
│  │              user.setStatus("1");                               │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          return userRepository.save(user);                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 更新用户                                                │    │
│  │       */                                                        │    │
│  │      public User updateUser(String userId, UserUpdateRequest req) {│  │
│  │          User user = userRepository.findById(userId)            │    │
│  │              .orElseThrow(() -> new UserNotFoundException(userId));│ │
│  │                                                                   │    │
│  │          if (StringUtils.isNotBlank(req.getNickName())) {      │    │
│  │              user.setNickName(req.getNickName());               │    │
│  │          }                                                       │    │
│  │          if (StringUtils.isNotBlank(req.getEmail())) {         │    │
│  │              user.setEmail(req.getEmail());                     │    │
│  │          }                                                       │    │
│  │          if (StringUtils.isNotBlank(req.getPhone())) {         │    │
│  │              user.setPhone(req.getPhone());                     │    │
│  │          }                                                       │    │
│  │          if (StringUtils.isNotBlank(req.getSex())) {           │    │
│  │              user.setSex(req.getSex());                         │    │
│  │          }                                                       │    │
│  │          if (StringUtils.isNotBlank(req.getAvatar())) {        │    │
│  │              user.setAvatar(req.getAvatar());                   │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          return userRepository.save(user);                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 修改密码                                                │    │
│  │       */                                                        │    │
│  │      public void changePassword(String userId, String oldPwd,   │    │
│  │                                  String newPwd) {               │    │
│  │          User user = userRepository.findById(userId)            │    │
│  │              .orElseThrow(() -> new UserNotFoundException(userId));│ │
│  │                                                                   │    │
│  │          // 验证旧密码                                           │    │
│  │          if (!passwordEncoder.matches(oldPwd, user.getPassword())) {│  │
│  │              throw new BizException(ResultCode.PASSWORD_ERROR);  │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 设置新密码                                           │    │
│  │          user.setPassword(passwordEncoder.encode(newPwd));       │    │
│  │          userRepository.save(user);                              │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 重置密码                                                │    │
│  │       */                                                        │    │
│  │      public void resetPassword(String userId, String newPwd) {   │    │
│  │          User user = userRepository.findById(userId)            │    │
│  │              .orElseThrow(() -> new UserNotFoundException(userId));│ │
│  │                                                                   │    │
│  │          user.setPassword(passwordEncoder.encode(newPwd));       │    │
│  │          userRepository.save(user);                              │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 启用/禁用用户                                          │    │
│  │       */                                                        │    │
│  │      public void updateStatus(String userId, String status) {   │    │
│  │          User user = userRepository.findById(userId)            │    │
│  │              .orElseThrow(() -> new UserNotFoundException(userId));│ │
│  │          user.setStatus(status);                                │    │
│  │          userRepository.save(user);                              │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 删除用户                                                │    │
│  │       */                                                        │    │
│  │      public void deleteUser(String userId) {                    │    │
│  │          userRepository.deleteById(userId);                      │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 根据用户名查询                                          │    │
│  │       */                                                        │    │
│  │      @Override                                                   │    │
│  │      public User loadUserByUsername(String username) {           │    │
│  │          return userRepository.findByUsername(username)         │    │
│  │              .orElseThrow(() -> new UsernameNotFoundException(   │    │
│  │                  "用户不存在: " + username));                    │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 四、认证服务

> 认证模块基于 **SaToken** 框架实现，简化了 Token 管理和会话处理。

### 4.1 SaToken 配置

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          SaToken 认证设计                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  SaToken 认证流程                                                 │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  1. 登录时调用 StpUtil.login(userId) 生成 Token                  │    │
│  │  2. Token 自动存入 Redis（默认 7 天有效期）                      │    │
│  │  3. 请求头携带 Authorization: <token> 即可完成认证               │    │
│  │  4. 使用 StpUtil.getLoginId() 获取当前登录用户ID                │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  SaTokenConfig SaToken 配置类                                    │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Configuration                                                  │    │
│  │  public class SaTokenConfig {                                     │    │
│  │                                                                   │    │
│  │      @Bean                                                        │    │
│  │      public StpLogic getStpLogicSa() {                           │    │
│  │          return new StpLogicSa();                                │    │
│  │      }                                                            │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  SaTokenWebMvcConfigurer Web 适配器                               │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Configuration                                                  │    │
│  │  public class SaTokenWebMvcConfigurer implements                  │    │
│  │          WebMvcConfigurer {                                       │    │
│  │                                                                   │    │
│  │      @Override                                                    │    │
│  │      public void addInterceptors(InterceptorRegistry registry) {  │    │
│  │          // 注册 SaToken 拦截器                                    │    │
│  │          registry.addInterceptor(new SaInterceptor(handle -> {  │    │
│  │              // 鉴权逻辑                                           │    │
│  │          })).addPathPatterns("/**")                              │    │
│  │            .excludePathPatterns(                                  │    │
│  │                "/api/v1/auth/login",                             │    │
│  │                "/api/v1/auth/refresh",                           │    │
│  │                "/swagger-ui/**",                                │    │
│  │                "/v3/api-docs/**"                                 │    │
│  │            );                                                    │    │
│  │      }                                                            │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.2 认证服务

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          认证服务设计                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  AuthService 认证服务                                             │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  public class AuthService {                                       │    │
│  │                                                                   │    │
│  │      private static final String LOGIN_FAIL_KEY = "login:fail:";│    │
│  │      private static final String REFRESH_TOKEN_KEY = "refresh:token:";│ │
│  │      private static final int MAX_LOGIN_FAIL = 5;               │    │
│  │      private static final int ACCESS_TOKEN_TIMEOUT = 7200;      │    │
│  │      private static final int REFRESH_TOKEN_TIMEOUT = 604800;   │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 用户登录                                                │    │
│  │       */                                                        │    │
│  │      public LoginResponse login(LoginRequest request) {         │    │
│  │          // 1. 检查登录失败次数                                  │    │
│  │          checkLoginFailCount(username);                          │    │
│  │                                                                   │    │
│  │          // 2. 验证用户                                          │    │
│  │          User user = userRepository.findByUsername(username)     │    │
│  │              .orElseThrow(() -> { recordLoginFail(...);         │    │
│  │                  return new BizException(ResultCode.USER_NOT_FOUND);│ │
│  │              });                                                 │    │
│  │                                                                   │    │
│  │          // 3. 验证密码                                          │    │
│  │          if (!passwordEncoder.matches(request.getPassword(),    │    │
│  │                                        user.getPassword())) {    │    │
│  │              recordLoginFail(username, request, "密码错误");     │    │
│  │              throw new BizException(ResultCode.PASSWORD_ERROR);  │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 4. 检查账号状态                                      │    │
│  │          if ("0".equals(user.getStatus())) {                    │    │
│  │              throw new BizException(ResultCode.ACCOUNT_LOCKED);  │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          // 5. 清除登录失败记录                                  │    │
│  │          clearLoginFailCount(username);                          │    │
│  │                                                                   │    │
│  │          // 6. SaToken 登录                                      │    │
│  │          StpUtil.login(user.getId());                            │    │
│  │          String token = StpUtil.getTokenValue();                │    │
│  │                                                                   │    │
│  │          // 7. 保存 Refresh Token                                │    │
│  │          saveRefreshToken(user.getId(), token);                  │    │
│  │                                                                   │    │
│  │          // 8. 更新用户登录信息                                  │    │
│  │          updateUserLoginInfo(user);                              │    │
│  │                                                                   │    │
│  │          // 9. 记录登录日志                                      │    │
│  │          loginLogRepository.insert(buildSuccessLoginLog(...));   │    │
│  │                                                                   │    │
│  │          // 10. 设置用户上下文                                  │    │
│  │          Set<String> roles = userRepository.findRoleCodesByUserId(user.getId());│ │
│  │          Set<String> permissions = userRepository.findPermissionsByUserId(user.getId());│ │
│  │          setUserContext(user, roles, permissions);               │    │
│  │                                                                   │    │
│  │          return LoginResponse.builder()                          │    │
│  │              .accessToken(token)                                │    │
│  │              .refreshToken(token)                                │    │
│  │              .tokenType("Bearer")                               │    │
│  │              .expiresIn((long) ACCESS_TOKEN_TIMEOUT)            │    │
│  │              .userId(user.getId())                              │    │
│  │              .username(user.getUsername())                      │    │
│  │              .nickname(user.getNickName())                      │    │
│  │              .build();                                           │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 刷新 Token                                              │    │
│  │       */                                                        │    │
│  │      public LoginResponse refreshToken(String refreshToken) {    │    │
│  │          Object userId = StpUtil.getLoginIdByToken(refreshToken);│    │
│  │          if (userId == null) {                                  │    │
│  │              throw new BizException(ResultCode.TOKEN_INVALID);   │    │
│  │          }                                                       │    │
│  │                                                                   │    │
│  │          StpUtil.logoutByTokenValue(refreshToken);              │    │
│  │          StpUtil.login(userId.toString());                      │    │
│  │          String newToken = StpUtil.getTokenValue();             │    │
│  │          saveRefreshToken(userId.toString(), newToken);          │    │
│  │                                                                   │    │
│  │          return LoginResponse.builder()                          │    │
│  │              .accessToken(newToken)                             │    │
│  │              .refreshToken(newToken)                             │    │
│  │              .tokenType("Bearer")                               │    │
│  │              .expiresIn((long) ACCESS_TOKEN_TIMEOUT)            │    │
│  │              .build();                                           │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 登出                                                    │    │
│  │       */                                                        │    │
│  │      public void logout() {                                      │    │
│  │          String userId = UserContextHolder.getUserId();           │    │
│  │          if (userId != null) {                                  │    │
│  │              redisTemplate.delete(REFRESH_TOKEN_KEY + userId);   │    │
│  │          }                                                       │    │
│  │          StpUtil.logout();                                       │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 五、角色与权限

### 5.1 角色实体

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          角色实体设计                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  Role 角色实体                                                   │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  @TableName("sys_role")                                         │    │
│  │  public class Role implements Serializable {                     │    │
│  │                                                                   │    │
│  │      @TableId                                                    │    │
│  │      private String id;                                          │    │
│  │                                                                   │    │
│  │      @Column(nullable = false)                                   │    │
│  │      private String name;                                         │    │
│  │                                                                   │    │
│  │      @Column(unique = true, nullable = false)                   │    │
│  │      private String code;                                         │    │
│  │                                                                   │    │
│  │      private Integer roleSort;                                   │    │
│  │                                                                   │    │
│  │      private String dataScope;    // 1:全部 2:本部门 3:本部门及以下 │    │
│  │                                                                   │    │
│  │      private Boolean menuCheckStrictly;                           │    │
│  │      private Boolean deptCheckStrictly;                           │    │
│  │                                                                   │    │
│  │      private String status;       // 1:启用 0:禁用                │    │
│  │                                                                   │    │
│  │      private String createDept;                                   │    │
│  │      private String createBy;                                     │    │
│  │      private LocalDateTime createTime;                            │    │
│  │      private String updateBy;                                     │    │
│  │      private LocalDateTime updateTime;                            │    │
│  │      private String remark;                                       │    │
│  │                                                                   │    │
│  │      @ManyToMany(fetch = FetchType.LAZY)                        │    │
│  │      @JoinTable(name = "sys_role_permission",                   │    │
│  │          joinColumns = @JoinColumn(name = "role_id"),            │    │
│  │          inverseJoinColumns = @JoinColumn(name = "permission_id"))│   │
│  │      private Set<Permission> permissions = new HashSet<>();      │    │
│  │                                                                   │    │
│  │      @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)    │    │
│  │      private Set<User> users = new HashSet<>();                  │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  Permission 权限实体                                             │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  @TableName("sys_permission")                                   │    │
│  │  public class Permission implements Serializable {               │    │
│  │                                                                   │    │
│  │      @TableId                                                    │    │
│  │      private String id;                                          │    │
│  │                                                                   │    │
│  │      @Column(nullable = false)                                   │    │
│  │      private String name;                                         │    │
│  │                                                                   │    │
│  │      private String pid;           // 父权限ID                   │    │
│  │      private String path;                                       │    │
│  │      private String component;                                   │    │
│  │      private String componentName;                               │    │
│  │                                                                   │    │
│  │      @Column(nullable = false)                                   │    │
│  │      private String menuType;    // M:目录 C:菜单 F:按钮         │    │
│  │                                                                   │    │
│  │      private String visible;      // 1:显示 0:隐藏               │    │
│  │      private String status;      // 1:正常 0:停用                │    │
│  │                                                                   │    │
│  │      private String perms;       // 权限标识                     │    │
│  │      private String permsType;   // 权限类型                     │    │
│  │                                                                   │    │
│  │      private String icon;                                       │    │
│  │      private Integer sort;                                       │    │
│  │                                                                   │    │
│  │      private String createDept;                                   │    │
│  │      private String createBy;                                     │    │
│  │      private LocalDateTime createTime;                            │    │
│  │      private String updateBy;                                     │    │
│  │      private LocalDateTime updateTime;                            │    │
│  │      private String remark;                                       │    │
│  │                                                                   │    │
│  │      @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)│  │
│  │      private Set<Role> roles = new HashSet<>();                   │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.2 权限服务

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          权限服务设计                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  PermissionService 权限服务                                       │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  public class PermissionService {                                 │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private PermissionRepository permissionRepository;           │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private RoleRepository roleRepository;                      │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取用户菜单                                            │    │
│  │       */                                                        │    │
│  │      public List<MenuTree> getUserMenus(String userId) {         │    │
│  │          User user = userRepository.findById(userId)             │    │
│  │              .orElseThrow(() -> new UserNotFoundException(userId));│ │
│  │                                                                   │    │
│  │          // 获取用户的权限集合                                    │    │
│  │          Set<String> permissions = user.getRoles().stream()      │    │
│  │              .flatMap(role -> role.getPermissions().stream())   │    │
│  │              .map(Permission::getPerms)                        │    │
│  │              .collect(Collectors.toSet());                     │    │
│  │                                                                   │    │
│  │          // 查询所有菜单类型的权限                                │    │
│  │          List<Permission> allMenus = permissionRepository        │    │
│  │              .findByMenuTypeInAndStatusAndPermsIn(             │    │
│  │                  Arrays.asList("M", "C"), "1", permissions);   │    │
│  │                                                                   │    │
│  │          // 构建菜单树                                           │    │
│  │          return buildMenuTree(allMenus);                         │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取用户按钮权限                                         │    │
│  │       */                                                        │    │
│  │      public Set<String> getUserButtonPerms(String userId) {      │    │
│  │          User user = userRepository.findById(userId)             │    │
│  │              .orElseThrow(() -> new UserNotFoundException(userId));│ │
│  │                                                                   │    │
│  │          return user.getRoles().stream()                        │    │
│  │              .flatMap(role -> role.getPermissions().stream())   │    │
│  │              .filter(p -> "F".equals(p.getMenuType()))         │    │
│  │              .map(Permission::getPerms)                          │    │
│  │              .collect(Collectors.toSet());                     │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 检查用户是否有权限                                       │    │
│  │       */                                                        │    │
│  │      public boolean hasPermission(String userId, String perm) {  │    │
│  │          User user = userRepository.findById(userId)             │    │
│  │              .orElseThrow(() -> new UserNotFoundException(userId));│ │
│  │                                                                   │    │
│  │          return user.getRoles().stream()                        │    │
│  │              .flatMap(role -> role.getPermissions().stream())   │    │
│  │              .anyMatch(p -> p.getPerms().equals(perm));        │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 分配角色权限                                            │    │
│  │       */                                                        │    │
│  │      @Transactional                                             │    │
│  │      public void assignRolePermissions(String roleId,            │    │
│  │                                        List<String> permissionIds) {│  │
│  │          Role role = roleRepository.findById(roleId)            │    │
│  │              .orElseThrow(() -> new RoleNotFoundException(roleId));│ │
│  │                                                                   │    │
│  │          // 清空现有权限                                         │    │
│  │          role.getPermissions().clear();                          │    │
│  │                                                                   │    │
│  │          // 添加新权限                                           │    │
│  │          List<Permission> permissions = permissionRepository     │    │
│  │              .findAllById(permissionIds);                       │    │
│  │          role.getPermissions().addAll(permissions);              │    │
│  │                                                                   │    │
│  │          roleRepository.save(role);                              │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 获取角色权限                                            │    │
│  │       */                                                        │    │
│  │      public Set<String> getRolePermissions(String roleId) {      │    │
│  │          Role role = roleRepository.findById(roleId)            │    │
│  │              .orElseThrow(() -> new RoleNotFoundException(roleId));│ │
│  │                                                                   │    │
│  │          return role.getPermissions().stream()                  │    │
│  │              .map(Permission::getId)                            │    │
│  │              .collect(Collectors.toSet());                      │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private List<MenuTree> buildMenuTree(List<Permission> menus) {│  │
│  │          Map<String, List<Permission>> grouped = menus.stream() │    │
│  │              .collect(Collectors.groupingBy(p ->               │    │
│  │                  StringUtils.isBlank(p.getPid()) ? "0" : p.getPid()));│ │
│  │                                                                   │    │
│  │          List<MenuTree> result = new ArrayList<>();            │    │
│  │          for (Permission menu : menus) {                        │    │
│  │              if ("0".equals(menu.getPid())) {                  │    │
│  │                  MenuTree tree = toMenuTree(menu);              │    │
│  │                  tree.setChildren(buildChildren(menu.getId(), grouped));│  │
│  │                  result.add(tree);                              │    │
│  │              }                                                   │    │
│  │          }                                                       │    │
│  │          return result;                                         │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private List<MenuTree> buildChildren(String pid,            │    │
│  │                                    Map<String, List<Permission>> grouped) {│ │
│  │          List<Permission> children = grouped.get(pid);           │    │
│  │          if (children == null) return Collections.emptyList();  │    │
│  │                                                                   │    │
│  │          return children.stream()                               │    │
│  │              .map(menu -> {                                     │    │
│  │                  MenuTree tree = toMenuTree(menu);               │    │
│  │                  tree.setChildren(buildChildren(menu.getId(), grouped));│  │
│  │                  return tree;                                   │    │
│  │              })                                                   │    │
│  │              .collect(Collectors.toList());                       │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private MenuTree toMenuTree(Permission menu) {             │    │
│  │          MenuTree tree = new MenuTree();                        │    │
│  │          tree.setId(menu.getId());                             │    │
│  │          tree.setName(menu.getName());                          │    │
│  │          tree.setPath(menu.getPath());                          │    │
│  │          tree.setComponent(menu.getComponent());                 │    │
│  │          tree.setIcon(menu.getIcon());                          │    │
│  │          tree.setSort(menu.getSort());                          │    │
│  │          return tree;                                           │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 六、组织管理

### 6.1 组织实体

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          组织实体设计                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  Org 组织实体                                                    │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Data                                                          │    │
│  │  @TableName("sys_org")                                          │    │
│  │  public class Org implements Serializable {                       │    │
│  │                                                                   │    │
│  │      @TableId                                                    │    │
│  │      private String id;                                          │    │
│  │                                                                   │    │
│  │      private String parentId;    // 父组织ID                      │    │
│  │      private String ancestors;    // 祖级列表                      │    │
│  │                                                                   │    │
│  │      @Column(nullable = false)                                   │    │
│  │      private String name;                                         │    │
│  │                                                                   │    │
│  │      private String code;                                        │    │
│  │      private String leader;                                      │    │
│  │      private String phone;                                       │    │
│  │      private String email;                                       │    │
│  │                                                                   │    │
│  │      private Integer sort;                                       │    │
│  │      private String status;                                      │    │
│  │                                                                   │    │
│  │      private String createDept;                                   │    │
│  │      private String createBy;                                     │    │
│  │      private LocalDateTime createTime;                            │    │
│  │      private String updateBy;                                     │    │
│  │      private LocalDateTime updateTime;                            │    │
│  │      private String remark;                                       │    │
│  │                                                                   │    │
│  │      @OneToMany(mappedBy = "org", fetch = FetchType.LAZY)       │    │
│  │      private Set<User> users = new HashSet<>();                  │    │
│  │                                                                   │    │
│  │      @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)     │    │
│  │      private Set<Org> children = new HashSet<>();                  │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 七、审计日志

### 7.1 登录日志

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          登录日志设计                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  LoginLogService 登录日志服务                                     │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Service                                                       │    │
│  │  public class LoginLogService {                                   │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private LoginLogRepository loginLogRepository;               │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 记录登录成功                                            │    │
│  │       */                                                        │    │
│  │      public void logSuccess(String username, LoginRequest request) {│ │
│  │          LoginLog log = new LoginLog();                          │    │
│  │          log.setUsername(username);                              │    │
│  │          log.setIp(getClientIp(request));                       │    │
│  │          log.setLoginLocation(getLocationByIp(log.getIp()));    │    │
│  │          log.setBrowser(getBrowser(request));                    │    │
│  │          log.setOs(getOs(request));                             │    │
│  │          log.setStatus("success");                              │    │
│  │          log.setMsg("登录成功");                                 │    │
│  │          log.setLoginTime(LocalDateTime.now());                  │    │
│  │                                                                   │    │
│  │          loginLogRepository.save(log);                           │    │
│  │                                                                   │    │
│  │          // 异步更新用户登录信息                                 │    │
│  │          updateUserLoginInfo(username, log.getIp());            │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      /**                                                        │    │
│  │       * 记录登录失败                                            │    │
│  │       */                                                        │    │
│  │      public void logFail(String username, LoginRequest request,  │    │
│  │                         String reason) {                        │    │
│  │          LoginLog log = new LoginLog();                          │    │
│  │          log.setUsername(username);                              │    │
│  │          log.setIp(getClientIp(request));                       │    │
│  │          log.setLoginLocation(getLocationByIp(log.getIp()));    │    │
│  │          log.setBrowser(getBrowser(request));                    │    │
│  │          log.setOs(getOs(request));                             │    │
│  │          log.setStatus("fail");                                 │    │
│  │          log.setMsg(reason);                                     │    │
│  │          log.setLoginTime(LocalDateTime.now());                  │    │
│  │                                                                   │    │
│  │          loginLogRepository.save(log);                           │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private void updateUserLoginInfo(String username, String ip) {│  │
│  │          userRepository.findByUsername(username)                │    │
│  │              .ifPresent(user -> {                               │    │
│  │                  user.setLoginIp(ip);                           │    │
│  │                  user.setLoginDate(LocalDateTime.now());         │    │
│  │                  user.setLoginCount(user.getLoginCount() + 1);   │    │
│  │                  userRepository.save(user);                      │    │
│  │              });                                                 │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      private String getClientIp(HttpServletRequest request) {   │    │
│  │          String ip = request.getHeader("X-Forwarded-For");      │    │
│  │          if (ip == null || ip.isEmpty() || "unknown".equals(ip)) {│ │
│  │              ip = request.getHeader("Proxy-Client-IP");        │    │
│  │          }                                                       │    │
│  │          if (ip == null || ip.isEmpty() || "unknown".equals(ip)) {│ │
│  │              ip = request.getHeader("WL-Proxy-Client-IP");      │    │
│  │          }                                                       │    │
│  │          if (ip == null || ip.isEmpty() || "unknown".equals(ip)) {│ │
│  │              ip = request.getHeader("HTTP_CLIENT_IP");          │    │
│  │          }                                                       │    │
│  │          if (ip == null || ip.isEmpty() || "unknown".equals(ip)) {│ │
│  │              ip = request.getHeader("HTTP_X_FORWARDED_FOR");    │    │
│  │          }                                                       │    │
│  │          if (ip == null || ip.isEmpty() || "unknown".equals(ip)) {│ │
│  │              ip = request.getRemoteAddr();                      │    │
│  │          }                                                       │    │
│  │          return ip.split(",")[0].trim();                         │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 八、API 接口

### 8.1 认证接口

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          认证 API                                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  AuthController 认证接口                                          │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @RestController                                                │    │
│  │  @RequestMapping("/api/v1/auth")                                  │    │
│  │  @Api(tags = "认证管理")                                         │    │
│  │  public class AuthController {                                    │    │
│  │                                                                   │    │
│  │      @Autowired                                                  │    │
│  │      private AuthService authService;                             │    │
│  │                                                                   │    │
│  │      @PostMapping("/login")                                      │    │
│  │      @ApiOperation("用户登录")                                    │    │
│  │      public Result<LoginResponse> login(                         │    │
│  │          @RequestBody @Valid LoginRequest request) {             │    │
│  │          LoginResponse response = authService.login(request);    │    │
│  │          return Result.success(response);                        │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PostMapping("/refresh")                                    │    │
│  │      @ApiOperation("刷新Token")                                  │    │
│  │      public Result<LoginResponse> refresh(                        │    │
│  │          @RequestParam String refreshToken) {                    │    │
│  │          LoginResponse response = authService.refreshToken(refreshToken);│  │
│  │          return Result.success(response);                        │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PostMapping("/logout")                                     │    │
│  │      @ApiOperation("退出登录")                                    │    │
│  │      public Result<Void> logout() {                              │    │
│  │          String username = UserContextHolder.getUsername();       │    │
│  │          authService.logout(username);                            │    │
│  │          return Result.success();                                │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping("/me")                                          │    │
│  │      @ApiOperation("获取当前用户信息")                            │    │
│  │      public Result<UserInfo> getCurrentUser() {                  │    │
│  │          String userId = UserContextHolder.getUserId();          │    │
│  │          User user = userService.getById(userId);                │    │
│  │          return Result.success(toUserInfo(user));                │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 8.2 用户管理接口

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          用户管理 API                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  UserController 用户管理接口                                       │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @RestController                                                │    │
│  │  @RequestMapping("/api/v1/users")                                 │    │
│  │  @Api(tags = "用户管理")                                         │    │
│  │  @PreAuthorize("@ss.hasPermi('system:user:list')")               │    │
│  │  public class UserController {                                    │    │
│  │                                                                   │    │
│  │      @PostMapping                                                │    │
│  │      @ApiOperation("创建用户")                                    │    │
│  │      public Result<UserDTO> create(@RequestBody @Valid UserCreateRequest req) {│  │
│  │          User user = userService.createUser(toUser(req));         │    │
│  │          return Result.success(toDTO(user));                     │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PutMapping("/{id}")                                        │    │
│  │      @ApiOperation("更新用户")                                    │    │
│  │      public Result<UserDTO> update(@PathVariable String id,       │    │
│  │                                    @RequestBody UserUpdateRequest req) {│  │
│  │          User user = userService.updateUser(id, req);             │    │
│  │          return Result.success(toDTO(user));                     │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @DeleteMapping("/{id}")                                      │    │
│  │      @ApiOperation("删除用户")                                    │    │
│  │      public Result<Void> delete(@PathVariable String id) {       │    │
│  │          userService.deleteUser(id);                              │    │
│  │          return Result.success();                                │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping("/{id}")                                         │    │
│  │      @ApiOperation("获取用户详情")                                │    │
│  │      public Result<UserDTO> getById(@PathVariable String id) {    │    │
│  │          User user = userService.getById(id);                     │    │
│  │          return Result.success(toDTO(user));                     │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @GetMapping                                                 │    │
│  │      @ApiOperation("分页查询用户")                                │    │
│  │      public Result<Page<UserDTO>> list(UserQuery query) {        │    │
│  │          Page<User> page = userService.pageQuery(query);         │    │
│  │          return Result.success(page.map(this::toDTO));            │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PutMapping("/{id}/password")                                │    │
│  │      @ApiOperation("修改密码")                                    │    │
│  │      public Result<Void> changePassword(                          │    │
│  │          @PathVariable String id,                                │    │
│  │          @RequestBody @Valid PasswordChangeDTO dto) {           │    │
│  │          userService.changePassword(id, dto.getOldPassword(),    │    │
│  │              dto.getNewPassword());                              │    │
│  │          return Result.success();                                │    │
│  │      }                                                           │    │
│  │                                                                   │    │
│  │      @PutMapping("/{id}/status")                                  │    │
│  │      @ApiOperation("修改用户状态")                                │    │
│  │      public Result<Void> updateStatus(                           │    │
│  │          @PathVariable String id,                                │    │
│  │          @RequestParam String status) {                          │    │
│  │          userService.updateStatus(id, status);                   │    │
│  │          return Result.success();                                │    │
│  │      }                                                           │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 九、API 接口

### 10.1 数据权限概述

系统支持基于组织架构的数据权限控制，通过 `DataScope` 注解和 `DataScopeAspect` 切面实现。

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          数据权限控制                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  数据权限范围类型                                                 │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  • 1: 全部数据权限                                              │    │
│  │  • 2: 自定义数据权限                                            │    │
│  │  • 3: 本部门数据权限                                            │    │
│  │  • 4: 本部门及以下数据权限                                      │    │
│  │  • 5: 仅本人数据权限                                            │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  DataScope 注解                                                  │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @DataScope(deptAlias = "u", userAlias = "u")                   │    │
│  │                                                                   │    │
│  │  注解参数说明：                                                  │    │
│  │  • deptAlias: 部门表别名                                        │    │
│  │  • userAlias: 用户表别名                                        │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 十一、日志管理

### 11.1 登录日志

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          登录日志设计                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  LoginLogController 登录日志接口                                  │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Tag(name = "登录日志")                                         │    │
│  │  @RestController                                                │    │
│  │  @RequestMapping("/api/v1/login-logs")                            │    │
│  │  public class LoginLogController {                                │    │
│  │                                                                   │    │
│  │      @GetMapping                                                │    │
│  │      public Result<IPage<LoginLogDTO>> pageQuery(LoginLogQuery query)│  │
│  │                                                                   │    │
│  │      @OperationLog("清空登录日志")                               │    │
│  │      @DeleteMapping("/clean")                                   │    │
│  │      public Result<Void> clean()                                │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 11.2 审计日志

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          审计日志设计                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  AuditLogController 审计日志接口                                 │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  @Tag(name = "审计日志")                                         │    │
│  │  @RestController                                                │    │
│  │  @RequestMapping("/api/v1/audit-logs")                           │    │
│  │  public class AuditLogController {                               │    │
│  │                                                                   │    │
│  │      @GetMapping                                                │    │
│  │      public Result<IPage<AuditLogDTO>> pageQuery(AuditLogQuery query)│ │
│  │                                                                   │    │
│  │      @GetMapping("/{id}")                                        │    │
│  │      public Result<AuditLogDTO> getById(@PathVariable Long id)   │    │
│  │  }                                                               │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  操作日志切面 OperationLogAspect                                 │    │
│  │  ───────────────────────────────────────────────────────────────│    │
│  │                                                                   │    │
│  │  • 自动记录带有 @OperationLog 注解的方法                         │    │
│  │  • 记录操作人、操作时间、操作类型、请求参数、返回结果            │    │
│  │  • 记录执行时间，用于性能监控                                    │    │
│  │                                                                   │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 十二、API 接口总览

### 12.1 认证接口

| 接口 | 方法 | 路径 | 描述 |
|------|------|------|------|
| 登录 | POST | /api/v1/auth/login | 用户登录 |
| 刷新Token | POST | /api/v1/auth/refresh | 刷新访问令牌 |
| 登出 | POST | /api/v1/auth/logout | 用户登出 |
| 获取当前用户 | GET | /api/v1/auth/me | 获取当前登录用户信息 |

### 12.2 用户管理接口

| 接口 | 方法 | 路径 | 描述 |
|------|------|------|------|
| 创建用户 | POST | /api/v1/users | 创建新用户 |
| 更新用户 | PUT | /api/v1/users/{id} | 更新用户信息 |
| 删除用户 | DELETE | /api/v1/users/{id} | 删除用户 |
| 获取用户详情 | GET | /api/v1/users/{id} | 获取用户详细信息 |
| 分页查询用户 | GET | /api/v1/users | 分页查询用户列表 |
| 修改密码 | PUT | /api/v1/users/{id}/password | 用户修改密码 |
| 重置密码 | PUT | /api/v1/users/{id}/password/reset | 管理员重置密码 |
| 修改用户状态 | PUT | /api/v1/users/{id}/status | 启用/禁用用户 |
| 分配角色 | PUT | /api/v1/users/{id}/roles | 为用户分配角色 |
| 获取用户角色 | GET | /api/v1/users/{id}/roles | 获取用户的角色列表 |

### 12.3 角色管理接口

| 接口 | 方法 | 路径 | 描述 |
|------|------|------|------|
| 创建角色 | POST | /api/v1/roles | 创建新角色 |
| 更新角色 | PUT | /api/v1/roles/{id} | 更新角色信息 |
| 删除角色 | DELETE | /api/v1/roles/{id} | 删除角色 |
| 获取角色详情 | GET | /api/v1/roles/{id} | 获取角色详细信息 |
| 分页查询角色 | GET | /api/v1/roles | 分页查询角色列表 |
| 分配菜单权限 | PUT | /api/v1/roles/{id}/menus | 分配菜单权限 |
| 获取菜单权限 | GET | /api/v1/roles/{id}/menus | 获取角色菜单权限 |

### 12.4 权限管理接口

| 接口 | 方法 | 路径 | 描述 |
|------|------|------|------|
| 获取菜单树 | GET | /api/v1/permissions/menus | 获取菜单树 |
| 获取用户权限 | GET | /api/v1/permissions/user | 获取当前用户权限 |

### 12.5 组织管理接口

| 接口 | 方法 | 路径 | 描述 |
|------|------|------|------|
| 获取组织树 | GET | /api/v1/orgs/tree | 获取组织架构树 |

### 12.6 日志接口

| 接口 | 方法 | 路径 | 描述 |
|------|------|------|------|
| 分页查询登录日志 | GET | /api/v1/login-logs | 分页查询登录日志 |
| 清空登录日志 | DELETE | /api/v1/login-logs/clean | 清空登录日志 |
| 分页查询审计日志 | GET | /api/v1/audit-logs | 分页查询审计日志 |
| 获取审计日志详情 | GET | /api/v1/audit-logs/{id} | 获取审计日志详情 |

---

## 十三、版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v1.0 | 2026-07-10 | 初始版本 |
| v1.1 | 2026-07-15 | 修正认证方案为 SaToken，更新目录结构，新增数据权限和日志管理章节 |

---

**文档版本：v1.1**
**最后更新：2026-07-15**
