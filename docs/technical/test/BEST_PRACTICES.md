# 喵流 (MeowFlow) - 测试编写最佳实践

> 本文档汇总测试编写的最佳实践、常见反模式与改进建议

---

## 一、AAA 模式（Arrange-Act-Assert）

### 1.1 标准结构

每个测试方法应清晰分为三段：

```java
@Test
@DisplayName("提交评分 - 新评分场景")
void testSubmitRating_NewRating() {
    // Arrange (Given) - 准备测试数据
    Template template = createTemplate();
    when(templateRepository.findById(TEMPLATE_ID)).thenReturn(template);

    // Act (When) - 执行被测方法
    TemplateRating result = ratingService.submitRating(TEMPLATE_ID, request);

    // Assert (Then) - 验证结果
    assertThat(result).isNotNull();
    verify(ratingRepository).insert(any(TemplateRating.class));
}
```

### 1.2 改进：使用空行 + 注释显式标注

```java
@Test
void testXxx() {
    // Given
    ...

    // When
    ...

    // Then
    ...
}
```

---

## 二、单一职责原则

### 2.1 一个测试只验证一个场景

**❌ 反模式**：

```java
@Test
void testEverything() {
    // 创建 + 查询 + 更新 + 删除
    workflowService.create(...);
    workflowService.getById(...);
    workflowService.update(...);
    workflowService.delete(...);

    // 一个失败全部失败
}
```

**✅ 正确做法**：

```java
@Test
void testCreateWorkflow() { ... }

@Test
void testGetById() { ... }

@Test
void testUpdate() { ... }

@Test
void testDelete() { ... }
```

### 2.2 独立断言（每条断言一个失败原因）

**❌ 反模式**：

```java
assertEquals(5, result.size());
assertTrue(result.contains("a"));
assertEquals(200, response.getStatus());
// 任一失败，无法定位具体问题
```

**✅ 正确做法**：使用 AssertJ 软断言（Soft Assertions）：

```java
SoftAssertions.assertSoftly(softly -> {
    softly.assertThat(result).hasSize(5);
    softly.assertThat(result).contains("a");
    softly.assertThat(response.getStatus()).isEqualTo(200);
});
// 全部断言执行，错误一次性报告
```

---

## 三、测试隔离

### 3.1 测试间不应共享状态

**❌ 反模式**：

```java
class WorkflowServiceTest {
    private Workflow workflow;  // 在多个测试间共享

    @Test
    void testA() {
        workflow = workflowService.create(...);  // 状态污染
    }

    @Test
    void testB() {
        // 依赖 testA 的副作用
        workflowService.update(workflow.getId(), ...);
    }
}
```

**✅ 正确做法**：

```java
class WorkflowServiceTest {
    @BeforeEach
    void setUp() {
        // 每个测试前重新初始化
    }

    @Test
    void testA() {
        Workflow workflow = workflowService.create(...);  // 局部变量
    }

    @Test
    void testB() {
        Workflow workflow = TestDataBuilder.createWorkflow();  // 独立构造
    }
}
```

### 3.2 避免 @BeforeAll 的副作用

`@BeforeAll` 中的状态会被所有测试共享，应仅用于启动重型资源：

```java
@BeforeAll
static void startContainer() {  // 仅启动 Docker 容器
    postgres.start();
}

@BeforeEach
void setUp() {  // 每个测试都重新初始化
    workflowRepository.deleteAll();
}
```

---

## 四、可读性

### 4.1 测试方法名表达意图

**❌ 反模式**：

```java
@Test
void test1() { ... }

@Test
void testWorkflow() { ... }

@Test
void test() { ... }
```

**✅ 正确做法**：

```java
@Test
@DisplayName("工作流名称为空时返回 400")
void testCreateWorkflow_EmptyName_ReturnsBadRequest() { ... }
```

### 4.2 测试代码就是文档

测试代码应当**清晰展示业务规则**，让读测试的人就能理解系统行为。

```java
@Test
@DisplayName("用户取消评分 - 验证通知发送")
void testCancelRating_SendsNotification() {
    // 这个测试不仅验证了取消评分的逻辑
    // 还展示了"取消评分会发送通知"这一业务规则
}
```

### 4.3 提取公共逻辑

