# 喵流 (MeowFlow) - 测试覆盖率指南

> 本文档定义测试覆盖率目标、JaCoCo 配置、报告生成与 CI 集成

---

## 一、覆盖率目标

### 1.1 分层目标

| 层级 | 当前门槛 | 目标门槛 | 说明 |
|------|---------|---------|------|
| **行覆盖率（LINE）** | 60% | **≥ 80%** | 项目整体 |
| **分支覆盖率（BRANCH）** | 未配置 | **≥ 70%** | 条件分支 |
| **核心 Service 层** | 未配置 | **≥ 90%** | 业务关键路径 |
| **基础设施层（common）** | 未配置 | **≥ 85%** | IdGenerator / TraceAspect 等 |
| **算法层（DAGSorter 等）** | 未配置 | **≥ 95%** | 边界条件必须覆盖 |
| **Controller 层** | 0% | **≥ 75%** | 每个 Controller 至少 5-10 个测试 |
| **生成代码（Lombok/MapStruct）** | 排除 | 排除 | 不计入覆盖率 |

### 1.2 模块优先级

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       覆盖率目标优先级                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  P0（必须 ≥ 90%）                                                       │
│  • meowflow-workflow: WorkflowService / WorkflowEngine / DAGSorter      │
│  • meowflow-executor: TaskConsumer / IdempotentService                  │
│  • meowflow-common: IdGenerator / RateLimiter                           │
│                                                                          │
│  P1（必须 ≥ 80%）                                                       │
│  • meowflow-user: UserService / PermissionService                       │
│  • meowflow-monitor: AlertService                                       │
│  • meowflow-infra: ModelRouter / FallbackChain                          │
│                                                                          │
│  P2（建议 ≥ 70%）                                                       │
│  • 所有 Controller 层（通过 MockMvc 测试）                              │
│  • meowflow-template: TemplateRatingService                             │
│                                                                          │
│  P3（不强制）                                                           │
│  • 配置类、Bean 装配                                                   │
│  • 简单 DTO / VO                                                       │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.3 覆盖率与测试成本平衡

> **覆盖率不是越高越好**。盲目追求 100% 覆盖率会导致大量无意义的测试代码。建议：
> - 关键业务逻辑：90%+
> - 工具类：80%+
> - Controller：70%+
> - 配置文件：可忽略

---

## 二、JaCoCo 配置

### 2.1 当前配置（父 POM）

`backend/meowflow/pom.xml` 已配置：

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.60</minimum>  <!-- 当前门槛偏低 -->
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 2.2 推荐配置（升级目标）

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <!-- 项目总体行覆盖率 ≥ 80% -->
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                        </limits>
                    </rule>
                    <!-- 关键模块单独规则 -->
                    <rule>
                        <element>BUNDLE</element>
                        <includes>
                            <include>com.meowflow.workflow.service.*</include>
                            <include>com.meowflow.workflow.engine.*</include>
                            <include>com.meowflow.executor.mq.*</include>
                            <include>com.meowflow.common.util.*</include>
                        </includes>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.90</minimum>
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.85</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 2.3 排除规则

在某些情况下应排除特定类：

```xml
<configuration>
    <excludes>
        <!-- DTO / VO / 实体 -->
        <exclude>com/meowflow/**/dto/**</exclude>
        <exclude>com/meowflow/**/vo/**</exclude>
        <exclude>com/meowflow/**/entity/**</exclude>
        <exclude>com/meowflow/**/request/**</exclude>
        <exclude>com/meowflow/**/response/**</exclude>

        <!-- Lombok 生成的代码 -->
        <exclude>**/*$*</exclude>

        <!-- 配置类 -->
        <exclude>com/meowflow/**/config/**/*Config.class</exclude>

        <!-- Application 主类 -->
        <exclude>com/meowflow/*Application.class</exclude>
    </excludes>
</configuration>
```

---

## 三、生成覆盖率报告

### 3.1 本地生成报告

```bash
# 1. 运行测试并生成报告
mvn clean test

# 2. 报告生成位置
backend/meowflow/target/site/jacoco/index.html
backend/meowflow/<module>/target/site/jacoco/index.html

# 3. 仅生成报告（不跑测试）
mvn jacoco:report

# 4. 验证覆盖率门槛
mvn verify
```

### 3.2 多模块聚合报告

由于项目是多模块结构，每个模块会生成独立报告。要生成**聚合报告**：

```xml
<!-- 在父 pom.xml 中添加聚合插件 -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>aggregate-report</id>
            <phase>verify</phase>
            <goals>
                <goal>report-aggregate</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

聚合报告位置：`backend/meowflow/target/site/jacoco-aggregate/index.html`

### 3.3 报告内容解读

打开 `index.html` 后：

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Coverage Report — MeowFlow                                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Element                    Missed Instructions   Coverage              │
│  ─────────────────────────────────────────────────────────────           │
│  meowflow                    12,345 / 98,765       87%   ← 项目总体      │
│  meowflow-workflow           1,234 / 12,345        90%   ← 模块级        │
│  WorkflowService             23 / 1,234           98%   ← 类级          │
│  create()                    5 / 567              99%                    │
│  getById()                   2 / 234              99%                    │
│  update()                    16 / 433             96%                    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

颜色规则：
- 🟢 绿色：80% 以上（达标）
- 🟡 黄色：50%-80%（关注）
- 🔴 红色：50% 以下（必须改进）

---

## 四、CI 集成

### 4.1 当前 GitHub Actions 配置

`.github/workflows/test.yml` 中：

```yaml
- name: Generate JaCoCo report
  run: mvn jacoco:report -q

