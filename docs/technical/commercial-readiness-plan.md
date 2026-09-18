# MeowFlow 可商用化补齐计划 (P0 + P1)

> 目标: 在 8 周内将 MeowFlow 从"功能验证"提升到"可商用"水平  
> 范围: 前端体验、模板生态、用户文档三大方向  
> 参考对标: Coze Studio / Dify

---

## 📋 总览

| 阶段 | 周期 | 目标 |
|------|------|------|
| 🔴 P0 阶段 | 第 1-4 周 | 实时调试能力 + 编辑器智能体验 |
| 🟡 P1 阶段 | 第 5-8 周 | 模板市场 + 用户文档 + 创建流程优化 |
| 🟢 P2 阶段 | 第 9-12 周 | Webhook 调试器 + i18n (可选) |

---

## 🔴 P0 阶段：核心交互补齐 (4 周)

### P0-1: 实时调试能力 (Week 1-2)

| 子任务 | 优先级 | 工时 | 状态 |
|--------|-------|------|------|
| 后端节点级断点 API | 🔴 高 | 2d | ⬜ |
| 前端断点标记 (画布图标) | 🔴 高 | 1d | ⬜ |
| 单步执行控制条 | 🔴 高 | 2d | ⬜ |
| 节点执行快照/变量查看 | 🔴 高 | 3d | ⬜ |
| 调试模式开关 + 状态栏 | 🔴 高 | 1d | ⬜ |

**后端新增**:
```java
// ExecutionController.java
POST /api/executions/{id}/pause
POST /api/executions/{id}/resume
POST /api/executions/{id}/step
GET  /api/executions/{id}/breakpoints

// WorkflowEngine.java
- Set<String> breakpoints = new HashSet<>();
- AtomicReference<ExecutionState> state = new AtomicReference<>(RUNNING);
- 节点执行前检查 state.get() == PAUSED, 若 true 则阻塞等待
```

**前端新增**:
```
src/components/workflow/DebugBar/        # 调试控制条
  - DebugBar.vue                         # 步进/继续/停止
  - BreakpointBadge.vue                  # 节点断点标记
src/components/workflow/VariableInspect/ # 变量查看器 (已有,需增强)
src/stores/debug.ts                       # 调试状态管理
```

**验收标准**:
- ✅ 用户可在任意节点点击设置断点
- ✅ 执行到断点时自动暂停
- ✅ 查看上游节点全部输出变量
- ✅ 支持单步执行 / 继续 / 停止
- ✅ 调试日志独立展示 (debug 级别)

---

### P0-2: 编辑器智能体验 (Week 2-3)

| 子任务 | 优先级 | 工时 | 状态 |
|--------|-------|------|------|
| 节点搜索面板 (Ctrl+K) | 🔴 高 | 1d | ⬜ |
| 智能推荐节点 (上下文感知) | 🟡 中 | 2d | ⬜ |
| 拖拽优化 (对齐辅助线) | 🟡 中 | 2d | ⬜ |
| 撤销/重做优化 (历史栈) | 🟢 低 | 1d | ⬜ |
| 自动保存 + 版本快照 | 🔴 高 | 2d | ⬜ |

**前端新增/改造**:
```
src/components/workflow/NodePalette/
  - NodeSearchPalette.vue          # Ctrl+K 命令面板
  - NodeRecommendation.vue         # 智能推荐 (基于前置节点类型)
src/composables/
  - useAutoLayout.ts               # 自动布局 (Dagre.js)
  - useAutoSave.ts                 # 自动保存 (debounce 2s)
  - useUndoRedo.ts                 # 撤销重做 (Operation Stack)
```

**对齐辅助线**:
```typescript
// useAlignmentGuides.ts
- 拖拽节点时检测与其他节点的 x/y 对齐
- 渲染蓝色虚线提示
- 距离 ≤ 8px 时自动吸附
```

---

