# 喵流 (MeowFlow) - 单元测试规范

> 本文档定义单元测试的编写规范、Mockito 使用约定、AssertJ 断言风格、命名约定与最佳实践
> 适用于所有后端模块：`meowflow-*` 共 8 个子模块

---

## 一、单元测试基础

### 1.1 单元测试定义

> **单元测试**：对软件最小可测试单元（通常是单个类的方法）进行隔离验证的测试，不依赖外部系统（DB / Redis / MQ / HTTP）。

### 1.2 适用范围

**应该写单元测试的代码**：
- ✅ Service 层业务逻辑（含条件分支、异常处理）
- ✅ 工具类（Util / Helper）
- ✅ 算法实现（DAG 排序、限流算法、ID 生成器）
- ✅ 配置类（条件装配逻辑）
- ✅ 枚举与常量的语义校验

**不需要写单元测试的代码**：
- ❌ 纯 POJO / Entity（无逻辑的字段容器）
- ❌ 自动生成的代码（MapStruct / Lombok 生成的方法）
- ❌ DTO / VO / Request / Response（数据传输对象）
- ❌ 简单的 Getter / Setter
- ❌ Spring 配置类（仅声明 Bean，无业务逻辑）

### 1.3 单元测试原则（FIRST 原则）

| 原则 | 含义 | 实践 |
|------|------|------|
| **F**ast | 测试执行要快（毫秒级） | 单元测试不应启动 Spring 容器、不连真实 DB |
| **I**ndependent | 测试间相互独立 | 不依赖执行顺序、不共享状态 |
| **R**epeatable | 可重复执行 | 不依赖时间、网络、文件系统 |
| **S**elf-Validating | 自验证（PASS / FAIL） | 不需要人工检查输出 |
| **T**imely | 及时编写 | 与生产代码同步编写（TDD 推荐） |

---

## 二、JUnit 5 使用规范

### 2.1 注解使用

| 注解 | 用途 | 示例 |
|------|------|------|
| `@Test` | 标记测试方法 | `@Test void testXxx()` |
| `@DisplayName` | 测试方法的中文/友好名称 | `@DisplayName("提交评分 - 新评分")` |
| `@Nested` | 嵌套测试类（分组相关测试） | `@Nested class RouteTests` |
| `@BeforeEach` | 每个测试方法前执行 | 初始化 Mock 对象 |
| `@AfterEach` | 每个测试方法后执行 | 清理资源 |
| `@BeforeAll` | 所有测试方法前执行一次（必须 static） | 启动重型资源 |
| `@AfterAll` | 所有测试方法后执行一次 | 关闭资源 |
| `@Disabled` | 禁用测试 | `@Disabled("待修复 #123")` |
| `@Tag` | 测试分组（用于过滤） | `@Tag("integration")` |
| `@ParameterizedTest` | 参数化测试 | `@ValueSource(strings = {"a", "b"})` |

### 2.2 断言 API 优先级

**优先使用 AssertJ**（已在项目中使用，见 `ModelRouterTest`），其次是 JUnit 5 内置断言，最后才是 Mockito 验证：

```java
// ✅ 推荐：AssertJ 流式断言（可读性最强）
assertThat(response)
    .isNotNull()
    .extracting(ChatResponse::getContent, ChatResponse::getModel)
    .containsExactly("Hello", "gpt-4o");

// ⚠️ 可用：JUnit 5 内置断言
assertNotNull(response);
assertEquals("Hello", response.getContent());

// ❌ 避免：旧式 assertTrue/assertFalse 拼凑
assertTrue(response != null && "Hello".equals(response.getContent()));
```

**常见断言对照表**：

| 场景 | AssertJ | JUnit 5 |
|------|---------|---------|
| 相等 | `.isEqualTo(expected)` | `assertEquals(expected, actual)` |
| 不为空 | `.isNotNull()` | `assertNotNull(actual)` |
| 抛异常 | `assertThatThrownBy(() -> ...).isInstanceOf(X.class)` | `assertThrows(X.class, () -> ...)` |
| 集合 | `.hasSize(3).contains("a", "b")` | `assertEquals(3, list.size())` |
| 布尔 | `.isTrue()` / `.isFalse()` | `assertTrue(cond)` |
| 字符串 | `.startsWith("prefix")` | - |
| 对象字段 | `.extracting(obj::getField).isEqualTo("v")` | `assertEquals("v", obj.getField())` |

