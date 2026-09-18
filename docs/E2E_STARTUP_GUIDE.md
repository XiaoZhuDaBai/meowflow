# MeowFlow 端到端启动指南

本指南帮助你从零开始启动完整的 MeowFlow 系统，并验证工作流能够端到端跑通。

## 前置要求

- Docker Desktop (运行基础设施)
- Java 17+ (已安装在 `C:\Users\11057\.jdks\ms-17.0.19`)
- Maven 3.x (已安装在 `D:\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3`)
- Node.js 18+ (运行前端)
- PowerShell 5.1+ (Windows)

## 快速启动 (3 步)

### 方式 1：开发模式（推荐）

适合：前端开发、调试后端

```powershell
# 步骤 1：启动基础设施
cd D:\Code\喵流\backend\meowflow
.\start-infra.ps1

# 步骤 2：在 IDE 中启动以下微服务（按顺序）
# - meowflow-gateway    (8080)
# - meowflow-user       (8081)
# - meowflow-workflow   (8082)
# - meowflow-infra      (8083)
# - meowflow-monitor    (8084) [可选]
# - meowflow-executor   (8085) [可选]

# 步骤 3：启动前端（真实模式）
cd D:\Code\喵流\frontend\meowflow-ui
$env:VITE_USE_MOCK='false'
npm run dev
```

访问：http://localhost:5173

### 方式 2：完全自动化

适合：演示、完整验证

```powershell
# 一键启动所有服务
cd D:\Code\喵流\backend\meowflow
.\start-all.ps1

# 等待 30 秒后启动前端
cd D:\Code\喵流\frontend\meowflow-ui
$env:VITE_USE_MOCK='false'
npm run dev
```

### 方式 3：跳过构建（使用已有 JAR）

```powershell
cd D:\Code\喵流\backend\meowflow
.\start-all.ps1 -SkipBuild
```

## 验证系统状态

运行验证脚本检查所有服务是否正常：

```powershell
cd D:\Code\喵流
.\verify-e2e.ps1
```

## 服务端口清单

### 基础设施

| 服务       | 端口           | 管理界面/用户名          |
|-----------|----------------|------------------------|
| Postgres  | 5432          | meowflow/meowflow123   |
| Redis     | 6379          | -                      |
| Nacos     | 8848          | http://localhost:8848/nacos |
| RabbitMQ  | 5672, 15672   | http://localhost:15672 (guest/guest) |
| MinIO     | 9000, 9001    | http://localhost:9001 (minioadmin/minioadmin) |

### 微服务

| 服务       | 端口  | 用途                    |
|-----------|------|------------------------|
| Gateway   | 8080 | 网关（主入口）           |
| User      | 8081 | 用户认证                |
| Workflow  | 8082 | 工作流核心              |
| Infra     | 8083 | 基础设施（邮件、对象存储）|
| Monitor   | 8084 | 监控日志                |
| Executor  | 8085 | 执行器注册              |

### 前端

| 服务       | 端口  | 用途                    |
|-----------|------|------------------------|
| Dev Server| 5173 | 开发服务器              |

## 端到端测试步骤

启动系统后，按以下步骤验证工作流能否跑通：

### 1. 登录系统

1. 访问 http://localhost:5173
2. 使用默认账号登录（或注册新账号）

### 2. 创建简单工作流

1. 进入工作流画布
2. 拖入一个 **HTTP 请求节点**
3. 配置节点：
   - URL: `https://jsonplaceholder.typicode.com/posts/1`
   - Method: `GET`
4. 点击"保存草稿"

### 3. 发布工作流

1. 点击"发布"按钮
2. 填写版本号（如 `v1.0.0`）和变更日志
3. 确认发布

### 4. 测试执行

1. 点击画布右上角的"运行"按钮
2. 观察：
   - ✅ 节点状态实时变化（pending → running → success）
   - ✅ 右侧日志面板显示执行事件
   - ✅ 没有重复的日志
   - ✅ 节点完成后显示输出数据
3. 打开浏览器控制台（F12）：
   - Network → 找到 SSE 连接
   - 确认事件流格式正确
   - 确认没有 JSON 解析错误

### 5. 测试停止功能

1. 创建一个包含 **延迟节点** 的工作流（延迟 30 秒）
2. 点击"运行"
3. 立即点击"停止"
4. 观察：
   - ✅ 后端执行被取消
   - ✅ 最终状态显示为 `cancelled`
   - ✅ 不会被覆盖为 `failed` 或 `success`

