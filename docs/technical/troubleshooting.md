# 排错记录（Troubleshooting）

> 记录项目开发过程中遇到的典型启动错误、根因、修复方案与后续建议，方便后续成员快速定位类似问题。

---

## 2026-07-19：meowflow-template 启动失败

### 现象 3：`AIInvokeLogMapper` Bean 缺失

**错误日志**

```
UnsatisfiedDependencyException: Error creating bean with name 'AICallLoggingAspect'
defined in file [...AICallLoggingAspect.class]: Unsatisfied dependency expressed
through constructor parameter 0: Error creating bean with name 'AIInvokeLogService':
Unsatisfied dependency expressed through field 'baseMapper':
No qualifying bean of type 'com.meowflow.infra.service.AIInvokeLogMapper' available
```

**根因**

`meowflow-template` 通过 pom 依赖了 `meowflow-infra`，但 `@MapperScan` 范围只覆盖了 `com.meowflow.template.repository`。infra 的 4 个 Mapper（`AIInvokeLogMapper` / `DocumentMapper` / `ChunkMapper` / `KnowledgeBaseMapper`）分布在 `com.meowflow.infra.service` 和 `com.meowflow.infra.persistence.mapper` 两个包下，都不在 template 的 `@MapperScan` 范围内，因此不会被注册为 MyBatis Bean。

同时，`@SpringBootApplication(scanBasePackages = "com.meowflow")` 会把 `com.meowflow.infra` 下所有 `@Component` 拉进容器（包括 `AIInvokeLogService`、`AICallLoggingAspect` 等），导致 Service Bean 创建时发现 `baseMapper` 缺失，连锁报错。

infra 模块本身没有启动类（`MeowflowInfraApplication` 是空标记类），没有 `META-INF/spring.factories`，也没有 `@MapperScan`，它的 Mapper 设计上是由**使用 infra 的业务模块**显式注册。

**修复**

将 `meowflow-template` 的 `@MapperScan` 扩展为多包：

```java
@MapperScan({
    "com.meowflow.template.repository",
    "com.meowflow.infra.service",
    "com.meowflow.infra.persistence.mapper"
})
```

---

### 现象 1：`IdGenerator` Bean 找不到

**错误日志**

```
UnsatisfiedDependencyException: Error creating bean with name 'templateCategoryController'
defined in file [...TemplateCategoryController.class]: Unsatisfied dependency expressed
through constructor parameter 0: Error creating bean with name 'templateCategoryService':
... No qualifying bean of type 'com.meowflow.common.util.IdGenerator' available
```

**根因**

`meowflow-template` 下 4 个 Service（`TemplateService`、`TemplateCategoryService`、`TemplateRatingService`、`TemplateReviewService`）都通过 `@RequiredArgsConstructor` 构造注入了 `com.meowflow.common.util.IdGenerator`。但 `IdGenerator` 类本身只带 `@Slf4j`，**不是 Spring Bean**。

`meowflow-common` 里提供了 `IdGeneratorFactory`（带 `@Component`），它**只在内部 `new IdGenerator(...)`** 并通过 `getIdGenerator()` 暴露实例，从未把 `IdGenerator` 自身注册为可注入的 Bean。

其它模块（如 `meowflow-executor`）通过在主类 `@Import({IdGeneratorFactory.class, ...})` 并显式注入 `IdGeneratorFactory` 来规避这个问题，但 template 模块走的是构造注入 `IdGenerator` 的写法，因此启动失败。

**修复**

新增 `meowflow-common/src/main/java/com/meowflow/common/util/IdGeneratorConfig.java`：

```java
@Configuration
public class IdGeneratorConfig {
    @Bean
    public IdGenerator idGenerator() {
        long workerId = WorkerIdAssigner.getWorkerIdOrDefault(0);
        return new IdGenerator(workerId);
    }
}
```

`MeowFlowTemplateApplication` 已 `@ComponentScan("com.meowflow.common")`，无需任何额外配置，`IdGenerator` 就会被注册为可注入的 Bean，4 个 Service 的构造注入能正常工作。

**影响**

- 4 个 Service 零改动
- `IdGeneratorFactory` 完整保留（`meowflow-executor`、`LogPersistenceService` 仍可继续使用）
- workerId 在 Bean 初始化阶段通过 `getWorkerIdOrDefault(0)` 获取，若 `WorkerIdAssigner` 未运行则回落为 0，行为与 `IdGeneratorFactory.@PostConstruct` 一致

