# 改名日志（RENAME_LOG）

## 2026-07-12：猪小能 (PigFlow) → 喵流 (MeowFlow)

### 背景

原项目名"猪小能 / PigFlow"过于泛化，缺少辨识度，也不利于品牌延展。
用户养了一只叫 **汉堡** 的金渐层猫，提议结合猫猫和工作流做更人格化的品牌表达。

经候选筛选（15 个方向），最终选定 **喵流 / MeowFlow**。

### 新品牌

| 项 | 值 |
| --- | --- |
| 中文名 | 喵流 |
| 英文名 | MeowFlow |
| Slogan | 让每一份工作，像猫一样优雅流动 |
| 副 Slogan | 流程，一撸就顺 |
| 品牌 IP | 汉堡 🐱（金渐层猫） |
| 主色 | 金渐层 `#E5C07B → #C19A6B → #8B6F47` |
| Logo | `frontend/meowflow-ui/public/favicon.svg` |

### 改名映射表

| 旧 | 新 |
| --- | --- |
| 猪小能 | 喵流 |
| PigFlow | MeowFlow |
| `pigflow-ui`（目录） | `meowflow-ui`（目录） |
| `pigflow`（后端根） | `meowflow`（后端根） |
| `com.pigflow.*` | `com.meowflow.*` |
| `pigflow-*`（Maven 模块） | `meowflow-*`（Maven 模块） |
| `pigflow.*`（localStorage key） | `meowflow.*`（localStorage key） |
| `@pigflow.com` | `@meowflow.com` |
| `pigflow.com` | `meowflow.com` |
| 数据库 `sys_*` / `wf_*` / `ai_*` 等"裸模块前缀" | `mf_sys_*` / `mf_wf_*` / `mf_ai_*` 等（**增加品牌前缀 mf_**） |

### 修改清单

- ✅ PRD.md 顶部名称 + 项目结构说明
- ✅ frontend/meowflow-ui/index.html title
- ✅ frontend/meowflow-ui/package.json name + description
- ✅ frontend/meowflow-ui/README.md
- ✅ frontend/meowflow-ui/src/views/Login.vue Logo + Slogan
- ✅ frontend/meowflow-ui/src/components/layout/AppHeader.vue Logo
- ✅ frontend/meowflow-ui/src/utils/constants.ts APP_NAME + STORAGE_KEYS
- ✅ frontend/meowflow-ui/src/mock/{users,llm,templates}.ts 邮箱/作者
- ✅ frontend/meowflow-ui/src/views/team/Manage.vue 邮箱
- ✅ frontend/meowflow-ui/.env.{development,production}
- ✅ frontend/meowflow-ui/public/favicon.svg（汉堡金渐层猫）
- ✅ backend/meowflow/pom.xml groupId / artifactId / modules / name / description
- ✅ README.md
- ✅ docs/** （24 个 md/html 全量扫描后批量替换）
- ✅ .idea/{encodings,misc,modules}.xml

### 仍待办（下一波）

- ✅ **2026-07-12 完成**：数据库正式前缀定为 **`mf_<module>_<entity>`**（MeowFlow 品牌前缀），全套脚本已落到 `scripts/sql/`
  - 00-init.sql（库创建 + 公共函数）
  - 01-schema.sql（**29 张表 DDL**）
  - 02-index.sql（索引 + pg_trgm / pgvector 提示）
  - 03-data.sql（默认账号 + 菜单 + 模型 + 集成）
  - apply.sh / reset.sql / README.md
  - 注：旧文档里猜想过的 `pig_*` / `tbl_pig_*` 前缀**从未真正落地**，所以本次无需迁移；database.md 中先前的 `sys_*` / `wf_*` 现统一升级为 `mf_sys_*` / `mf_wf_*`
- ⏳ 实际重命名目录 `backend/pigflow/` → `backend/meowflow/`（文件级 mv）
- ⏳ 实际重命名目录 `frontend/pigflow-ui/` → `frontend/meowflow-ui/`（文件级 mv）
- ⏳ 后端 Java 代码内 `package com.pigflow.*` → `com.meowflow.*`（IDE 重构）
- ⏳ Logo banner SVG（README 顶部那张，已落 `docs/assets/logo-banner.svg`，README 引用待切换为相对路径）
- ⏳ 域名 `meowflow.com` 注册状态核查
- ⏳ 商标注册评估

### 注意事项

1. **不要动 node_modules**：改名后 `npm install` 会重新生成依赖，不要手工替换其中的 `pigflow` 字符串。
2. **dist/** 是构建产物，已过期，构建后会重新生成，当前留下的 `pigflow` 字样可忽略。
3. **数据库表名**没动 —— 这是设计上的"反历史重命名"，方便老数据迁移。