```java
// ❌ 重复代码
@Test
void testA() {
    Template template = new Template();
    template.setId("t1");
    template.setName("Test Template");
    template.setStatus("active");
    // ...
}

@Test
void testB() {
    Template template = new Template();
    template.setId("t2");
    template.setName("Test Template");
    template.setStatus("active");
    // ...
}

// ✅ 抽取工厂方法
private Template createTemplate(String id) {
    Template template = new Template();
    template.setId(id);
    template.setName("Test Template");
    template.setStatus("active");
    return template;
}
```

### 4.4 使用常量替代魔法值

```java
// ❌ 魔法值
when(repository.findById("template-123")).thenReturn(template);

// ✅ 命名常量
private static final String TEMPLATE_ID = "template-123";
when(repository.findById(TEMPLATE_ID)).thenReturn(template);
```

---

## 五、Mock 策略

### 5.1 Mock vs Stub vs Spy

| 工具 | 含义 | 适用场景 |
|------|------|---------|
| **Mock** | 行为可验证的替身 | 验证方法是否被调用 |
| **Stub** | 返回固定值的替身 | 提供测试数据 |
| **Spy** | 真实方法 + 部分 Mock | 部分行为需保留 |

**项目实践**：
- 跨边界依赖（DB / MQ / HTTP）→ Mock
- 简单返回值 → Stub（用 `thenReturn`）
- 避免使用 Spy（难以维护）

### 5.2 不要过度 Mock

**❌ 反模式**：

```java
@Test
void testServiceX() {
    // Mock 了 5 个依赖，但只测了 1 个简单方法
    when(dep1.foo()).thenReturn(x);
    when(dep2.bar()).thenReturn(y);
    when(dep3.baz()).thenReturn(z);
    when(dep4.qux()).thenReturn(w);
    when(dep5.quux()).thenReturn(v);

    String result = service.simpleMethod();  // 不依赖任何 Mock
    assertEquals("expected", result);
}
```

**✅ 正确做法**：

```java
@Test
void testServiceX() {
    // simpleMethod 不依赖任何外部，直接测试
    String result = service.simpleMethod();
    assertEquals("expected", result);
}
```

### 5.3 不要 Mock 简单值对象

```java
// ❌ 反模式
User mockUser = mock(User.class);
when(mockUser.getName()).thenReturn("Alice");

// ✅ 正确做法
User realUser = User.builder().name("Alice").build();
```

### 5.4 Mock 外部 HTTP 服务（推荐 WireMock）

对于外部 API，使用 WireMock 替代手写 Mock Controller：

```java
@RegisterExtension
static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

@Test
void testHttpCall() {
    wireMock.stubFor(post("/external-api")
        .willReturn(okJson("{\"status\":\"ok\"}")));

    // 测试代码触发 HTTP 调用

    wireMock.verify(postRequestedFor(urlEqualTo("/external-api")));
}
```

---

## 六、异常测试

### 6.1 验证抛异常

```java
// ✅ 推荐：AssertJ
assertThatThrownBy(() -> service.fail())
    .isInstanceOf(BizException.class)
    .hasMessageContaining("模板不存在")
    .hasFieldOrPropertyWithValue("code", "TEMPLATE_NOT_FOUND");

// ✅ 可用：JUnit 5
BizException exception = assertThrows(BizException.class,
    () -> service.fail());
assertEquals("模板不存在", exception.getMessage());

// ❌ 反模式
try {
    service.fail();
    fail("应该抛出异常");
} catch (Exception e) {
    // 验证逻辑
}
```

### 6.2 验证异常类型

```java
// 精确类型
assertThatThrownBy(() -> service.fail())
    .isInstanceOf(BizException.class);

// 父类型（更宽松）
assertThatThrownBy(() -> service.fail())
    .isInstanceOf(RuntimeException.class);

// 类型 + 因果链
assertThatThrownBy(() -> service.fail())
    .isInstanceOf(BizException.class)
    .hasCauseInstanceOf(SQLException.class);
```

---

## 七、参数化测试

### 7.1 适用场景

- ✅ 边界值测试
- ✅ 多组类似输入
- ✅ 枚举值遍历

### 7.2 不适用场景

- ❌ 不同测试需要不同的 Mock 设置（应拆分为独立测试）
- ❌ 测试逻辑复杂（参数化会降低可读性）