---

### 现象 2：启动时 `meowflow.crypto.aes-key` 缺失

**错误日志**

```
BeanCreationException: Error creating bean with name 'AESEncryptor':
Injection of autowired dependencies failed
Caused by: IllegalArgumentException: Could not resolve placeholder
'meowflow.crypto.aes-key' in value "${meowflow.crypto.aes-key}"
```

**根因**

`com.meowflow.common.security.AESEncryptor` 同时带有 `@Component` 和被 `CryptoConfig.aesEncryptor()` 注册为 `@Bean`。template 模块的 `@ComponentScan("com.meowflow.common")` 把它作为 `@Component` 拉进容器，触发了 `@Value("${meowflow.crypto.aes-key}")` 的解析，而 template 的 `application.yml` 和 `meowflow-common` 的 `application.yml` 都没有提供这个 key。

注意：即便 template 模块没有 Service 直接使用 `AESEncryptor`，只要扫到 `@Component`，Spring 在 bean 生命周期就**必须**解析 `@Value`，配置缺失直接抛错。

**修复**

新增 `meowflow-template/src/main/resources/application-dev.yml`：

```yaml
meowflow:
  crypto:
    aes-key: ${MEOWFLOW_CRYPTO_AES_KEY:m3dLu8JrJ22Jr0PJbM215/N/I4KOfjmNqzhHmJGav80=}
    key-id: dev-local
```

dev profile 已激活（`application.yml` 中 `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:dev}`），dev 默认值生效；生产部署通过 `MEOWFLOW_CRYPTO_AES_KEY` 环境变量或 Nacos 覆盖默认密钥。

**key 生成方式**

```bash
# 方式一：openssl
openssl rand -base64 32

# 方式二：JDK jshell
jshell --execution=local - <<EOF
import java.util.Base64;
import java.security.SecureRandom;
System.out.println(Base64.getEncoder().encodeToString(
  SecureRandom.getInstanceStrong().generateSeed(32)));
/exit
EOF
```

---

### 现象 4：`ChatModelRouter` 找不到唯一的 `CircuitBreakerRegistry`

**错误日志**

```
APPLICATION FAILED TO START

Parameter 1 of constructor in com.meowflow.infra.chat.ChatModelRouter
required a single bean, but 2 were found:
    - circuitBreakerRegistry: defined by method 'circuitBreakerRegistry'
      in class path resource [com/meowflow/common/config/UnifiedCircuitBreakerConfig.class]
    - aiCircuitBreakerRegistry: defined by method 'aiCircuitBreakerRegistry'
      in class path resource [com/meowflow/common/config/UnifiedCircuitBreakerConfig.class]
```

**根因**

`meowflow-common` 的 `UnifiedCircuitBreakerConfig` 同时暴露了两个 `CircuitBreakerRegistry` Bean：
- `circuitBreakerRegistry`：通用熔断器（任何模块可用）
- `aiCircuitBreakerRegistry`：AI 专用熔断器（被 `AICircuitBreakerRegistryHolder` 正确使用）

`ChatModelRouter` 是 AI 路由组件，本应使用 AI 专用 Registry，但它通过 `@RequiredArgsConstructor` 注入了 `private final CircuitBreakerRegistry circuitBreakerFactory;`，**没有加 `@Qualifier`**。Lombok 生成的构造器也没有 `@Qualifier`，Spring 找不到单一匹配 Bean，启动失败。

对照 `AICircuitBreakerRegistryHolder` 的写法（`@Qualifier("aiCircuitBreakerRegistry")` + `@Autowired(required = false)`）才是正确模式。

**修复**

替换 `ChatModelRouter` 的 Lombok 自动构造器为手写构造器，并在参数上加 `@Qualifier`：

```java
@Slf4j
@Component("chatModelRouter")
public class ChatModelRouter {

    private final Map<String, ChatClient> chatClients;
    private final CircuitBreakerRegistry circuitBreakerFactory;

    public ChatModelRouter(
            Map<String, ChatClient> chatClients,
            @Qualifier("aiCircuitBreakerRegistry") CircuitBreakerRegistry circuitBreakerFactory) {
        this.chatClients = chatClients;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }
    // ... 原有方法不变 ...
}
```

**影响**

- `ChatModelRouter` 改为明确注入 `aiCircuitBreakerRegistry`，与 `AICircuitBreakerRegistryHolder` 行为一致
- 已有单元测试（`ChatModelRouterTest` / `ChatModelRouterExtendedTest`）只 mock `CircuitBreakerRegistry`，无需改动

