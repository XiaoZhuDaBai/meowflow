# Dify 「Create New App / Workflow」UX 研究报告

> 数据来源：`langgenius/dify` 仓库 `main` 分支（2026‑07 抓取）
> 关键文件：`web/app/components/app/create-app-modal/index.tsx`、`web/app/components/app/create-app-dropdown.tsx`、`web/app/components/app/create-from-dsl-modal/`、`web/app/components/app/create-app-dialog-shell.tsx`、`web/app/components/explore/`

---

## TL;DR

Dify 用 **「Create 按钮 + Dropdown → 三类入口 Modal」** 模式，把 "Blank / Template / DSL Import" 三种创建路径都收纳在同一个入口。真正创建应用靠 **单个 Dialog（`CreateAppDialogShell`）**，里面同时承载 **AppType 选择器 + 名称 + 描述 + 图标**。**Templates 不是 Modal 的 tab，而是 `/explore` 的独立页面（Home 默认就是这个页）**，符合「模板浏览需要重浏览、轻表单」的认知节奏。DSL 导入是平行的第二个 Modal（带 FROM_FILE / FROM_URL 两个 tab）。

---

## 1. Dify 的「Create New App/Workflow」整体流程（5 步叙事）

```
[1] 用户在 Studio 首页点击右上角「Create」按钮
        ↓
[2] 弹出 Dropdown（CreateAppDropdown），给出三个选项：
    · Create from Blank（空白创建）
    · Create from Template（模板创建 — 可选）
    · Import DSL（上传/填 URL 导入）
        ↓
[3a] Blank  → 弹出「Start from Blank」Dialog
    ↓
[4a] 在 Dialog 里依次：
    · 选 App Type（卡片式，6 个，2 个默认显示 + 「for beginners」折叠扩展 3 个）
    · 填写 App Name（input）
    · 可选：点 emoji/图片选 App Icon（AppIconPicker 弹层）
    · 可选：填写 App Description（textarea，明确标 「Optional」）
        ↓
[5a] 点 Create（快捷键 Cmd/Ctrl+Enter）→ POST createApp
    → toast 成功 → 关闭 Dialog → 用 getRedirection() 路由到对应编辑页
      （workflow → /workflow/<id>/draft，chatbot → /chatbot/<id>/configuration，等）
```

```
[3b] Template → 不打开 Dialog；而是跳到 /explore（或保留 onCreateFromTemplate 回调，
                 由宿主决定）
        ↓
[4b] /explore 页面（ExploreAppListHeader + Category 横排 + Search）展示模板
    ↓
[5b] 点模板卡片 → 「Add to Workspace」→ 走 installApp 后端路径创建应用 → 路由到编辑页
```

```
[3c] DSL → 弹出「Import App」Dialog（两个 tab：From File / From URL）
        ↓
[4c] 选文件 → FileReader 读 → 触发 importDSL(YAML_CONTENT)
              或填 URL → importDSL(YAML_URL)
        ↓
[5c] 三种返回：
    · COMPLETED → 直接路由到编辑页
    · PENDING (DSL 版本不兼容) → 弹出 dsl-confirm-modal 让用户确认降级导入
    · FAILED / COMPLETED_WITH_WARNINGS → toast 报错或警告
```

---

## 2. 入口组件：`CreateAppDropdown`

文件：`web/app/components/app/create-app-dropdown.tsx`

```tsx
<DropdownMenu>
  <DropdownMenuTrigger>
    <Button>{t('operation.create')}</Button>
  </DropdownMenuTrigger>
  <DropdownMenuContent>
    <DropdownMenuItem onSelect={onCreateBlank}>
      {t('newApp.startFromBlank')}
    </DropdownMenuItem>
    {onCreateTemplate && (
      <DropdownMenuItem onSelect={onCreateTemplate}>
        {t('newApp.startFromTemplate')}
      </DropdownMenuItem>
    )}
    <DropdownMenuItem onSelect={onImportDSL}>
      {t('importDSL')}
      {t('newApp.dropDSLToCreateApp')} {/* hint */}
    </DropdownMenuItem>
  </DropdownMenuContent>
</DropdownMenu>
```

