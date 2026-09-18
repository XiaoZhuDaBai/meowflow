# 喵流 (MeowFlow) - 测试常见问题 FAQ

> 本文档汇总测试相关的常见问题与解决方案

---

## 一、环境与配置

### Q1：Testcontainers 启动失败怎么办？

**症状**：
```
Could not find a valid Docker environment.
```

**原因**：本地没有 Docker 环境或 Docker 未运行。

**解决方案**：
1. 安装 Docker Desktop（Windows / macOS）
2. 确保 Docker daemon 启动（Linux：`sudo systemctl start docker`）
3. 检查 `~/.docker/config.json` 认证
4. CI 环境中 GitHub Actions 已自动提供 Docker

**临时降级方案**：

```java
@Profile("!testcontainers-disabled")
@Testcontainers
class IntegrationTest { ... }
```

通过 `-Dspring.profiles.active=testcontainers-disabled` 跳过 Testcontainer 测试。

---

### Q2：H2 和 PostgreSQL 行为不一致怎么办？

**常见问题**：
- H2 不支持 `JSONB` 类型（项目使用 JSON 字段时）
- H2 不支持 `pg_trgm` / `zhparser` 全文检索
- H2 不支持 `tsvector`
- H2 不支持 PostgreSQL 特有的函数（如 `gen_random_uuid()`）

**解决方案**：
1. 对 JSONB / 全文检索测试，必须使用 Testcontainers PostgreSQL
2. 在 `application-test.yml` 中按模块切换：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/test  # 快速集成测试用 H2
    # url: jdbc:h2:mem:testdb  # 真实行为测试用 Testcontainer
```

3. 使用 `@ConditionalOnProperty` 在测试 profile 下切换

---

### Q3：测试数据库连接池耗尽怎么办？

**症状**：
```
HikariPool-1 - Connection is not available, request timed out after 30000ms.
```

**解决方案**：

```yaml
# application-test.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 5000
      idle-timeout: 300000
      max-lifetime: 600000
```

---

## 二、Mock 相关

### Q4：如何 Mock 静态方法？

**答案**：使用 Mockito 5+ 的 `mockito-inline`：

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-inline</artifactId>
    <version>5.8.0</version>
    <scope>test</scope>
</dependency>
```

```java
try (MockedStatic<IdGenerator> mocked = Mockito.mockStatic(IdGenerator.class)) {
    mocked.when(IdGenerator::nextId).thenReturn(123L);

    // 测试代码
    service.doSomething();

    mocked.verify(() -> IdGenerator.nextId(), times(1));
}
```

**注意**：静态方法 Mock 会导致测试与实现强耦合，应优先重构为依赖注入。

---

### Q5：如何验证方法被调用且参数正确？

```java
// 精确匹配
verify(repository).findById(1L);

// 任意参数
verify(repository).findById(anyLong());

// 自定义匹配器
verify(repository).save(argThat(e ->
    "test".equals(e.getName()) && e.getScore() > 0));

// 捕获参数（用于复杂验证）
ArgumentCaptor<Workflow> captor = ArgumentCaptor.forClass(Workflow.class);
verify(repository).save(captor.capture());
Workflow saved = captor.getValue();
assertThat(saved.getName()).isEqualTo("expected");
```

---

### Q6：@MockBean 和 @Mock 有什么区别？

| 注解 | 上下文 | 用途 |
|------|--------|------|
| `@Mock` | 普通 JUnit 测试 | 创建 Mock 对象（不涉及 Spring） |
| `@MockBean` | Spring Boot 测试 | 在 Spring 上下文中替换 Bean |

**使用场景**：

```java
// 纯单元测试
@ExtendWith(MockitoExtension.class)
class ServiceUnitTest {
    @Mock
    private Repository repo;  // 不启动 Spring
}

// Spring 集成测试
@SpringBootTest
class ServiceIntegrationTest {
    @MockBean
    private ExternalApi api;  // 替换 Spring 上下文中的 Bean
}
```

---

## 三、Testcontainers 相关

### Q7：Testcontainers 启动很慢怎么办？

**优化方案**：

1. **容器复用**（开发环境）：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <environmentVariables>
            <TESTCONTAINERS_REUSE_ENABLE>true</TESTCONTAINERS_REUSE_ENABLE>
        </environmentVariables>
    </configuration>
</plugin>
```

2. **使用更轻量的镜像**：

```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");  // alpine 镜像更小
```

3. **Testcontainers Cloud**（CI 加速）：

```yaml
env:
  TESTCONTAINERS_HOST_OVERRIDE: tc.agent.host
```

---

### Q8：Testcontainer 之间能通信吗？

**答案**：可以，使用 Docker 网络：

```java
@Container
static Network network = Network.newNetwork();

