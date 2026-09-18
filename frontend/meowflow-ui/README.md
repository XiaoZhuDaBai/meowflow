# 喵流 (MeowFlow) - 前端工程

> Vue 3 + TypeScript + Vite + Element Plus + AntV G6 v5,带 mock 数据可独立运行。
> 由汉堡 🐱（金渐层）代言,流程，一撸就顺。

## 快速开始

> 💡 目录中 `frontend/pigflow-ui/` 是历史目录名（待下一波迁移到 `frontend/meowflow-ui/`），临时仍可工作。
> 一次别名或 PowerShell `cmd /c ren pigflow-ui meowflow-ui` 即可。

```bash
cd "D:\Code\猪小能\frontend\pigflow-ui"
npm install
npm run dev          # http://127.0.0.1:5173
npm run build        # 产出 dist/
npm run build:fast   # 跳过类型检查,更快
npm run preview      # 预览构建产物
```

> Node ≥ 18,推荐 Node 22(本机已验证)。

## 目录结构

```
meowflow-ui/
├── public/
├── src/
│   ├── api/                # axios + 业务 API (workflow/template/stat/log/user/system)
│   ├── mock/               # 路由化的 mock 数据,接入 axios 适配层
│   ├── stores/             # Pinia:app / user / workflow / log
│   ├── layouts/            # DefaultLayout (header + nav + content)
│   ├── components/
│   │   ├── layout/         # AppHeader / NavMenu / UserMenu
│   │   ├── common/         # EmptyState / StatusTag / JsonViewer
│   │   └── workflow/Canvas # 工作流编辑器零件 (画布、工具栏、节点库、配置面板)
│   ├── views/              # 页面 (Login、Dashboard、workflow/*、template、stat、log、user、team、system、error/404)
│   ├── utils/              # 通用工具
│   ├── types/              # 共享类型
│   ├── assets/styles/      # 全局样式与 CSS 变量
│   ├── router/             # 路由 + 守卫
│   ├── App.vue
│   └── main.ts
├── vite.config.ts
└── .env.development / .env.production
```

## 切到真实后端

1. 在 `.env.production` 中修改:
   ```
   VITE_USE_MOCK=false
   VITE_API_BASE_URL=https://your-api.example.com/api
   ```
2. `src/api/http.ts` 会自动绕过 mock 适配器,直接发送请求到 `VITE_API_BASE_URL`。
3. 响应需要符合 `{ code, message, data }` 的统一信封格式。

## 页面 ↔ 路由 ↔ Mock 接口 映射表

| PRD 页面 | 路由 | 源文件 | 主要 mock 接口 |
| --- | --- | --- | --- |
| P01 工作流列表 | `/workflows` | `views/workflow/List.vue` | `GET /workflows` |
| P02 可视化编辑器 | `/editor/:id?` | `views/workflow/Editor.vue` | `GET /workflows/:id` · `PUT /workflows/:id` · `POST /workflows/:id/publish` · `POST /workflows/:id/run` |
| P03 节点配置面板 | 编辑器右侧 | `components/workflow/Canvas/ConfigPanel.vue` | `GET /nodes/catalog` |
| P04 模板市场 | `/templates` | `views/template/Market.vue` | `GET /templates` · `POST /templates/:id/use` |
| P05 执行日志 | `/logs` · `/workflows/:id/detail` | `views/log/List.vue` · `views/workflow/Detail.vue` | `GET /logs` · `GET /logs/:execId` |
| P06 统计看板 | `/dashboard` · `/statistics` | `views/Dashboard.vue` · `views/stat/Dashboard.vue` | `GET /stats/overview` · `GET /stats/trend` · `GET /stats/cost` |
| P07 用户中心 | `/profile` | `views/user/Profile.vue` | `GET /users/me` · `PUT /users/me` |
| P08 团队管理 | `/team` | `views/team/Manage.vue` | (mock 静态数据) |
| P09 系统设置 | `/settings` | `views/system/Settings.vue` | `GET /system/llm-models` · `GET /system/integrations` |
| — Login | `/login` | `views/Login.vue` | — (一键 mock 登录) |
| — 404 | `/:pathMatch(.*)*` | `views/error/404.vue` | — |

