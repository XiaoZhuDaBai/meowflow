# 喵流 (MeowFlow) - 集成测试规范

> 本文档定义集成测试的编写规范、Testcontainers 使用、Spring Boot Test 注解与数据初始化策略

---

## 一、集成测试定义

> **集成测试**：验证多个组件协作的测试，包括 Service + Repository + DB、Service + MQ、Service + 外部 HTTP API。

### 1.1 集成测试 vs 单元测试

| 维度 | 单元测试 | 集成测试 |
|------|---------|---------|
| 启动 Spring | ❌ 否 | ✅ 是（部分或全部） |
| 真实数据库 | ❌ 否（H2/Mock） | ✅ 是（Testcontainers） |
| 执行速度 | 毫秒级 | 秒级 ~ 分钟级 |
| 隔离性 | 完全隔离 | 部分隔离（共享 Spring 容器） |
| 编写成本 | 低 | 中 |
| 调试难度 | 低 | 中 |

### 1.2 当前项目集成测试现状

| 模块 | 集成测试基类 | 数据库策略 |
|------|------------|-----------|
| **meowflow-workflow** | `BaseServiceTest`（Testcontainers PG + H2 application-test.yml） | 双策略 |
| **meowflow-executor** | `BaseExecutorServiceTest`（Testcontainers PG） | Testcontainers |
| **meowflow-infra** | 无基类，使用 application-test.yml | H2 |
| **meowflow-template** | 无基类 | - |
| **meowflow-user / monitor / common** | 无 | **缺失** |

---

## 二、Spring Boot Test 注解

### 2.1 常用注解

| 注解 | 用途 | 示例 |
|------|------|------|
| `@SpringBootTest` | 加载完整 Spring 上下文 | `@SpringBootTest` |
| `@SpringBootTest(webEnvironment = ...)` | 指定 Web 环境 | `MOCK`, `RANDOM_PORT`, `NONE` |
| `@WebMvcTest(XxxController.class)` | 仅加载 Controller 层（用于 MockMvc 测试） | `@WebMvcTest(WorkflowController.class)` |
| `@DataJpaTest` | 仅加载 JPA Repository 层 | `@DataJpaTest` |
| `@TestConfiguration` | 测试专用配置 | `@TestConfiguration public class TestConfig` |
| `@ActiveProfiles("test")` | 激活 test profile | - |
| `@AutoConfigureMockMvc` | 自动配置 MockMvc | - |
| `@MockBean` | 在 Spring 上下文中替换 Bean 为 Mock | `@MockBean private ExternalApi api;` |

### 2.2 @SpringBootTest 实战

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class WorkflowServiceIntegrationTest extends BaseServiceTest {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowRepository workflowRepository;

    @BeforeEach
    void setUp() {
        workflowRepository.deleteAll();  // 清理数据
    }

    @Test
    void testCreateAndRetrieveWorkflow() {
        // Given
        WorkflowCreateRequest request = new WorkflowCreateRequest();
        request.setName("Integration Test Workflow");

        // When
        WorkflowResponse created = workflowService.create(request, 1L);

        // Then
        assertNotNull(created.getId());
        WorkflowResponse retrieved = workflowService.getById(created.getId());
        assertEquals("Integration Test Workflow", retrieved.getName());
    }
}
```

### 2.3 webEnvironment 选项

| 选项 | 用途 | 启动 Tomcat | 默认端口 |
|------|------|------------|---------|
| `MOCK` | 默认，使用 MockMvc | ❌ | - |
| `RANDOM_PORT` | 启动真实 Tomcat，端口随机 | ✅ | 随机 |
| `DEFINED_PORT` | 使用配置文件中的端口 | ✅ | 8080 |
| `NONE` | 不启动 Web 环境（纯 Service 测试） | ❌ | - |

**推荐**：
- Service 集成测试 → `NONE`
- Controller 测试 → `MOCK`（使用 MockMvc）
- 端到端 HTTP 测试 → `RANDOM_PORT`（使用 TestRestTemplate）

---

## 三、Testcontainers 使用规范

### 3.1 Testcontainers 简介

Testcontainers 是一个 Java 库，可以在 Docker 容器中运行真实的数据库、消息队列、缓存等，为集成测试提供**真实的外部依赖**。

### 3.2 项目依赖

父 POM 已配置 Testcontainers BOM：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers-bom</artifactId>
            <version>1.19.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

各模块 pom.xml 中按需引入：

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

### 3.3 基本使用模式

#### 3.3.1 PostgreSQL Container（项目标准）

```java
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class BaseServiceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("meowflow_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        postgres.start();  // 显式启动（推荐）
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }
}
```

**关键点**：
- `@Container` + `static` → 整个测试类共享一个容器（启动一次）
- `@DynamicPropertySource` → 将容器属性注入到 Spring 上下文
- `postgres.start()` 在静态方法中调用，确保容器在 Spring 启动前运行

#### 3.3.2 Redis Container（如需）

```java
@Container
static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