### P0-3: 节点配置面板 (Week 3-4)

| 子任务 | 优先级 | 工时 | 状态 |
|--------|-------|------|------|
| Setter 抽象层 (参考 Coze) | 🔴 高 | 3d | ⬜ |
| Formily 动态表单集成 | 🟡 中 | 2d | ⬜ |
| 变量引用器 (Picker) 优化 | 🔴 高 | 2d | ⬜ |
| 配置实时校验 | 🟡 中 | 1d | ⬜ |
| 模板片段库 (SnippetSelector) | 🟢 低 | 2d | ⬜ |

**已存在**:
```
- VariablePicker.vue (需增强)
- SnippetSelector.vue (已有,需增强内容)
- AbstractNodeConfig.vue (需重构)
```

**Setter 抽象层**:
```typescript
// types/setter.ts
export interface SetterDefinition {
  type: 'string' | 'number' | 'boolean' | 'select' | 'json' | 'code' | 'prompt';
  label: string;
  required?: boolean;
  default?: any;
  options?: Array<{ label: string; value: any }>;
  validation?: (value: any) => string | null;
}

// registry/setters.ts
const SETTERS: Record<string, SetterDefinition> = {
  prompt: { type: 'code', language: 'markdown', ... },
  model: { type: 'select', options: AIModels, ... },
};
```

---

## 🟡 P1 阶段：模板生态与文档 (4 周)

### P1-1: 模板市场 (Week 5-6)

**目标**: 从当前 1 个客服模板 → 50+ 行业模板

| 类别 | 数量 | 模板 |
|------|------|------|
| 🤖 客服场景 | 8 | 自动回复、智能工单、多轮问答、FAQ 检索、满意度调研、转人工决策、坐席分配、客服质检 |
| 📈 营销场景 | 8 | 内容生成、社媒发布、营销文案、用户画像、A/B 测试、邮件营销、线索评分、活动报名 |
| 💼 办公场景 | 8 | 周报生成、会议纪要、请假审批、报销流程、数据报表、合同审核、招聘筛选、日程安排 |
| 🔍 数据分析 | 8 | 日志分析、异常检测、SQL 生成、数据清洗、可视化生成、报表生成、用户行为分析、转化漏斗 |
| 💰 金融场景 | 6 | 风控审核、贷款评估、合同抽取、舆情监控、行情分析、量化交易辅助 |
| 🎓 教育场景 | 6 | 作业批改、试卷生成、学习路径、智能问答、知识图谱、教研分析 |
| 🏥 医疗场景 | 4 | 病例摘要、药品咨询、影像初筛、随访提醒 |
| ⚙️ 开发者场景 | 6 | CI/CD、代码审查、Bug 分类、文档生成、API 测试、依赖分析 |

**新增模板文件**:
```
src/mock/builtinTemplates/
  index.ts                    # 统一导出
  customer-service.ts         # 8 个客服模板
  marketing.ts                # 8 个营销模板
  office.ts                   # 8 个办公模板
  data-analysis.ts            # 8 个数据分析模板
  finance.ts                  # 6 个金融模板
  education.ts                # 6 个教育模板
  medical.ts                  # 4 个医疗模板
  developer.ts                # 6 个开发者模板
```

**模板元数据增强**:
```typescript
export interface BuiltinTemplate {
  // ... 现有字段
  difficulty: 'easy' | 'medium' | 'hard';        // 难度
  estimatedSetupMinutes: number;                  // 预计搭建时长
  requiredModels: string[];                       // 所需模型
  requiredIntegrations: string[];                // 所需集成
  previewImage?: string;                          // 预览图 (静态截图)
  videoUrl?: string;                              // 演示视频
}
```

---

### P1-2: 用户文档体系 (Week 6-7)