### 7.3 示例

```java
@ParameterizedTest
@ValueSource(strings = {"", " ", "  "})
@DisplayName("工作流名称为空白时返回 400")
void testCreateWorkflow_BlankName_Rejects(String blankName) {
    WorkflowCreateRequest request = new WorkflowCreateRequest();
    request.setName(blankName);

    assertThatThrownBy(() -> service.create(request, 1L))
        .isInstanceOf(BizException.class)
        .hasMessageContaining("名称不能为空");
}

@ParameterizedTest
@EnumSource(value = NodeType.class, names = {"HTTP", "EMAIL", "DATABASE"})
@DisplayName("节点重试场景 - 不同节点类型")
void testNodeRetry(NodeType nodeType) {
    // 测试所有需要重试的节点类型
}
```

---

## 八、测试代码也是代码

### 8.1 测试代码应保持高质量

- 不复制粘贴（抽取公共方法）
- 命名清晰
- 遵循项目代码规范（Checkstyle / SonarQube）
- 定期重构

### 8.2 测试代码也需要 Review

PR Review 时：
- [ ] 测试覆盖了关键场景
- [ ] 测试独立、可重复
- [ ] 命名清晰、意图明确
- [ ] 无 `Thread.sleep()`、无随机数
- [ ] 适当的断言（不过度、不缺失）
- [ ] 不测试实现细节（只测试行为）

### 8.3 保持测试同步更新

修改生产代码时，同步更新测试：
- 修改方法签名 → 更新测试调用
- 修改业务规则 → 更新测试断言
- 重构内部实现 → 测试**不应**变（只测行为）

---

## 九、测试运行与调试

### 9.1 测试失败的调试步骤

```
1. 查看错误信息（哪个断言失败）
2. 阅读测试代码（预期是什么）
3. 阅读生产代码（实际是什么）
4. 添加断点或日志
5. 修复生产代码或更新测试
```

### 9.2 测试运行慢的优化

| 优化项 | 方法 |
|--------|------|
| 启动 Spring 慢 | 使用 `@WebMvcTest` 替代 `@SpringBootTest` |
| Docker 启动慢 | 容器复用、Testcontainers Cloud |
| 数据库初始化慢 | 使用 H2（仅快速场景） |
| 多个测试重复启动 | 静态容器、共享 Application Context |

### 9.3 测试 flaky（不稳定）的常见原因

| 原因 | 解决方案 |
|------|---------|
| 依赖时间 | 使用 `Clock` 接口注入 |
| 并发竞争 | 使用 `CountDownLatch` 同步 |
| 资源未清理 | `@AfterEach` 清理 |
| 随机数据 | 使用固定 seed |
| 网络依赖 | Mock 或 WireMock |

---

## 十、推荐资源

### 10.1 推荐阅读

- 《单元测试之道》(Java 版) - Andrew Hunt
- 《有效的单元测试》- Lasse Koskela
- [Google Testing Blog](https://testing.googleblog.com/)

### 10.2 推荐工具

| 工具 | 用途 |
|------|------|
| **AssertJ** | 流式断言 |
| **Mockito** | Mock 框架 |
| **WireMock** | HTTP Mock |
| **Testcontainers** | 真实外部依赖 |
| **Awaitility** | 异步等待 |
| **JaCoCo** | 覆盖率 |
| **Pitest** | 变异测试 |
| **ArchUnit** | 架构测试 |

---

## 十一、常见反模式汇总

| ❌ 反模式 | ✅ 正确做法 |
|----------|-----------|
| 测试依赖执行顺序 | `@BeforeEach` 重新初始化 |
| 测试间共享 `@Mock` 状态 | 使用 `@Mock` + `@BeforeEach` 重置 |
| 测试方法做多件事 | 一个测试一个场景 |
| 魔法值无命名 | 使用命名常量 |
| Mock 所有依赖 | 仅 Mock 跨边界依赖 |
| 使用 `Thread.sleep()` | 使用 Awaitility |
| 过度追求 100% 覆盖率 | 关键路径 ≥ 90%，其他按价值 |
| 测试私有方法 | 通过公共方法间接测试 |
| 测试日志输出 | 测试业务行为，不测日志 |
| `assertTrue(x == y)` | 使用 AssertJ `.isEqualTo()` |