**关键观察**：
- Dropdown 把三种「创建路径」平铺，避免了 Nested Modal 的尴尬。
- "Template" 项是 **optional prop**，由宿主页面决定是否显示（首页 explore 上不显示，因为整页就是模板）。
- 每项只是 `DropdownMenuItem`，无 icon 也无说明文字；模态把详情留给 Dialog。

---

## 3. 主 Modal 结构：`CreateAppModal` + `CreateAppDialogShell`

文件：
- `web/app/components/app/create-app-modal/index.tsx`
- `web/app/components/app/create-app-dialog-shell.tsx`

### 3.1 DialogShell（容器）
```tsx
<Dialog open={show} onOpenChange={(nextOpen) => { if (!nextOpen) onClose() }}>
  <DialogContent className={cn(contentClassName, 'overflow-visible')}>
    <DialogTitle>{title}</DialogTitle>     {/* "Start from Blank" */}
    {children}
  </DialogContent>
</Dialog>
```

- 单一 Dialog 实例，**标题 + 内容 + 底部操作栏**。
- 没有 stepper —— 因为 Dify 把所有字段（type + name + desc + icon）放进了一个屏幕。

### 3.2 Dialog 内容布局（基于源码还原）

```
┌────────────────────────────────────────────────────────────────┐
│ Start from Blank                                       ✕  │
├──────────────────────────────────┬─────────────────────────────┤
│                                  │                             │
│  Choose App Type                 │   [ Preview Panel ]         │
│  ┌──────────┐ ┌──────────┐       │                             │
│  │  ✓       │ │          │       │   workflow 长截图            │
│  │ Workflow │ │ Chatflow │       │   (随选中 type 切换)        │
│  │ short    │ │ short    │       │                             │
│  │ desc...  │ │ desc...  │       │                             │
│  └──────────┘ └──────────┘       │                             │
│                                  │                             │
│  ▼ More for beginners            │   ─ 标题 (e.g. "Workflow")  │
│  ┌──────────┐ ┌──────────┐ ┌───┐ │   ─ 一句话面向用户的描述    │
│  │ Chatbot  │ │ Agent    │ │...│ │                             │
│  └──────────┘ └──────────┘ └───┘ │                             │
│                                  │                             │
│  Name *                          │                             │
│  [🤖  emoji bg] [App Name…]      │                             │
│                                  │                             │
│  Description (Optional)          │                             │
│  ┌────────────────────────────┐  │                             │
│  │  …描述                      │  │                             │
│  └────────────────────────────┘  │                             │
│                                  │                             │
│  💡 No idea? See tips            │                             │
│                                  │                             │
├──────────────────────────────────┴─────────────────────────────┤
│ [Cancel]                  [Create (⌘↵)]                       │
└────────────────────────────────────────────────────────────────┘
```

**关键交互**：
- 默认 `appMode = ADVANCED_CHAT`（在 studio 默认开启 chatflow）；如果 `defaultAppMode` 传入（比如从外部点 "New Workflow"），则初始选 Workflow 且 "More for beginners" 自动展开。
- 类型卡片 **单击即选**（不需要 Confirm），右侧 Preview（`AppScreenShot` + `AppPreview`）实时刷新。
- 图标左下角是 emoji + 背景色默认 `#FFEAD5`、`🤖`；点击触发 `AppIconPicker` 浮层，支持 emoji 和上传 image。
- 描述 textarea 有 placeholder，没有长度限制提示，但**明确标了 "Optional"**。
- 底部 [Cancel] 是二级按钮，[Create] 是主按钮，且 **在按钮内显式标注快捷键 ⌘/Ctrl + Enter**。
- Billing limit 通过 `<AppsFull />` 组件插入；permission 通过 `hasPermission()` 拦截；缺权限时按钮置灰。

