# MeowFlow 本地开发环境部署指南

## 环境要求

- Docker Desktop 4.x+
- 16GB+ RAM（推荐）
- 20GB+ 可用磁盘空间

---

## 一、首次部署（完整启动）

### 1. 进入部署目录

```powershell
cd d:\Code\喵流\backend\meowflow\deploy\docker
```

### 2. 构建并启动所有服务

```powershell
# 构建自定义 RabbitMQ 镜像（包含延时队列插件）
docker-compose -f docker-compose.dev.yml build

# 启动所有服务
docker-compose -f docker-compose.dev.yml up -d
```

> **首次启动会执行以下操作：**
> 1. 构建自定义 RabbitMQ 镜像（约1-2分钟）
> 2. 拉取 Nacos、PostgreSQL、Redis、MinIO 等基础镜像
> 3. 初始化 PostgreSQL 数据库（执行 `init-postgres.sh`）
> 4. 启动所有服务并等待健康检查

### 3. 验证服务状态

```powershell
docker-compose -f docker-compose.dev.yml ps
```

正常状态应显示：
```
NAME                STATUS          PORTS
meowflow-nacos      Up (healthy)   0.0.0.0:8848->8848/tcp, 9848/tcp, 9849/tcp
meowflow-postgres   Up (healthy)   0.0.0.0:5432->5432/tcp
meowflow-rabbitmq   Up (healthy)   0.0.0.0:5672->5672/tcp, 0.0.0.0:15672->15672/tcp
meowflow-redis      Up (healthy)   0.0.0.0:6379->6379/tcp
meowflow-minio      Up (healthy)   0.0.0.0:9000->9000/tcp, 0.0.0.0:9001->9001/tcp
meowflow-app        Up (healthy)   0.0.0.0:8080->8080/tcp
```

---

## 二、查看日志

```powershell
# 查看所有服务日志
docker-compose -f docker-compose.dev.yml logs -f

# 查看指定服务日志
docker-compose -f docker-compose.dev.yml logs -f nacos
docker-compose -f docker-compose.dev.yml logs -f rabbitmq
docker-compose -f docker-compose.dev.yml logs -f meowflow-app
```

---

## 三、停止和清理

```powershell
# 停止服务（保留数据卷）
docker-compose -f docker-compose.dev.yml stop

# 停止并删除容器（保留数据卷）
docker-compose -f docker-compose.dev.yml down

# 完全清理（包括数据卷，慎用！）
docker-compose -f docker-compose.dev.yml down -v
```

---

## 四、后续开发常用命令

### 重启单个服务

```powershell
docker-compose -f docker-compose.dev.yml restart meowflow-app
```

### 进入容器调试

```powershell
# 进入 RabbitMQ 容器
docker exec -it meowflow-rabbitmq bash

# 进入 PostgreSQL 客户端
docker exec -it meowflow-postgres psql -U meowflow -d meowflow
```

### 重置数据库

```powershell
# 1. 删除数据卷
docker volume rm meowflow_postgres_data

# 2. 重启 PostgreSQL（会自动执行 init-postgres.sh）
docker-compose -f docker-compose.dev.yml up -d postgres
```

---

## 五、访问地址汇总

| 服务 | 地址 | 账号 |
|------|------|------|
| 应用 API | http://localhost:8080 | - |
| Nacos 控制台 | http://localhost:8848/nacos/ | nacos / nacos |
| RabbitMQ 管理界面 | http://localhost:15672/ | guest / guest |
| MinIO 控制台 | http://localhost:9001/ | minioadmin / minioadmin |
| PostgreSQL | localhost:5432 | meowflow / meowflow123 |
| Redis | localhost:6379 | - |

---

## 六、数据库初始化说明

`init-postgres.sh` 在 PostgreSQL 容器首次启动时自动执行：

```sql
-- 启用 pg_trgm 扩展（模糊搜索支持）
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 尝试启用 zhparser 扩展（中文分词，如不可用会跳过）
CREATE EXTENSION IF NOT EXISTS zhparser;

-- 创建性能优化索引
CREATE INDEX idx_pg_trgm ON pg_class USING gin (relname gin_trgm_ops);
```

> **注意**：zhparser 扩展在 Alpine 基础镜像中可能不可用，如果需要中文分词搜索功能，请考虑使用 Debian 基础的 PostgreSQL 镜像。

---

## 七、常见问题

### Q: RabbitMQ 延时队列插件没有启用？

检查插件是否正确安装：
```powershell
docker exec -it meowflow-rabbitmq rabbitmq-plugins list -e
```

应该看到：
```
[e*] rabbitmq_delayed_message_exchange 3.12.0
[e*] rabbitmq_management               3.12.x
```

### Q: Nacos 启动失败？

Nacos 首次启动较慢（约30-60秒），等待健康检查通过即可。

### Q: 应用连接数据库失败？

确保 PostgreSQL 健康检查通过后再启动应用：
```powershell
docker-compose -f docker-compose.dev.yml up -d postgres
sleep 10
docker-compose -f docker-compose.dev.yml up -d
```

---

## 八、重新构建应用镜像

修改代码后需要重新构建应用镜像：

```powershell
# 重新构建所有镜像
docker-compose -f docker-compose.dev.yml build --no-cache

# 只重建应用服务
docker-compose -f docker-compose.dev.yml build meowflow-app
```