### 2.3 测试方法命名规范

**格式**：`<methodName>_<scenario>_<expectedResult>`

**示例**（取自 `ModelRouterTest`）：

```java
@Test
void testRouteAndComplete_OpenAIModel() { ... }      // 路由到 OpenAI 模型
@Test
void testRouteAndComplete_UnsupportedModel() { ... } // 不支持的模型抛异常
@Test
void testGetSupportedModels_NoClients() { ... }       // 无客户端时返回空
@Test
void testSubmitRating_NewRating() { ... }             // 提交新评分
@Test
void testSubmitRating_TemplateNotFound() { ... }      // 模板不存在
```

**命名要点**：
1. 必须描述**输入场景**而非业务描述
2. 期望结果可以是隐式的（方法名已暗示），但**异常场景必须显式说明**
3. 避免使用 `test1`、`test2` 这类无意义命名
4. 中文 DisplayName + 英文方法名（适合中国团队）

**配合 @DisplayName**：

```java
@Test
@DisplayName("路由到 OpenAI 客户端 (gpt-4o)")
void testRouteAndComplete_OpenAIModel() {
    // ...
}
```

### 2.4 Given-When-Then 模式（AAA 模式）

**AAA = Arrange / Act / Assert**，每个测试方法分为三段：

```java
@Test
@DisplayName("提交评分 - 新评分")
void testSubmitRating_NewRating() {
    // Given (Arrange)：准备测试数据和 Mock 行为
    Template template = createTemplate();
    when(templateRepository.findById(TEMPLATE_ID)).thenReturn(template);
    when(ratingRepository.findByTemplateIdAndUserId(TEMPLATE_ID, USER_ID)).thenReturn(null);
    when(ratingRepository.selectAverageScore(TEMPLATE_ID)).thenReturn(4.5);

    RatingRequest request = new RatingRequest();
    request.setScore(5);

    // When (Act)：执行被测方法
    TemplateRating result = ratingService.submitRating(TEMPLATE_ID, request);

    // Then (Assert)：验证结果
    assertNotNull(result);
    assertEquals(5, result.getScore());
    verify(ratingRepository).insert(any(TemplateRating.class));
}
```

**规范要点**：
1. 三段之间用空行分隔，提高可读性
2. 注释使用 `// Given` `// When` `// Then` 显式标注
3. 一个测试方法**只验证一个场景**（一个业务规则）

---

## 三、Mockito 使用规范

### 3.1 项目现状

项目使用 **Mockito 5.8.0 + JUnit 5 Extension**：

```java
@ExtendWith(MockitoExtension.class)
class TemplateRatingServiceTest {

    @Mock
    private TemplateRatingRepository ratingRepository;

    @InjectMocks
    private TemplateRatingService ratingService;

    @Test
    void testXxx() {
        // ...
    }
}
```

**两种初始化方式**：

| 方式 | 优点 | 缺点 |
|------|------|------|
| `@Mock` + `@InjectMocks` | 简洁，由 Mockito 注入 | 字段必须可注入（构造器/setter） |
| 手动 `Mockito.mock()` | 灵活，支持构造器参数 | 代码冗长 |

### 3.2 基础 Mock 操作

#### 3.2.1 打桩（Stub）

```java
// 返回固定值
when(repository.findById(1L)).thenReturn(entity);

// 返回 null（默认）
when(repository.findById(1L)).thenReturn(null);

// 链式调用返回（多参数或多状态）
when(repository.findByNameAndStatus("a", "active"))
    .thenReturn(entity1)
    .thenReturn(entity2)              // 第二次返回 entity2
    .thenThrow(new RuntimeException()); // 第三次抛异常

// 抛出异常
when(repository.findById(999L)).thenThrow(new ResourceNotFoundException());

// 任意参数
when(repository.findById(anyLong())).thenReturn(entity);

// void 方法的异常
doThrow(new IOException()).when(fileService).delete(anyString());
```