### 3.3 提交逻辑核心代码
```tsx
const app = await createApp({
  name,
  description,
  icon_type:   appIcon.type,         // 'emoji' | 'image'
  icon:        appIcon.type === 'emoji' ? appIcon.icon : appIcon.fileId,
  icon_background: appIcon.type === 'emoji' ? appIcon.background : undefined,
  mode:        appMode,              // CHAT | AGENT_CHAT | COMPLETION | ADVANCED_CHAT | WORKFLOW
})

try { await trackCreateApp({ source: 'studio_blank', appMode: app.mode }) } catch {}

toast.success('App created')
onSuccess(); onClose()
setNeedRefresh('1')                          // Jotai 原子：让侧边栏重拉
invalidateAppList()                          // React Query 失效
getRedirection(app, push, { ... })           // 根据 mode 跳到对应 route
```

`getRedirection()` 把 `mode` 映射成：
- `WORKFLOW` → `/app/<id>/workflow/draft`
- `ADVANCED_CHAT` → `/app/<id>/chatbot-configuration` 或 `/chat/<id>/...`
- `CHAT` → `/chatbot-configuration`
- `AGENT_CHAT` → `/agent/configuration`
- `COMPLETION` → `/completion-configuration`

---

## 4. App Type 选择器（卡片版）

文件里的逻辑（节选）：

```tsx
const [appMode, setAppMode] = useState(defaultAppMode || AppModeEnum.ADVANCED_CHAT)
const [isAppTypeExpanded, setIsAppTypeExpanded] = useState(
  () => shouldExpandBeginnerAppTypes(defaultAppMode)
)  // 只有 CHAT/AGENT/COMPLETION 是 "beginner" 才会默认展开

// 默认展示两个卡片（高级用户）：Workflow + Chatflow
// 点 "More for beginners" 才显示三个：Chatbot / Agent / Text Generator
```

**Type card 组件**：
```tsx
function AppTypeCard({ icon, title, description, active, onClick }) {
  return (
    <button onClick={onClick}
      className={cn(
        'flex w-full flex-col items-start gap-2 rounded-xl border p-4 text-left',
        active ? 'border-components-input-border-focus bg-state-accent-hover' : 'border-components-input-border-hover bg-components-input-bg-normal',
      )}>
      {icon} {title} <RiArrowRightSLine /> {description}
    </button>
  )
}
```

**模式说明**（i18n key 还原）：
| Enum key | 用户名称 | 一句话技术描述 | 适用人群 |
|---|---|---|---|
| `WORKFLOW` | Workflow | 工作流自动化，多步骤触发 | 高级 |
| `ADVANCED_CHAT` | Chatflow / Advanced | 多轮对话 + 工作流式编排 | 高级（**默认选中**） |
| `CHAT` | Chatbot | 单轮聊天 | Beginner |
| `AGENT_CHAT` | Agent | 工具调用对话 | Beginner |
| `COMPLETION` | Complete App / Text Generator | 单次补全生成 | Beginner |

---

## 5. Template Gallery 怎么展示

文件：`web/app/components/explore/app-list/`、`web/app/components/explore/category.tsx`、`web/app/components/explore/sidebar/index.tsx`

### 路由结构
```
/explore/apps        → 现在直接 redirect 到 /
/explore/installed   → 已经安装到 workspace 的应用列表
/explore              → Explore layout（带 Sidebar）
/                     → (commonLayout)/page.tsx，直接渲染 <AppList />
```

也就是说：**Studio 首页（`/`）本身就是 Explore 页面**，"创建工作流"和"浏览模板"是同一个屏幕的两件事。

### 页面结构（Explore）
```
┌──────────────────────────────────────────────────────────────────┐
│ [Sidebar: Studio · Installed Apps (collapsible)]                  │
├──────────────────────────────────────────────────────────────────┤
│  Apps Title                                [View more →]        │
│  [Search Input]                                                 │
│  Category tabs: [All] [Writing] [Programming] [Agent] [...]      │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐     │
│  │ 📌 [Continue working on…]:  ───────────         ← 可选   │     │
│  └─────────────────────────────────────────────────────────┘     │
│                                                                  │
│  Recommendations / Templates Grid  (app-card * N)                │
└──────────────────────────────────────────────────────────────────┘
```