---

### 现象 5：`circuitBreakerRegistry` 找不到唯一的 `CircuitBreakerConfig`

**错误日志**

```
APPLICATION FAILED TO START

Parameter 0 of method circuitBreakerRegistry
in com.meowflow.common.config.UnifiedCircuitBreakerConfig
required a single bean, but 2 were found:
    - llmCircuitBreakerConfig: defined by method 'llmCircuitBreakerConfig'
      in class path resource [com/meowflow/common/config/Resilience4jConfig.class]
    - defaultCircuitBreakerConfig: defined by method 'defaultCircuitBreakerConfig'
      in class path resource [com/meowflow/common/config/UnifiedCircuitBreakerConfig.class]
```

**根因**

`meowflow-common` 里有两个独立的 `@Configuration` 类同时注册了 `CircuitBreakerConfig`：

- `Resilience4jConfig.llmCircuitBreakerConfig()` —— 顾名思义是为 LLM 调用准备的（参数与通用 Config 完全相同）
- `UnifiedCircuitBreakerConfig.defaultCircuitBreakerConfig()` —— 给通用 `circuitBreakerRegistry` 用

二者参数完全一致，且 `llmCircuitBreakerConfig` **没有任何消费方引用**（仅靠 Bean 名 `llmCircuitBreakerConfig` 自我暴露）。`circuitBreakerRegistry(CircuitBreakerConfig defaultCircuitBreakerConfig)` 通过参数名解析，但 Spring 看到 2 个同类型 Bean 直接报歧义。

这是上一条"现象 4"修复后冒出的下一层问题——把注意力引到 common 的熔断器配置后，发现 common 模块自身已经存在配置冗余。

**修复**

两步：

1. **删除孤儿 Bean `llmCircuitBreakerConfig`**，整个 `Resilience4jConfig` 只保留 `apiRateLimiterConfig`，并把 `CircuitBreakerConfig` 的 import 一并清掉；同时把类上的 `@ConditionalOnClass(CircuitBreakerConfig.class)` 改为 `@ConditionalOnClass(RateLimiterConfig.class)`，更准确反映该类职责（限流）

2. **给 `circuitBreakerRegistry` 注入加 `@Qualifier("defaultCircuitBreakerConfig")` 显式化**——即便后续又新增了别的 Config 也不会再起冲突

```java
// UnifiedCircuitBreakerConfig.java
@Bean
@ConditionalOnProperty(name = "meowflow.circuit-breaker.enabled", havingValue = "true", matchIfMissing = true)
public CircuitBreakerRegistry circuitBreakerRegistry(
        @Qualifier("defaultCircuitBreakerConfig") CircuitBreakerConfig defaultCircuitBreakerConfig) {
    log.info("初始化统一熔断器 Registry");
    return CircuitBreakerRegistry.of(defaultCircuitBreakerConfig);
}
```

```java
// Resilience4jConfig.java（精简后）
@ConditionalOnClass(RateLimiterConfig.class)
@Configuration
public class Resilience4jConfig {

    @Bean
    public RateLimiterConfig apiRateLimiterConfig() {
        return RateLimiterConfig.custom()
                .limitForPeriod(100)
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
    }
}
```

**验证**

全仓搜索 `CircuitBreakerConfig` 的消费方仅剩 `UnifiedCircuitBreakerConfig.circuitBreakerRegistry(...)`（参数名 `defaultCircuitBreakerConfig`）一处，`llmCircuitBreakerConfig` 没有任何 `@Qualifier` 引用，可以安全删除。

---

## 后续建议

按风险与工作量排序：

### 优先级 P0：必修项

1. **统一各业务模块的 `meowflow.crypto.aes-key` 配置** ✅ 已完成
   - 已将默认值配置迁移到 `meowflow-common/src/main/resources/common-defaults.yml`
   - 各业务模块通过 `spring.config.import: optional:classpath:common-defaults.yml` 导入公共配置
   - 生产环境通过环境变量 `MEOWFLOW_CRYPTO_AES_KEY` 或 Nacos 覆盖默认值
   - 已更新模块：workflow、user、monitor、executor、gateway