- name: Upload coverage to Codecov
  uses: codecov/codecov-action@v3
  with:
    files: ${{ github.workspace }}/backend/meowflow/target/site/jacoco/jacoco.xml
    flags: unittests
    name: codecov-umbrella
  continue-on-error: true

- name: Check coverage gate
  run: |
    coverage=$(cat target/site/jacoco/jacoco.xml | grep -oP 'Line coverage.*?\K[0-9.]+' || echo "0")
    echo "Current coverage: $coverage"
    if (( $(echo "$coverage < 0.60" | bc -l) )); then
      echo "Coverage $coverage is below 60% threshold"
      exit 1
    fi
```

### 4.2 推荐改进

#### 4.2.1 移除 `continue-on-error`

```yaml
- name: Upload coverage to Codecov
  uses: codecov/codecov-action@v3
  with:
    files: ${{ github.workspace }}/backend/meowflow/target/site/jacoco/jacoco.xml
    flags: unittests
    name: codecov-umbrella
  # 移除 continue-on-error: true
```

#### 4.2.2 使用 JaCoCo 自带 check（更准确）

```yaml
- name: Verify with coverage gate
  run: mvn verify
```

#### 4.2.3 上传 HTML 报告作为 Artifact

```yaml
- name: Upload JaCoCo report
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: jacoco-report
    path: |
      backend/meowflow/**/target/site/jacoco/*.html
      backend/meowflow/target/site/jacoco-aggregate/*.html
    retention-days: 30
```

### 4.3 Codecov 配置（可选）

在项目根目录创建 `codecov.yml`：

```yaml
coverage:
  status:
    project:
      default:
        target: 80%
        threshold: 1%   # 允许 1% 波动
        if_ci_failed: error
    patch:
      default:
        target: 80%
comment:
  layout: "header, diff, components, files"
```

---

## 五、覆盖率分析与改进

### 5.1 查找未覆盖代码

JaCoCo 报告提供了：

1. **红色高亮**：未覆盖的代码行（点击查看）
2. **黄色钻石**：未覆盖的分支
3. **覆盖率排行**：点击查看最低覆盖率类

### 5.2 优先改进策略

```
┌─────────────────────────────────────────────────────────────────────────┐
│                   覆盖率改进优先级矩阵                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  改进价值                                                                 │
│     ▲                                                                    │
│     │                                                                    │
│  高 │  ┌────────────────────┐  ┌────────────────────┐                  │
│     │  │ 高价值、低成本       │  │ 高价值、高成本       │                  │
│     │  │ Service 核心方法     │  │ 算法边界条件         │                  │
│     │  │ → 立即做             │  │ → 计划做             │                  │
│     │  └────────────────────┘  └────────────────────┘                  │
│     │                                                                    │
│  低 │  ┌────────────────────┐  ┌────────────────────┐                  │
│     │  │ 低价值、低成本       │  │ 低价值、高成本       │                  │
│     │  │ Getter/Setter       │  │ 复杂反射/工具        │                  │
│     │  │ → 跳过             │  │ → 评估跳过           │                  │
│     │  └────────────────────┘  └────────────────────┘                  │
│     │                                                                    │
│     └────────────────────────────────────────► 改进成本                  │
│           低                  中                  高                    │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.3 常见低覆盖率原因

| 原因 | 解决方案 |
|------|---------|
| 异常路径未覆盖 | 添加 `assertThrows` 测试覆盖异常分支 |
| 边界条件未覆盖 | 参数化测试覆盖 min/max/empty/null |
| 异步代码未覆盖 | 使用 Awaitility 等待 + 异步测试 |
| 静态方法难覆盖 | 重构为依赖注入或使用 `mockito-inline` |
| 配置类无测试 | 排除规则（JaCoCo excludes）|

---

## 六、覆盖率 KPI 跟踪

### 6.1 当前问题

- ❌ JaCoCo check 门槛仅 60%（偏低）
- ❌ CI 脚本解析 jacoco.xml 不可靠（XML 格式可能变）
- ❌ `continue-on-error: true` 让 Codecov 失败被忽略
- ❌ 模块级覆盖率无独立控制

### 6.2 改进路线图

| 阶段 | 时间 | 目标 |
|------|------|------|
| **Phase 1** | 2026-Q3 | 行覆盖率 ≥ 60% → 70% |
| **Phase 2** | 2026-Q4 | 行覆盖率 ≥ 70% → 80%，启用分支覆盖率检查 |
| **Phase 3** | 2027-Q1 | 模块级差异化门槛（核心 ≥ 90%，其他 ≥ 75%） |
| **Phase 4** | 2027-Q2 | 集成覆盖率增量检查（PR diff 覆盖率） |

---

## 七、报告查看入口

| 报告类型 | 路径 |
|---------|------|
| 单模块报告 | `backend/meowflow/<module>/target/site/jacoco/index.html` |
| 聚合报告 | `backend/meowflow/target/site/jacoco-aggregate/index.html` |
| Surefire 测试报告 | `backend/meowflow/<module>/target/surefire-reports/` |
| Codecov Dashboard | https://codecov.io/gh/<org>/meowflow |

---

## 八、参考资源

- [JaCoCo 官方文档](https://www.jacoco.org/jacoco/trunk/doc/)
- [JaCoCo Maven Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
- [Codecov 文档](https://docs.codecov.com/)