**`category.tsx` 用的是横向 RadioGroup + Pill 样式**：
- 一个 "All Categories" + 5~10 个具体类别。
- 选中态：浅色背景 + semibold；未选中：透明背景 + tertiary text。
- 不是 Dropdown，是横向滚动的 Chip Row。

**`explore-app-list-header.tsx`**：
```tsx
<Title>{t('apps.title')}</Title>
<Link>{t('apps.viewMore')}</Link>
<SearchInput value={keywords} onChange={...} />
<Category list={categories} value={currCategory} onChange={onCategoryChange} />
```

**`explore-recommendations.tsx`**：组合 `ContinueWork`（"继续未完成的工作流"）+ `LearnDify`（新手教程）。

### 模板卡
- 复用 `app-card`（`web/app/components/explore/app-card/`），里面包含 icon、name、author、description、install count、Use/Add 按钮。
- "Use / Try" 行为不需要先打开 Modal —— 直接调 `installApp` 然后 `getRedirection` 到编辑页。

### "Templates" 是页面不是 Modal

这是 **Dify 的核心 UX 决策**：模板发现需要空间浏览（搜索 + 分类 + 卡片），所以把它独立成路由 `/explore`，**而不放进创建 Modal**。点击 Studio 的 [Create → From Template] 不是再开一个 Modal，而是 **保留 `onCreateFromTemplate` 回调让宿主页面决定行为** —— 一般就是 `push('/explore')` 或滚动到模板区域。

---

## 6. DSL / Import 功能

文件：
- `web/app/components/app/create-from-dsl-modal/index.tsx`（14.5 KB，主 dialog）
- `web/app/components/app/create-from-dsl-modal/uploader.tsx`（拖拽上传）
- `web/app/components/app/create-from-dsl-modal/dsl-confirm-modal.tsx`（版本不兼容确认弹窗）
- `web/app/components/app/create-from-dsl-modal/types.ts`

### Modal 结构

```
┌──────────────────────────────────────────────────┐
│ Import App                              ✕       │
├──────────────────────────────────────────────────┤
│  [From File] | [From URL]                        │
│                                                  │
│  FROM_FILE:                                      │
│  ┌────────────────────────────────────────────┐  │
│  │         Drop YAML here, or [Browse]        │  │
│  │         (dragenter 高亮态)                │  │
│  └────────────────────────────────────────────┘  │
│  选中后：显示文件名 + 大小 + 删除按钮            │
│                                                  │
│  FROM_URL:                                       │
│  [https://example.com/app.yml           ]       │
│                                                  │
├──────────────────────────────────────────────────┤
│  [Cancel]                [Create (⌘↵)]         │
└──────────────────────────────────────────────────┘
```

### 关键代码
```tsx
const tabs = [
  { key: CreateFromDSLModalTab.FROM_FILE, label: 'DSL File' },
  { key: CreateFromDSLModalTab.FROM_URL,  label: 'DSL URL'  },
]

const onCreate = async () => {
  const response = currentTab === FROM_FILE
    ? await importDSL({ mode: YAML_CONTENT, yaml_content: fileContent })
    : await importDSL({ mode: YAML_URL,    yaml_url:    dslUrlValue })

  const { status, app_id, app_mode, imported_dsl_version, current_dsl_version, warnings } = response

  if (status === COMPLETED) {
    // 创建成功 → toast + 跳转
    getRedirection(...)
  } else if (status === PENDING) {
    // DSL 版本与系统版本不兼容 → 弹 dsl-confirm-modal 让用户降级
    setShowErrorModal(true)
  } else if (status === COMPLETED_WITH_WARNINGS) {
    // 创建成功但有警告（缺插件等）→ toast warning
  } else {
    toast.error(...)
  }
}
```

### 拖拽接入

Dropdown 的 "Import DSL" 项触发 `onImportDSL`。同时整个 Studio 区域支持 **拖一个 .yml 文件进任何页面 → 自动调起 CreateFromDSLModal**：
```tsx
// 在 explore-app-list 里通过 droppedFile prop 传入
<CreateFromDSLModal droppedFile={droppedFile} ... />
```