### 6. 测试外部触发器

**Webhook 触发**：

```powershell
# 获取 webhook URL（在工作流设置中）
$webhookUrl = "http://localhost:8080/workflow/api/webhook/{token}"

# 触发执行
Invoke-WebRequest -Uri $webhookUrl -Method POST -ContentType "application/json" -Body '{"test":"data"}'
```

观察工作流是否自动执行（不需要登录）。

### 7. 测试版本隔离

1. 发布工作流版本 `v1.0.0`
2. 编辑画布，修改节点
3. 保存草稿（不发布）
4. 点击"运行"
5. 观察：
   - ✅ 执行的是已发布的 `v1.0.0`
   - ✅ 不是未发布的草稿

## 常见问题

### Q1: 前端请求 404

**症状**：前端请求 `/workflow/api/...` 返回 404

**原因**：Vite 代理未生效或后端未启动

**解决**：
1. 检查 `vite.config.ts` 中是否有 `proxy` 配置
2. 检查后端服务是否在 8080 端口监听
3. 重启前端开发服务器

### Q2: SSE 重复推送事件

**症状**：日志中同一事件出现多次

**原因**：Redis Stream 游标未生效（已修复）

**解决**：确保使用最新代码，Redis Stream `streamRange()` 已正确实现

### Q3: 节点状态不更新

**症状**：执行后节点一直显示 pending

**原因**：SSE 事件数据格式错误（已修复）

**解决**：确保 `ExecutionStreamController` 使用 `ObjectMapper` 序列化事件

### Q4: 停止按钮无效

**症状**：点击停止后，后端仍在执行

**原因**：前端未调用 cancel 接口（已修复）

**解决**：确保 `useWorkflowRun.ts` 的 `stop()` 方法调用 `executionApi.cancel()`

### Q5: 基础设施启动失败

**症状**：Docker 容器启动报错

**解决**：
```powershell
# 清理旧容器和卷
cd D:\Code\喵流\backend\meowflow\deploy\docker
docker compose -f docker-compose.full.yml down -v

# 重新启动
docker compose -f docker-compose.full.yml up -d
```

### Q6: 微服务无法注册到 Nacos

**症状**：微服务启动后 Nacos 中看不到实例

**解决**：
1. 检查 Nacos 是否正常运行：http://localhost:8848/nacos
2. 确保 Nacos 2.x 的 gRPC 端口 9848 未被占用
3. 检查微服务配置中的 Nacos 地址

## 停止系统

### 停止所有服务

```powershell
cd D:\Code\喵流\backend\meowflow
.\stop-all.ps1
```

### 仅停止基础设施

```powershell
cd D:\Code\喵流\backend\meowflow\deploy\docker
docker compose -f docker-compose.full.yml down
```

### 完全清理（包括数据卷）

```powershell
cd D:\Code\喵流\backend\meowflow\deploy\docker
docker compose -f docker-compose.full.yml down -v
```

## 开发模式切换

### Mock 模式（前端开发）

```powershell
cd D:\Code\喵流\frontend\meowflow-ui
# 使用默认 .env.development (VITE_USE_MOCK=true)
npm run dev
```

### 真实后端模式

```powershell
cd D:\Code\喵流\frontend\meowflow-ui
$env:VITE_USE_MOCK='false'
npm run dev
```

或者创建 `.env.development.local`：

```env
VITE_USE_MOCK=false
```

## 日志位置

- **微服务日志**：`D:\Code\喵流\backend\meowflow\logs\{service-name}.log`
- **Docker 容器日志**：`docker logs meowflow-{service-name}`
- **前端控制台**：浏览器 F12 → Console

## 性能优化提示

1. **开发模式**：使用 `start-infra.ps1` + IDE 启动微服务，方便调试
2. **生产模式**：使用 `start-all.ps1` 完整启动，模拟生产环境
3. **前端热重载**：修改代码后自动刷新，无需重启
4. **后端热重载**：在 IDE 中使用 Spring Boot DevTools

## 下一步

完成端到端验证后，可以：

1. 开发更复杂的工作流（条件分支、循环、人工输入）
2. 集成外部系统（webhook、定时触发）
3. 编写自动化测试
4. 性能调优和压测

---

**文档版本**: v1.0  
**更新日期**: 2026-09-03  
**维护者**: MeowFlow Team