2. **修复 `AESEncryptor` 的双重注册** ✅ 已完成
   - `AESEncryptor` 类本身没有 `@Component`，仅通过 `CryptoConfig.@Bean` 注册
   - `CryptoConfig` 有 `@ConditionalOnProperty` 控制，仅在配置存在时创建 Bean
   - 此问题在之前修复中已解决

3. **审视 template 与 infra 的依赖关系** ✅ 已完成
   - 已从 `meowflow-template/pom.xml` 中移除 `meowflow-infra` 依赖
   - `MeowFlowTemplateApplication` 不再需要 `@Import(InfraMapperAutoConfiguration.class)`
   - template 模块不再拉入 infra 的所有 Bean

4. **统一 infra 模块的 Mapper 扫描入口** ✅ 已完成
   - 已在 `meowflow-infra/src/main/java/com/meowflow/infra/config/InfraMapperAutoConfiguration.java` 新增 `@MapperScan` 配置类
   - 已新增 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 自动装配声明
   - `meowflow-template` 和 `meowflow-workflow` 已改用 `@Import(InfraMapperAutoConfiguration.class)` 方式引入
   - 使用方无需再手动维护 Mapper 列表

5. **`ChatModelRouter` 与 `AICircuitBreakerRegistryHolder` 的耦合方式不一致** ✅ 已完成
   - `ChatModelRouter` 已在构造器中添加 `@Qualifier("aiCircuitBreakerRegistry")`，与 `AICircuitBreakerRegistryHolder` 保持一致

6. **`UnifiedCircuitBreakerConfig` 两个 Registry 边界约定** ✅ 已完成
   - 已在 `UnifiedCircuitBreakerConfig` 中添加详细注释说明两个 Registry 的用途
   - `aiCircuitBreakerRegistry` 已标记 `@Primary`，AI 组件默认使用此 Registry
   - `circuitBreakerRegistry` 标记为通用熔断器，预留给 HTTP/DB 限流等场景

7. **建立"未消费 Bean 巡检"机制** ✅ 已完成
   - 已创建 `OrphanBeanScannerTest` 测试类，检测未消费的孤儿 Bean
   - 已创建 `BaseIntegrationTest` 基类，支持集成测试

### 优先级 P1：改进项

1. **`WorkerIdAssigner` 改造为真正可用的 workerId 分配** ✅ 已完成
   - 已删除 `IdGeneratorConfig`，Service 直接依赖 `IdGeneratorFactory`
   - `IdGeneratorFactory` 在 `@PostConstruct` 中获取 workerId，此时 `WorkerIdAssigner` 已完成初始化
   - `WorkerIdAssigner` 增加主机名哈希 fallback，Redis 不可用时仍可启动

2. **限定 `meowflow-template` 的 `@ComponentScan` 范围** ✅ 已完成
   - 已移除 `@SpringBootApplication(scanBasePackages = "com.meowflow")` 中的 scanBasePackages
   - 使用 `@ComponentScan` 的 `excludeFilters` 排除其他模块的包
   - 添加了详细的类级别注释说明扫描范围

3. **抽离开发/生产配置约定**
   - 把"dev 默认值 + 环境变量覆盖"这一模式写入 `docs/technical/modules/common.md` 或新建 `docs/technical/configuration.md`
   - 给出模板片段，方便后续模块直接复制

### 优先级 P2：可选项

1. **`AESEncryptor` 解耦静态调用** ✅ 已完成
   - 已将 `setEncryptor` 调用统一到 `CryptoConfig.typeHandlerInitializer`
   - 使用 `@Order(100)` 的 `ApplicationRunner` 在容器启动后注入
   - 不再需要业务模块手动调用 `setEncryptor`，遵循 Spring 容器生命周期

2. **WorkerIdAssigner 失败回退** ✅ 已完成
   - 已在 `WorkerIdAssigner.assignFallbackWorkerId()` 中实现主机名哈希 fallback
   - Redis 不可用时使用本地策略分配 workerId，确保应用可以启动

3. **新增 CI 启动冒烟测试** ✅ 已完成
   - 已在 `meowflow-template` 模块创建 `TemplateSmokeTest` 测试类
   - 验证主类能否成功创建 Spring 上下文

---

## 相关文档

- `docs/technical/modules/common.md`：公共配置约定、IdGeneratorFactory 使用说明
- `docs/technical/field-encryption.md`：配置与注意事项
- `docs/technical/BACKEND_WORK_BREAKDOWN.md`：模块划分与依赖关系
- `docs/technical/architecture.md`：模块依赖与调用关系（template / infra / common）