@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withNetwork(network)
        .withNetworkAliases("postgres");

@Container
static GenericContainer<?> redis = new GenericContainer<>("redis:7")
        .withNetwork(network)
        .withNetworkAliases("redis");
```

---

## 四、Spring Boot Test 相关

### Q9：@SpringBootTest 启动太慢怎么办？

**解决方案**：

| 方案 | 说明 |
|------|------|
| `@WebMvcTest(XxxController.class)` | 仅加载 Controller + MockMvc |
| `@DataJpaTest` | 仅加载 JPA + 嵌入式数据库 |
| `@JsonTest` | 仅加载 Jackson |
| 拆分 `@SpringBootConfiguration` | 减少组件扫描 |

**示例**：

```java
// ❌ 启动慢（加载所有 Bean）
@SpringBootTest
class WorkflowControllerTest { ... }

// ✅ 启动快（仅 Controller + MockMvc）
@WebMvcTest(WorkflowController.class)
class WorkflowControllerTest {
    @MockBean
    private WorkflowService service;
    @Autowired
    private MockMvc mockMvc;
}
```

---

### Q10：如何禁用 Sa-Token 鉴权进行测试？

```java
@WebMvcTest(controllers = WorkflowController.class)
@AutoConfigureMockMvc(addFilters = false)  // 禁用所有 Filter
class WorkflowControllerTest { ... }
```

或在测试配置中排除：

```java
@SpringBootApplication(exclude = {SaTokenConfigure.class})
```

---

### Q11：测试中如何处理事务回滚？

```java
@SpringBootTest
@Transactional  // 默认回滚
class WorkflowServiceTest {

    @Test
    void testXxx() {
        // 数据库操作
        // 测试结束后自动回滚
    }
}
```

**注意**：`@Transactional` 会导致 `@Transactional(propagation = REQUIRES_NEW)` 不生效，可能无法测试真实事务行为。

---

## 五、CI 与报告

### Q12：CI 中 Surefire 报告乱码怎么办？

**解决方案**：设置编码：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-Dfile.encoding=UTF-8</argLine>
    </configuration>
</plugin>
```

---

### Q13：如何跳过某些测试在 CI 中执行？

```bash
# 跳过集成测试
mvn test -DexcludedGroups=integration

# 仅运行快速测试
mvn test -Dgroups=unit
```

测试类标记：

```java
@Tag("integration")
class IntegrationTest { ... }

@Tag("unit")
class UnitTest { ... }
```

---

### Q14：JaCoCo 报告显示 0% 覆盖率？

**常见原因**：
1. 测试未执行（`@Disabled` 或 `mvn test` 没跑）
2. JaCoCo agent 未附加（`prepare-agent` 没执行）
3. 报告路径错误（多模块）
4. 类被 `excludes` 排除了

**排查步骤**：

```bash
# 1. 确认测试已执行
ls backend/meowflow/<module>/target/surefire-reports/

# 2. 检查 JaCoCo exec 文件
ls backend/meowflow/<module>/target/jacoco.exec

# 3. 手动生成报告
mvn jacoco:report

# 4. 查看报告
open backend/meowflow/<module>/target/site/jacoco/index.html
```

---

## 六、最佳实践问题

### Q15：TDD 还是后写测试？

**建议**：项目初期可采用 **后写测试**（覆盖率提升），关键新功能采用 **TDD**。

| 阶段 | 推荐方式 |
|------|---------|
| 旧代码补测试 | 后写测试，目标覆盖率 |
| 新功能开发 | TDD（先测试后实现） |
| Bug 修复 | 先写失败测试，再修复 |

---

### Q16：Controller 要不要测试？

**答案**：✅ **必须测试**。

**原因**：
- Controller 是 API 入口，是契约的体现
- URL / 参数 / 响应格式变更需要测试捕获
- 鉴权、限流、异常处理都在 Controller 层

**测试方式**：`@WebMvcTest` + MockMvc

---

### Q17：私有方法需要测试吗？

**答案**：❌ **不直接测试**，通过公共方法间接覆盖。

**原因**：
- 私有方法是实现细节
- 通过公共方法间接测试更稳定
- 直接测试私有方法会导致重构困难（修改方法名 / 访问修饰符都会破坏测试）

---

### Q18：测试代码覆盖率 100% 就够了吗？

**答案**：❌ **不够**。

**原因**：
- 覆盖率只能告诉你"哪些代码执行了"，不能告诉你"是否正确"
- 100% 覆盖 ≠ 100% 正确
- 边角条件、异常路径、并发场景需要专门测试