@DynamicPropertySource
static void redisProperties(DynamicPropertyRegistry registry) {
    redis.start();
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
}
```

#### 3.3.3 RabbitMQ Container（如需）

```java
@Container
static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3-management");

@DynamicPropertySource
static void rabbitProperties(DynamicPropertyRegistry registry) {
    rabbitmq.start();
    registry.add("spring.rabbitmq.host", rabbitmq::getHost);
    registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
    registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
}
```

### 3.4 容器复用（Ryuk 与 Testcontainers Cloud）

**当前配置每次启动都创建新容器**。在大型测试套件中，建议：

```xml
<!-- pom.xml 中启用容器复用 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <systemPropertyVariables>
            <reuseContainers>true</reuseContainers>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

### 3.5 容器生命周期注解

| 注解 | 行为 |
|------|------|
| `@Container` | 标记容器字段，由 JUnit 5 扩展管理生命周期 |
| 不加 `@Container` | 需要手动 `.start()` / `.stop()` |

### 3.6 数据库迁移（Flyway）

如果生产环境使用 Flyway，在 Testcontainer 中应保持一致：

```java
@DynamicPropertySource
static void properties(DynamicPropertyRegistry registry) {
    postgres.start();
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    // 启用 Flyway 跑迁移
    registry.add("spring.flyway.enabled", () -> "true");
    registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");  // 验证 schema
}
```

---

## 四、数据初始化与清理

### 4.1 每个测试前清理（@BeforeEach）

```java
@BeforeEach
void setUp() {
    workflowRepository.deleteAll();
    versionRepository.deleteAll();
    userRepository.deleteAll();
}
```

**优点**：测试间完全隔离
**缺点**：每个测试都执行清理，慢

### 4.2 测试数据 Fixtures

```java
@BeforeEach
void setUp() {
    // 清理
    workflowRepository.deleteAll();

    // 准备通用数据
    testUser = userRepository.save(User.builder()
            .id(1L)
            .name("Test User")
            .build());

    testWorkflow = workflowRepository.save(Workflow.builder()
            .name("Base Workflow")
            .ownerId(testUser.getId())
            .build());
}

@Test
void testXxx() {
    // testUser 和 testWorkflow 已存在
    // ...
}
```

### 4.3 SQL 脚本（src/test/resources/sql/）

```sql
-- src/test/resources/sql/cleanup.sql
DELETE FROM wf_execution WHERE workflow_id LIKE 'test-%';
DELETE FROM workflow WHERE id LIKE 'test-%';
```

加载方式：

```java
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Test
void testXxx() {
    // ...
}
```

### 4.4 事务回滚（适合 Service 层）

```java
@SpringBootTest
@Transactional  // 每个测试自动回滚
class WorkflowServiceTransactionalTest {

    @Test
    void testXxx() {
        // 数据库操作
        // 测试结束后自动回滚，无需手动清理
    }
}
```

**注意**：`@Transactional` 会影响事务行为（如 `@Transactional(propagation = REQUIRES_NEW)` 不再生效），需要谨慎使用。

---

## 五、Controller 层测试（@WebMvcTest）

### 5.1 当前现状

项目中有 **24 个 Controller**，但**没有 @WebMvcTest 测试**。这是测试覆盖的重要缺口。

### 5.2 推荐实践：使用 MockMvc

```java
@WebMvcTest(WorkflowController.class)
@AutoConfigureMockMvc(addFilters = false)  // 禁用 Sa-Token 鉴权
class WorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkflowService workflowService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/workflows - 创建工作流")
    void testCreateWorkflow() throws Exception {
        // Given
        WorkflowCreateRequest request = new WorkflowCreateRequest();
        request.setName("New Workflow");

        WorkflowResponse mockResponse = new WorkflowResponse();
        mockResponse.setId(1L);
        mockResponse.setName("New Workflow");

        when(workflowService.create(any(), eq(1L))).thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("New Workflow"));

        verify(workflowService).create(any(), eq(1L));
    }

    @Test
    @DisplayName("POST /api/workflows - 参数校验失败返回 400")
    void testCreateWorkflow_ValidationError() throws Exception {
        // Given
        WorkflowCreateRequest request = new WorkflowCreateRequest();
        request.setName("");  // 空字符串，触发 @NotBlank 校验

        // When & Then
        mockMvc.perform(post("/api/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
```