#### 3.2.2 验证（Verify）

```java
// 验证调用次数（默认 1 次）
verify(repository).save(any(Entity.class));

// 验证调用次数（精确）
verify(repository, times(3)).save(any(Entity.class));

// 验证从未调用
verify(repository, never()).delete(anyLong());

// 验证至少/最多
verify(repository, atLeast(1)).save(any());
verify(repository, atMost(5)).save(any());

// 验证无其他交互
verifyNoMoreInteractions(repository);

// 验证调用顺序（多个 Mock）
InOrder inOrder = inOrder(repository, validator);
inOrder.verify(repository).findById(1L);
inOrder.verify(validator).validate(any());
```

#### 3.2.3 参数匹配

```java
// 精确匹配
verify(repository).save(argThat(e -> e.getName().equals("test")));

// 任意参数
verify(repository).save(any(Entity.class));

// 自定义匹配器
verify(repository).findByStatus(argThat((String s) -> s.startsWith("act")));
```

### 3.3 Mock vs Spy

| 维度 | `@Mock` | `@Spy` |
|------|---------|--------|
| 默认行为 | 所有方法返回默认值（null/0/false） | 调用真实方法 |
| 适用场景 | 完全隔离依赖 | 部分 mock（部分真实逻辑） |
| 用法 | `@Mock private Dep dep;` | `@Spy private Dep dep = new Dep();` |

**优先使用 `@Mock`**，因为 Spy 的行为难以预测且可能产生副作用。

### 3.4 静态方法 Mock（谨慎使用）

```java
// 使用 mockito-inline
try (MockedStatic<IdGenerator> mocked = Mockito.mockStatic(IdGenerator.class)) {
    mocked.when(IdGenerator::nextId).thenReturn(123L);

    // 执行测试代码
    service.doSomething();

    // 验证
    mocked.verify(() -> IdGenerator.nextId(), times(1));
}
```

**警告**：静态方法 Mock 会导致测试与实现强耦合，应优先考虑：
1. 重构代码，通过依赖注入替代静态调用
2. 使用 `Mockito.mockStatic` 时，必须在 `try-with-resources` 内使用

### 3.5 高级特性：BDD 风格（推荐）

```java
// BDD 风格：given / when / then（语义更清晰）
import static org.mockito.BDDMockito.*;

@Test
void testXxx() {
    // given
    given(repository.findById(1L)).willReturn(entity);

    // when
    Entity result = service.findById(1L);

    // then
    then(repository).should().findById(1L);
    assertThat(result).isEqualTo(entity);
}
```

**优点**：与 Given-When-Then 测试模式语义一致，团队新成员更容易理解。

### 3.6 常见反模式

| ❌ 反模式 | ✅ 正确做法 |
|----------|-----------|
| Mock 所有依赖（包括简单 POJO） | 仅 Mock 跨边界依赖（DB / 外部服务） |
| `verify(repository, times(1)).save(any())`（默认值就是 1） | 省略 times(1)，或在多次调用时显式标注 |
| 在测试中执行 `Thread.sleep()` | 使用 Awaitility 异步等待 |
| `doReturn(x).when(mock).method(any())` 用于非 void 方法 | 优先用 `when(mock.method(any())).thenReturn(x)` |
| 测试间共享 `@Mock` 状态 | 每个测试 `@BeforeEach` 重置 Mock |

---

## 四、AssertJ 断言风格

### 4.1 基础断言

```java
// 字符串
assertThat(name).isEqualTo("Alice").startsWith("Al").hasSize(5);

// 数字
assertThat(count).isEqualTo(10).isPositive().isLessThan(100);

// 集合
assertThat(list).hasSize(3).contains("a", "b").doesNotContain("z");

// Map
assertThat(map).hasSize(2).containsKey("k1").containsValue("v1");

// Optional
assertThat(optional).isPresent().get().isEqualTo("value");

// 异常
assertThatThrownBy(() -> service.fail())
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("invalid");
```