**推荐指标**：
- 关键业务逻辑：覆盖率 + 变异测试（Pitest）
- 工具类：覆盖率
- 配置类：可忽略

---

## 七、特定框架问题

### Q19：MyBatis-Plus 的 Service 测试要注意什么？

```java
@SpringBootTest
class WorkflowServiceTest extends BaseServiceTest {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();  // 清理数据
    }

    @Test
    void testXxx() {
        // WorkflowService 调用 MyBatis-Plus 的 IService
        // 真实数据库操作，验证完整链路
    }
}
```

**注意**：MyBatis-Plus 的 `LambdaQueryWrapper` 等需要 MySQL/PostgreSQL 真实 SQL 行为，H2 可能有差异。

---

### Q20：RabbitMQ 集成测试怎么做？

**方案 A：Testcontainers**

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

**方案 B：Embedded Broker（不推荐）**

```java
// 不推荐：EmbeddedRabbitBroker 已废弃
```

**方案 C：Mock MessageListener**

```java
@MockBean
private RabbitTemplate rabbitTemplate;

@Test
void testMessageSend() {
    service.sendMessage("payload");
    verify(rabbitTemplate).convertAndSend("exchange", "key", "payload");
}
```

---

### Q21：Redis 集成测试怎么做？

**方案 A：Testcontainers**

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

**方案 B：Embedded Redis（不推荐用于真实场景）**

```xml
<!-- 已废弃，仅用于单元测试 -->
<dependency>
    <groupId>it.ozimov</groupId>
    <artifactId>embedded-redis</artifactId>
</dependency>
```

**方案 C：使用项目已有的 TestRedisConfiguration**

项目已有 `TestRedisConfiguration` 用于 mock Redis，可参考：

```java
// 参考 meowflow-workflow/src/test/java/com/meowflow/workflow/TestRedisConfiguration.java
```

---

## 八、调试与排查

### Q22：测试在本地通过，CI 失败？

**常见原因**：

| 原因 | 解决方案 |
|------|---------|
| CI 时间与本地不同 | 使用 `Clock` 注入 |
| CI 时区不同 | 使用 UTC 时间 |
| CI 资源紧张导致超时 | 增加超时时间 |
| CI 文件路径差异 | 使用相对路径 |
| CI 环境变量缺失 | 显式设置默认值 |
| 并发执行导致竞争 | 使用 `@Execution(SAME_THREAD)` |

---

### Q23：测试偶发失败（Flaky Test）怎么办？

**排查步骤**：

```
1. 重现：连续运行 100 次，记录失败率
   mvn test -Dtest=XxxTest -Dsurefire.rerunFailingTestsCount=3

2. 分析日志：查看是否资源竞争

3. 修复：
   - 用 Awaitility 替代 Thread.sleep
   - 用 fixed seed 替代随机数
   - 用 @DirtiesContext 隔离 Spring 上下文
   - 用 CountDownLatch 同步并发测试
```

**记录**：
- 创建 Issue 跟踪 flaky test
- 在测试方法上添加 `@Flaky` 注解
- 定期 review 并修复

---

### Q24：如何查看测试执行的 SQL？

```yaml
# application-test.yml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  datasource:
    url: jdbc:h2:mem:testdb;TRACE_LEVEL_SYSTEM_OUT=3
```

或者在 MyBatis 中：

```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

---

## 九、项目特定问题

### Q25：如何测试 AOP 切面（如 TraceAspect）？

```java
@SpringBootTest
class TraceAspectTest {

    @Autowired
    private TestService testService;

    @Test
    void testAspectExecution() {
        // Given
        String traceId = "test-trace-001";

        // When
        TraceContextHolder.setTraceId(traceId);
        String result = testService.doSomething();

        // Then
        assertThat(traceId).isEqualTo(TraceContextHolder.getTraceId());
    }
}
```

---

### Q27：如何测试异步执行（如 CompletableFuture）？

```java
@Test
void testAsyncExecution() throws Exception {
    // When
    CompletableFuture<String> future = asyncService.executeAsync();
    String result = future.get(5, TimeUnit.SECONDS);

    // Then
    assertThat(result).isEqualTo("expected");
}

// 或使用 Awaitility
@Test
void testAsyncWithAwaitility() {
    asyncService.executeAsync();

    await().atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> {
            assertThat(asyncService.getStatus()).isEqualTo("completed");
        });
}
```

---

## 十、参考资源

- [Spring Boot Testing Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.testing)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- 项目内部文档：[`UNIT_TEST_GUIDE.md`](UNIT_TEST_GUIDE.md)、[`INTEGRATION_TEST_GUIDE.md`](INTEGRATION_TEST_GUIDE.md)