而且 `uploader.tsx` 自带 `dragenter / dragover / dragleave / drop` 四个监听，支持在 Modal 内再次拖拽替换。

### 版本兼容确认弹窗（dsl-confirm-modal.tsx）
- 当 `imported_dsl_version ≠ current_dsl_version` 时弹出。
- 告诉用户「你导入的是 X 版本，本系统只支持 Y 版本，是否继续」。
- 二次确认后调 `importDSLConfirm({ import_id })` 真正完成导入。

---

## 7. 一个完整的工作流特例：Default `appMode = WORKFLOW`

Dify 把 **`defaultAppMode` 作为 prop** 注入 CreateAppModal。当用户从 Workflow / Studio 区域专门点击 "New Workflow" 时：

```tsx
<CreateAppModal
  show={showCreateModal}
  onClose={() => setShowCreateModal(false)}
  onSuccess={() => setShowSuccessModal(true)}
  defaultAppMode={AppModeEnum.WORKFLOW}   // ← 关键：指定类型
/>
```

注入 `WORKFLOW` 后：
1. 初始选中的是 Workflow 卡片（不是默认的 ADVANCED_CHAT）。
2. 因为 `shouldExpandBeginnerAppTypes(WORKFLOW) === false`，所以 "More for beginners" 区是默认折叠的。
3. 用户不用选 type，可以直接填 Name → Create。

---

## 8. 表单字段与默认值总结

| 字段 | 是否必填 | 默认 | 控件 |
|---|---|---|---|
| App Type | ✅ | ADVANCED_CHAT（或 `defaultAppMode`） | 卡片选择器 |
| Name | ✅ | "" | `Input` |
| Icon | ❌ | `{ type: 'emoji', icon: '🤖', background: '#FFEAD5' }` | `AppIconPicker`（emoji + image） |
| Description | ❌ | "" | `Textarea`，**显式标 Optional** |
| Permission | — | 自动检测 | `hasPermission(...)` |
| Billing limit | — | 自动检测 | `<AppsFull />` 内联 |

`createApp` 请求体字段：
```ts
{
  name: string,
  description: string,
  icon_type: 'emoji' | 'image',
  icon: string,                  // emoji char or file_id
  icon_background: string | undefined,
  mode: AppModeEnum,            // CHAT | AGENT_CHAT | COMPLETION | ADVANCED_CHAT | WORKFLOW
}
```

---

## 9. 推荐的借鉴项（给喵流）

> 喵流的 "Create New Workflow" 当前是直接进 detail 页，缺少类型选择、模板引导、拖拽导入。下面是按优先级排序的 5 个 Dify 模式，建议直接落地：

### 🟢 P0 — 必须做
1. **入口 Dropdown 收口 3 类创建路径**
   把 "Blank / From Template / Import" 放进同一个 `Create` 按钮的下拉，避免路由分散。`CreateAppDropdown.tsx` 几乎可以照搬。

2. **单 Dialog 收口所有创建字段**
   别打开 Wizard / Stepper。把 **类型选择 + 名称 + 描述 + 图标** 在一个屏幕内完成。
   - 类型用 2 个默认可见 + 折叠"更多"的渐进式披露（避免新用户被 5 个选项淹没）。
   - 描述栏一定要 **标 Optional**，新用户才不会卡住。
   - Create 按钮显示快捷键 `⌘/Ctrl + Enter`，对应一个全局 hotkey。

3. **`defaultAppMode` Prop 支持直链**
   我们的"工作流"产品只要一种类型，所以 `defaultAppMode='Workflow'`，用户点 "New Workflow" 直接进 Dialog（已经默认选好类型），不用被无关类型分散注意力。