### 4.2 对象字段提取

```java
// 提取单个字段
assertThat(response).extracting(Response::getStatus).isEqualTo("OK");

// 提取多个字段（元组断言）
assertThat(response).extracting(Response::getCode, Response::getMessage)
    .containsExactly(200, "Success");

// 类型安全的字段断言
assertThat(users).extracting(User::getAge).containsOnly(20, 25, 30);
```

### 4.3 集合深度断言

```java
// 所有元素满足条件
assertThat(users).allMatch(u -> u.getAge() >= 18);

// 至少一个满足
assertThat(users).anyMatch(u -> u.getName().equals("admin"));

// 嵌套对象断言
assertThat(users)
    .hasSize(3)
    .filteredOn(u -> u.getActive())
    .extracting(User::getName)
    .containsExactly("Alice", "Bob");
```

### 4.4 时间相关断言（需 AssertJ 3.x）

```java
import java.time.LocalDateTime;

LocalDateTime now = LocalDateTime.now();
assertThat(timestamp).isCloseTo(now, within(1, ChronoUnit.SECONDS));
```

### 4.5 文件断言（可选）

```java
import org.assertj.core.api.FileAssert;

assertThat(new File("pom.xml")).exists().isFile().canRead();
```

---

## 五、测试数据构造

### 5.1 TestDataBuilder 模式

项目已有 `TestDataBuilder` 模式（见 `meowflow-workflow/.../TestDataBuilder.java`）：

```java
public class TestDataBuilder {

    private TestDataBuilder() {
        // 工具类
    }

    public static Workflow createWorkflow() {
        Workflow workflow = new Workflow();
        workflow.setId(1L);
        workflow.setName("Test Workflow");
        workflow.setStatus("draft");
        // ...
        return workflow;
    }

    public static Workflow createWorkflow(Long id, String name) {
        Workflow workflow = createWorkflow();
        workflow.setId(id);
        workflow.setName(name);
        return workflow;
    }

    public static WorkflowDefinition createSimpleWorkflow() {
        // 构造一个完整的简单工作流定义
        return WorkflowDefinition.builder()
                .workflowId("test-workflow")
                .nodes(List.of(...))
                .edges(List.of(...))
                .build();
    }
}
```

**优点**：
1. 集中管理测试数据默认值
2. 重载支持自定义关键字段
3. 静态方法 + 私有构造器 → 工具类

### 5.2 Builder 模式（更复杂对象）

```java
// 复杂对象使用链式 Builder
User user = User.builder()
    .id(1L)
    .name("Alice")
    .email("alice@example.com")
    .role(UserRole.ADMIN)
    .createdAt(LocalDateTime.now())
    .build();
```

### 5.3 测试夹具（Fixture）文件

对于复杂的 JSON / YAML 数据，使用 `src/test/resources/fixtures/` 目录：

```
src/test/resources/
├── fixtures/
│   ├── workflow-simple.json
│   ├── workflow-with-parallel.json
│   └── chat-request.json
└── application-test.yml
```

加载方式：

```java
String json = new String(getClass().getResourceAsStream("/fixtures/workflow-simple.json").readAllBytes());
WorkflowDefinition def = objectMapper.readValue(json, WorkflowDefinition.class);
```

---

## 六、参数化测试

### 6.1 基本用法

```java
@ParameterizedTest
@ValueSource(strings = {"gpt-4o", "gpt-4o-mini", "gpt-4-turbo"})
void testIsModelSupported_OpenAI(String modelName) {
    when(openAiClient.getSupportedModels()).thenReturn(Arrays.asList("gpt-4o", "gpt-4o-mini", "gpt-4-turbo"));

    assertThat(modelRouter.isModelSupported(modelName)).isTrue();
}

@ParameterizedTest
@EnumSource(NodeType.class)
void testAllNodeTypes(NodeType type) {
    // 测试所有枚举值
}

@ParameterizedTest
@CsvSource({
    "gpt-4o, 0.7, 2048",
    "claude-3-5-sonnet, 0.5, 4096"
})
void testChatOptions(String model, double temp, int maxTokens) {
    // ...
}

@ParameterizedTest
@MethodSource("provideInvalidInputs")
void testInvalidInput(String input, String expectedError) {
    // ...
}

static Stream<Arguments> provideInvalidInputs() {
    return Stream.of(
        Arguments.of("", "must not be blank"),
        Arguments.of(null, "must not be null")
    );
}
```