### 5.3 Controller 测试覆盖清单

| 模块 | Controller 数量 | 建议测试方法数（约） |
|------|---------------|------------------|
| **meowflow-workflow** | 4 | 30-40 |
| **meowflow-user** | 7 | 50-60 |
| **meowflow-template** | 5 | 35-45 |
| **meowflow-monitor** | 3 | 20-25 |
| **meowflow-infra** | 4 | 30-35 |
| **meowflow-executor** | 1 | 8-10 |

---

## 六、外部依赖 Mock

### 6.1 为什么要 Mock 外部依赖

- **稳定性**：避免依赖外部 API 可用性
- **速度**：避免真实 HTTP 调用耗时
- **可控性**：可以注入任意响应（包括异常）

### 6.2 Mock HTTP 服务（推荐工具）

**WireMock**（项目未引入，推荐加入）：

```xml
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock-standalone</artifactId>
    <version>3.4.1</version>
    <scope>test</scope>
</dependency>
```

```java
@SpringBootTest
class HttpNodeIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void testHttpNodeWithMockServer() {
        // Given - Stub HTTP 响应
        wireMock.stubFor(post(urlEqualTo("/api/send-email"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"success\":true}")));

        // When - 触发 HTTP 节点
        WorkflowResponse result = workflowEngine.execute(...);

        // Then - 验证调用
        wireMock.verify(postRequestedFor(urlEqualTo("/api/send-email")));
    }
}
```

### 6.3 当前 application-test.yml 的 Mock 配置

```yaml
meowflow:
  ai:
    mock: true        # 启用 Mock LLM
  notification:
    mock: true        # 启用 Mock 通知
  mcp:
    mock: true        # 启用 Mock MCP
```

**优点**：通过 `@ConditionalOnProperty` 自动切换实现
**缺点**：Mock 行为可能与真实服务有差异

---

## 七、异步与并发测试

### 7.1 Awaitility（推荐）

用于异步操作的等待：

```xml
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <version>4.2.0</version>
    <scope>test</scope>
</dependency>
```

```java
import static org.awaitility.Awaitility.await;
import java.time.Duration;

@Test
void testAsyncWorkflowExecution() {
    // When
    workflowEngine.executeAsync(executionId);

    // Then - 等待异步任务完成
    await().atMost(Duration.ofSeconds(10))
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                WorkflowExecution execution = executionRepository.findById(executionId).orElseThrow();
                assertThat(execution.getStatus()).isEqualTo("SUCCESS");
            });
}
```

### 7.2 异步线程池测试

```java
@Test
void testAsyncTaskExecution() throws Exception {
    CompletableFuture<String> future = taskService.executeAsync();

    String result = future.get(5, TimeUnit.SECONDS);
    assertThat(result).isEqualTo("expected");
}
```

---

## 八、集成测试 Checklist

编写每个集成测试时，确保：

- [ ] 使用 `@SpringBootTest` 或 `@WebMvcTest`（按测试范围）
- [ ] 使用 `@ActiveProfiles("test")` 激活 test 配置
- [ ] 使用 Testcontainers 而非 H2（对生产行为敏感的测试）
- [ ] `@BeforeEach` 清理数据，确保测试间隔离
- [ ] 使用 `application-test.yml` 启用 Mock 外部服务
- [ ] 异步测试使用 Awaitility 而非 `Thread.sleep`
- [ ] Controller 测试使用 MockMvc 而非真实 HTTP
- [ ] 验证关键的事务边界（@Transactional 回滚 / 显式 commit）
- [ ] 测试数据使用 Builder / Factory 模式构造
- [ ] 测试类继承通用基类（`BaseServiceTest` 等）

---

## 九、参考资源

- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.testing)
- [Testcontainers 官方文档](https://www.testcontainers.org/)
- [Awaitility 文档](https://github.com/awaitility/awaitility)
- [WireMock 文档](https://wiremock.org/docs/)
- 项目内参考：[`BaseServiceTest`](../../backend/meowflow/meowflow-workflow/src/test/java/com/meowflow/workflow/BaseServiceTest.java)