| 子任务 | 优先级 | 工时 | 状态 |
|--------|-------|------|------|
| 用户手册 (USER_GUIDE.md) | 🔴 高 | 2d | ⬜ |
| 快速开始 (QUICKSTART.md) | 🔴 高 | 1d | ⬜ |
| 最佳实践 (BEST_PRACTICES.md) | 🟡 中 | 2d | ⬜ |
| 节点参考手册 (NODE_REFERENCE.md) | 🔴 高 | 3d | ⬜ |
| FAQ 常见问题 (FAQ.md) | 🟡 中 | 1d | ⬜ |
| 视频脚本 (5 个核心视频) | 🟢 低 | 2d | ⬜ |

**新增文档**:
```
docs/user/
├── README.md                       # 用户文档入口
├── QUICKSTART.md                   # 5分钟快速上手
├── USER_GUIDE.md                   # 完整使用手册
│   ├── 工作流编排基础
│   ├── 节点配置详解
│   ├── 知识库管理
│   ├── 模型管理
│   ├── 模板与复用
│   ├── 调试与监控
│   └── 团队协作
├── NODE_REFERENCE.md               # 38 节点详细参考
├── BEST_PRACTICES.md               # 最佳实践 20 例
├── FAQ.md                          # 常见问题 30 问
└── videos/                         # 视频脚本
    ├── 01-quickstart.md
    ├── 02-workflow-basics.md
    ├── 03-knowledge-base.md
    ├── 04-agent.md
    └── 05-debugging.md
```

---

### P1-3: 创建流程优化 (Week 7-8)

参考 Dify `CreateAppModal` 三段式：

| 子任务 | 优先级 | 工时 | 状态 |
|--------|-------|------|------|
| CreateAppDialogShell | 🔴 高 | 1d | ⬜ |
| CreateBlankTab 增强 | 🟡 中 | 1d | ⬜ |
| CreateTemplateTab 增强 | 🔴 高 | 2d | ⬜ |
| CreateDslTab 增强 | 🟡 中 | 1d | ⬜ |
| /explore 路由 + 模板浏览页 | 🔴 高 | 2d | ⬜ |

**当前已有** (位于 `src/components/workflow/`):
```
- CreateWorkflowDialog.vue
- CreateBlankTab.vue
- CreateTemplateTab.vue
- CreateDslTab.vue
```

**改造要点**:
```
Dify: [Create 按钮 + Dropdown] → [三类入口 Modal] → [Explore 独立页]
喵流: 改造为同样的三层结构:
  1. 顶部 Create 按钮 → 弹出下拉 (Blank / From Template / From DSL)
  2. 点击后弹出单个 Dialog (类型选择 + 名称 + 描述 + 图标)
  3. 模板选择跳转 /explore 页面 (独立路由)
```

---

## 📊 整体节奏

```
Week 1-2  P0-1 实时调试能力 ─┐
Week 2-3  P0-2 编辑器体验    ├→  发布 v1.5 "可调试"
Week 3-4  P0-3 节点配置     ─┘
Week 5-6  P1-1 模板市场 (50个) ─┐
Week 6-7  P1-2 用户文档体系    ├→  发布 v1.6 "可商用"
Week 7-8  P1-3 创建流程优化  ─┘
Week 9+   可选 P2 (Webhook调试器 / i18n)
```

---

## 🎯 验收指标

| 指标 | 当前 | 目标 |
|------|------|------|
| 编辑器操作流畅度 (60fps) | ~30fps | ≥60fps |
| 节点配置代码量 (单节点平均) | ~200 行 | <100 行 (Setter 抽象) |
| 内置模板数量 | 1 | 50+ |
| 用户文档完整度 | 0% | 100% |
| 创建到运行平均时间 | 10 分钟 | <3 分钟 |
| 调试时间 (典型流程) | 30 分钟 | <5 分钟 |
| 整体可商用评分 | 70 分 | 95 分 |

---

**创建日期**: 2026-09-05  
**计划周期**: 8 周  
**目标版本**: v1.6 (可商用版)