### 6.2 适用场景

- 多组类似输入验证同一逻辑
- 边界值测试（min/max/empty/null）
- 异常场景的所有触发条件

---

## 七、测试组织

### 7.1 嵌套测试类（@Nested）

适合测试类较大时按功能分组（参考 `ModelRouterTest`）：

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ModelRouter Tests")
class ModelRouterTest {

    @Nested
    @DisplayName("routeAndComplete Tests")
    class RouteAndCompleteTests {
        @Test
        void testRouteAndComplete_OpenAIModel() { ... }

        @Test
        void testRouteAndComplete_UnsupportedModel() { ... }
    }

    @Nested
    @DisplayName("CircuitBreaker Tests")
    class CircuitBreakerTests {
        @Test
        void testRouteWithCircuitBreaker() { ... }
    }
}
```

### 7.2 测试目录结构

```
src/test/java/com/meowflow/<module>/
├── <Module>ApplicationTests.java          # Spring 上下文加载测试
├── service/
│   ├── WorkflowServiceTest.java          # Service 单元测试
│   └── TemplateRatingServiceTest.java
├── engine/                                # 核心算法测试
│   └── DAGSorterTest.java
├── util/                                  # 工具类测试
│   └── IdGeneratorTest.java
├── BaseUnitTest.java                     # 通用基类（可选）
├── BaseServiceTest.java                  # 集成测试基类
└── TestDataBuilder.java                  # 测试数据构造器
```

### 7.3 测试文件命名

- 测试类：`XxxTest.java` 或 `XxxTests.java`（两种后缀 Surefire 都识别）
- 测试方法：以 `test` 开头（可选），但推荐使用 `methodName_scenario_expectedResult` 模式

---

## 八、运行与调试

### 8.1 本地运行

```bash
# 运行所有测试
mvn test

# 运行单个模块
mvn -pl meowflow-workflow test

# 运行单个测试类
mvn -pl meowflow-workflow test -Dtest=WorkflowServiceTest

# 运行单个测试方法
mvn -pl meowflow-workflow test -Dtest=WorkflowServiceTest#testCreateWorkflow

# 使用标签过滤
mvn test -Dgroups=unit
mvn test -Dgroups=integration

# 调试模式（挂起等待 debugger）
mvn test -Dmaven.surefire.debug
```

### 8.2 IDE 集成

**IntelliJ IDEA**：
- 在测试方法左侧的绿色箭头点击运行 / 调试
- 右键测试类 → Run / Debug

---

## 九、总结：单元测试 Checklist

编写每个单元测试时，确保：

- [ ] 测试方法名遵循 `methodName_scenario_expectedResult` 模式
- [ ] 使用 `@DisplayName` 提供中文友好名称
- [ ] 包含 Given / When / Then 三段注释
- [ ] 一个测试方法只验证一个业务场景
- [ ] 使用 AssertJ 流式断言（非 JUnit 内置）
- [ ] 仅 Mock 跨边界依赖（DB / 外部服务），不 Mock 简单值对象
- [ ] 使用 `verify` 验证关键交互（不验证所有）
- [ ] 无 `Thread.sleep()`、无随机数（除非显式 mock）
- [ ] 测试不依赖执行顺序
- [ ] 测试执行时间 < 100ms

---

## 十、参考资源

- [JUnit 5 用户指南](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito 官方文档](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ 断言指南](https://assertj.github.io/doc/)
- [MockMvc 测试](https://docs.spring.io/spring-framework/reference/testing/spring-mvc-test-framework.html)