## Mock 接口完整清单

| Method | URL | 说明 |
| --- | --- | --- |
| GET | `/workflows` | 工作流列表 (支持 keyword/category/status 过滤) |
| POST | `/workflows` | 新建工作流 |
| GET | `/workflows/:id` | 工作流详情 |
| PUT | `/workflows/:id` | 更新工作流 |
| DELETE | `/workflows/:id` | 删除工作流 |
| POST | `/workflows/:id/publish` | 发布 |
| POST | `/workflows/:id/stop` | 停止 |
| POST | `/workflows/:id/duplicate` | 复制 |
| POST | `/workflows/:id/run` | 测试运行(创建 execution 记录) |
| GET | `/templates` | 模板列表 |
| GET | `/templates/:id` | 模板详情 |
| POST | `/templates/:id/use` | 用模板创建工作流 |
| GET | `/stats/overview` | 总览数据 |
| GET | `/stats/trend` | 7 日趋势 |
| GET | `/stats/cost` | 成本构成 |
| GET | `/logs` | 执行日志列表 |
| GET | `/logs/:execId` | 单条执行详情 |
| GET | `/users/me` | 当前用户 |
| PUT | `/users/me` | 更新用户资料 |
| GET | `/system/llm-models` | LLM 模型列表 |
| PUT | `/system/llm-models/:id` | 更新模型 |
| GET | `/system/integrations` | 集成列表 |
| PUT | `/system/integrations/:id` | 更新集成 |
| GET | `/nodes/catalog` | 节点目录(26 个节点,触发器/AI/流程/工具/通知) |

所有数据持久化在 `localStorage` 的 `meowflow.*` 键下,清空浏览器存储即可重置。

## 技术栈

- **Vue 3.4** `<script setup>` 组合式 API
- **TypeScript 5**
- **Vite 5** 构建,原生 ESM
- **Element Plus 2.7**(自动按需导入,`unplugin-auto-import` + `unplugin-vue-components`)
- **AntV G6 v5** 工作流画布
- **Vue Router 4** + **Pinia 2**
- **Axios 1** + 拦截器 + Mock 适配层
- **Font Awesome 6**(CDN)
- **ECharts 5**(统计看板)
- **dayjs**(日期)

## 已实现 vs 已 stub

✅ 已完整实现
- 9 个 PRD 页面 + Login + 404 全部联通
- 编辑器:节点库、拖拽添加、G6 v5 画布、连线、运行模拟、底部日志
- 上面 23 个 mock 接口全部命中
- localStorage 持久化 mock 工作流
- 自动登录 + 权限说明弹窗
- 路由守卫:无 token → 跳 `/login`

⚠ 已 stub 但 UI 可用
- 团队管理 (`/team`):静态 mock 数据,无可写接口
- WebSocket 实时日志推送:仅前端 `setTimeout` 模拟,可替换为真实 SSE
- 工作流导入/导出 / 版本回滚:API 路由表中预留,UI 未做按钮
- G6 5.x 节点 badge 完全自定义(`tool` 节点底栏);带自定义 HTML overlay,不依赖 G6 自身的 label 字段

❌ 未实现
- 真实后端请求(VITE_USE_MOCK=false 时需要的后端已就绪后再接入)
- 国际化(目前仅 zh-CN)
- 单元测试(可加 vitest)

## 常见问题

- **端口被占用?** Vite 默认 `5173`,会在 `server.port` 段调整;启动时会自动尝试下一个可用端口。
- **首次加载样式闪烁?** Element Plus 通过 unplugin 按需注入,确保给 `main.ts` 引入 `global.css`。
- **是否影响其他工作目录?** 不会,前端目录在 `frontend/meowflow-ui/`,与 docs、backend 完全隔离。