### 🟢 P1 — 强烈建议
4. **Templates 进 Home，不是 Modal**
   把当前 "Studio 主页" 改造成 **Explore + 我的工作流**的混合页（类似 Dify），新建/继续/模板同屏。模板区放：
   - 顶部 Search + 横向类别 Chip（用 `<Category />` 那个 RadioGroup 风格）
   - 下面是模板卡片网格
   - 头部的 "继续未完成" 区（Dify 叫 `ContinueWork`）

5. **拖 DSL 文件进任意页面 = 自动弹导入 Modal**
   全局监听 `dragover / drop`，捕获 `.yml/.yaml/.json` 文件，弹出与"点击 Import"完全同一个 Modal。Import 弹层支持 **FROM_FILE / FROM_URL** 两个 tab + **版本不兼容确认弹窗**。

### 🟡 P2 — 锦上添花
6. **`getRedirection()` 路由表**
   在后端 create 接口返回 app 后，前端根据 mode 决定跳到 /workflow/draft, /chat/configuration 等。这样后端 `mode` 一变前端不用改。

7. **Permission / Billing Full 三态**
   - 无权限：按钮置灰 + tooltip
   - 余额满：插入 `<AppsFull />` 升级提示
   - 正常：正常提交流程
   这三态用同一个 hook（`useProviderContext + hasPermission + useSuspenseQuery systemFeatures`）集中处理。

8. **Toast + invalidate queries 一把梭**
   提交成功后 `setNeedRefresh + invalidateAppList` 让侧边栏和工作流列表自动刷新，**不要靠手动 reload**。

---

## 10. 关键文件清单（Dify 仓库 `main` 分支）

| 路径 | 用途 | 行数 |
|---|---|---|
| `web/app/components/app/create-app-dropdown.tsx` | 入口下拉（Blank/Template/DSL） | 4.2 KB |
| `web/app/components/app/create-app-modal/index.tsx` | Blank 创建 Dialog | 21 KB |
| `web/app/components/app/create-app-dialog-shell.tsx` | Dialog 容器 | 1.7 KB |
| `web/app/components/app/create-from-dsl-modal/index.tsx` | DSL 导入 Dialog | 14.5 KB |
| `web/app/components/app/create-from-dsl-modal/uploader.tsx` | 拖拽上传组件 | 5.9 KB |
| `web/app/components/app/create-from-dsl-modal/dsl-confirm-modal.tsx` | DSL 版本不兼容确认 | 2.4 KB |
| `web/app/components/app/create-from-dsl-modal/types.ts` | `CreateFromDSLModalTab` enum | — |
| `web/app/components/base/app-icon-picker/` | Emoji + Image 图标选择 | — |
| `web/app/components/explore/app-list/index.tsx` | 模板首页 | 21 KB |
| `web/app/components/explore/app-list/explore-app-list-header.tsx` | 搜索 + 类别 chip | 1.8 KB |
| `web/app/components/explore/category.tsx` | RadioGroup Pill 类目筛选 | 2.6 KB |
| `web/app/components/explore/continue-work/` | "继续未完成的工作流"区块 | — |
| `web/app/components/explore/app-card/` | 模板卡片 | — |
| `web/app/components/explore/sidebar/index.tsx` | Explore 侧边栏（含折叠） | 7.5 KB |
| `web/app/(commonLayout)/page.tsx` | Studio 主页（直接渲染 AppList） | 384 B |
| `web/app/(commonLayout)/explore/apps/page.tsx` | 旧 `/explore/apps` 重定向到 `/` | 682 B |
| `web/app/utils/app-redirection.ts` | mode → 编辑页路由映射 | — |
| `web/app/utils/dsl-import-warning.ts` | DSL 警告文案的 i18n | — |
| `web/service/apps.ts` | `createApp / importDSL / importDSLConfirm` API | — |

---

## 11. 一句话总结

> **Dify 把 "Create" 拆成 "下拉选路径（3 个选项）+ 单 Dialog 表单 + 独立 Explore 页面" 三段式**：下拉是导航，Dialog 收集创建参数，Explore 是模板发现场景。这个分层既保持了创建流程的轻量（一屏完成），又让模板浏览有充分空间 —— 是喵流可以低成本复用的成熟模式。
