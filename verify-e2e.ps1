# MeowFlow 端到端部署校验
#
# 用途：在一台机器上按顺序确认「基础设施 -> 后端服务 -> 网关路由 -> 前端 -> 完整业务链路」是否可用。
#       真正的业务链路（登录/创建工作流/保存版本/发布/执行/模板模块创建）由
#       frontend/meowflow-ui/scripts/e2e-smoke.mjs 完成，本脚本负责前置条件与编排，
#       避免两处重复实现同一套断言。
#
# 用法：
#   ./verify-e2e.ps1                 # 全量校验
#   ./verify-e2e.ps1 -SkipSmoke      # 只查环境，不跑业务链路
#   ./verify-e2e.ps1 -FrontendUrl http://127.0.0.1:5173
#
# 退出码：0 = 全部通过；1 = 有硬性失败

param(
    [switch]$SkipInfra,
    [switch]$SkipJars,
    [switch]$SkipFrontend,
    [switch]$SkipSmoke,
    [string]$GatewayUrl = "http://localhost:8080",
    [string]$FrontendUrl = "http://127.0.0.1:5173",
    [string]$NacosUrl = "http://localhost:8848"
)

$ErrorActionPreference = "Stop"

# 路径基于脚本自身位置推导，不写死到某台机器
$RepoRoot = $PSScriptRoot
$BackendRoot = Join-Path $RepoRoot "backend\meowflow"
$FrontendDir = Join-Path $RepoRoot "frontend\meowflow-ui"
$DockerDir = Join-Path $BackendRoot "deploy\docker"

$script:passed = 0
$script:failed = 0

function Write-Ok($msg, $detail) {
    $script:passed++
    $line = "  [OK]   $msg"
    if ($detail) { $line += "  ($detail)" }
    Write-Host $line -ForegroundColor Green
}

function Write-Fail($msg, $detail) {
    $script:failed++
    $line = "  [FAIL] $msg"
    if ($detail) { $line += "  ($detail)" }
    Write-Host $line -ForegroundColor Red
}

function Write-Tip($msg) { Write-Host "         $msg" -ForegroundColor DarkGray }

function Write-Section($title) {
    Write-Host ""
    Write-Host "=== $title ===" -ForegroundColor Cyan
}

function Test-Url([string]$url, [int]$timeoutSec = 8) {
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec $timeoutSec
        return @{ Ok = $true; Status = $r.StatusCode }
    } catch {
        return @{ Ok = $false; Status = $_.Exception.Response.StatusCode.value__ }
    }
}

Write-Host "MeowFlow 端到端部署校验" -ForegroundColor Cyan
Write-Host "  仓库根目录: $RepoRoot" -ForegroundColor DarkGray
Write-Host "  网关:       $GatewayUrl" -ForegroundColor DarkGray

# ---------------------------------------------------------------- 1. 基础设施
if (-not $SkipInfra) {
    Write-Section "1/5 基础设施容器"
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Fail "未找到 docker 命令" "请安装 Docker Desktop"
    } else {
        Push-Location $DockerDir
        try {
            $composeFile = if (Test-Path "docker-compose.full.yml") { "docker-compose.full.yml" } else { "docker-compose.dev.yml" }
            $raw = docker compose -f $composeFile ps --format json 2>$null
            # compose v2 每行一个 JSON 对象
            $containers = @()
            foreach ($line in ($raw -split "`n")) {
                if ($line.Trim()) { $containers += ($line | ConvertFrom-Json) }
            }
            foreach ($svc in @("postgres", "redis", "nacos", "rabbitmq", "minio")) {
                $hit = $containers | Where-Object { $_.Service -eq $svc -and $_.State -eq "running" } | Select-Object -First 1
                if ($hit) { Write-Ok "$svc 运行中" } else { Write-Fail "$svc 未运行" "docker compose -f $composeFile up -d" }
            }
        } catch {
            Write-Fail "读取容器状态失败" $_.Exception.Message
        } finally {
            Pop-Location
        }
    }
} else {
    Write-Host ""
    Write-Host "1/5 跳过基础设施检查" -ForegroundColor DarkGray
}

# ---------------------------------------------------------------- 2. 后端 JAR
if (-not $SkipJars) {
    Write-Section "2/5 后端可执行 JAR"
    $jars = @(
        "meowflow-gateway", "meowflow-user", "meowflow-workflow", "meowflow-executor",
        "meowflow-template", "meowflow-monitor", "meowflow-infra"
    ) | ForEach-Object { Join-Path $BackendRoot "$_\target\$_-exec.jar" }

    $missing = @()
    foreach ($jar in $jars) {
        if (Test-Path $jar) {
            Write-Ok (Split-Path $jar -Leaf)
        } else {
            Write-Fail "$(Split-Path $jar -Leaf) 不存在"
            $missing += $jar
        }
    }
    if ($missing.Count -gt 0) {
        Write-Tip "构建命令：cd backend/meowflow; ./build.ps1 -SkipTests"
        Write-Tip "或直接启动全部服务：./start-all.ps1（会先构建）"
    }
} else {
    Write-Host ""
    Write-Host "2/5 跳过后端 JAR 检查" -ForegroundColor DarkGray
}

# ---------------------------------------------------------------- 3. 网关健康 + Nacos 注册
Write-Section "3/5 后端服务健康与注册"
$services = @(
    @{ Name = "gateway";  Port = 8080 },
    @{ Name = "user";     Port = 8081 },
    @{ Name = "workflow"; Port = 8082 },
    @{ Name = "executor"; Port = 8083 },
    @{ Name = "template"; Port = 8084 },
    @{ Name = "monitor";  Port = 8085 },
    @{ Name = "infra";    Port = 8086 }
)

foreach ($svc in $services) {
    $r = Test-Url "http://localhost:$($svc.Port)/actuator/health"
    if ($r.Ok -and $r.Status -eq 200) {
        Write-Ok "$($svc.Name) 健康" "port $($svc.Port)"
    } elseif ($r.Status) {
        Write-Fail "$($svc.Name) 未就绪" "port $($svc.Port) HTTP $($r.Status)"
    } else {
        Write-Fail "$($svc.Name) 不可达" "port $($svc.Port) 未监听"
        Write-Tip "若端口被占用但健康检查无响应，说明是残留僵尸进程："
        Write-Tip "Get-NetTCPConnection -LocalPort $($svc.Port) -State Listen | ForEach-Object { Stop-Process -Id `$_.OwningProcess -Force }"
    }
}

# 服务必须在 Nacos 注册，否则网关 lb:// 路由会返回 503
$nacosOk = (Test-Url "$NacosUrl/nacos").Ok
if (-not $nacosOk) {
    Write-Fail "Nacos 不可达" $NacosUrl
} else {
    foreach ($name in @("meowflow-gateway", "meowflow-user", "meowflow-workflow", "meowflow-executor", "meowflow-template", "meowflow-monitor", "meowflow-infra")) {
        try {
            $res = Invoke-RestMethod -Uri "$NacosUrl/nacos/v1/ns/instance/list?serviceName=$name&namespaceId=public" -TimeoutSec 5
            $healthy = @($res.hosts | Where-Object { $_.healthy })
            if ($healthy.Count -gt 0) {
                Write-Ok "$name 已注册" "$($healthy[0].ip):$($healthy[0].port)"
            } else {
                Write-Fail "$name 无健康实例" "网关访问该服务会返回 503"
                Write-Tip "服务启动时必须带 NACOS_ENABLED=true；start-all.ps1 已自动设置。"
            }
        } catch {
            Write-Fail "$name 注册状态查询失败" $_.Exception.Message
        }
    }
}

# ---------------------------------------------------------------- 4. 前端
if (-not $SkipFrontend) {
    Write-Section "4/5 前端"
    if (-not (Test-Path (Join-Path $FrontendDir "node_modules"))) {
        Write-Fail "前端依赖未安装" "cd frontend/meowflow-ui; npm install"
    } else {
        Write-Ok "node_modules 已就绪"
    }

    $devEnv = Join-Path $FrontendDir ".env.development"
    if (Test-Path $devEnv) {
        $content = Get-Content $devEnv -Raw
        if ($content -match "VITE_USE_MOCK=false") {
            Write-Ok ".env.development 未启用 mock" "VITE_USE_MOCK=false"
        } else {
            Write-Fail ".env.development 启用了 mock" "页面不会走真实后端"
        }
    } else {
        Write-Fail ".env.development 不存在"
    }

    $fe = Test-Url $FrontendUrl 5
    if ($fe.Ok) {
        Write-Ok "Vite 开发服务器在运行" $FrontendUrl
        # 通过前端代理打一次网关，验证代理链路
        $proxied = Test-Url "$FrontendUrl/user/actuator/health" 8
        if ($proxied.Ok -and $proxied.Status -eq 200) {
            Write-Ok "前端代理 -> 网关链路可用" "/user/actuator/health"
        } else {
            Write-Fail "前端代理 -> 网关链路不可用" "HTTP $($proxied.Status)"
        }
    } else {
        Write-Fail "Vite 开发服务器未运行" "cd frontend/meowflow-ui; npm run dev"
    }
} else {
    Write-Host ""
    Write-Host "4/5 跳过前端检查" -ForegroundColor DarkGray
}

# ---------------------------------------------------------------- 5. 业务链路
Write-Section "5/5 业务链路（登录 -> 创建工作流 -> 保存版本 -> 发布 -> 执行 -> 模板创建）"
if ($SkipSmoke) {
    Write-Host "  已跳过" -ForegroundColor DarkGray
} else {
    $smoke = Join-Path $FrontendDir "scripts\e2e-smoke.mjs"
    if (-not (Test-Path $smoke)) {
        Write-Fail "未找到冒烟脚本" "frontend/meowflow-ui/scripts/e2e-smoke.mjs"
    } elseif (-not (Get-Command node -ErrorAction SilentlyContinue)) {
        Write-Fail "未找到 node 命令" "需要 Node.js 18+"
    } else {
        $env:BASE_URL = $GatewayUrl
        & node $smoke
        if ($LASTEXITCODE -eq 0) {
            Write-Ok "端到端冒烟测试全部通过"
        } else {
            Write-Fail "端到端冒烟测试存在失败项" "见上方输出"
        }
        Remove-Item Env:\BASE_URL -ErrorAction SilentlyContinue
    }
}

# ---------------------------------------------------------------- 汇总
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
if ($script:failed -eq 0) {
    Write-Host "校验通过：$($script:passed) 项" -ForegroundColor Green
} else {
    Write-Host "校验结束：通过 $($script:passed) 项，失败 $($script:failed) 项" -ForegroundColor Red
}
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "常用入口：" -ForegroundColor Yellow
Write-Host "  前端页面   $FrontendUrl"
Write-Host "  网关       $GatewayUrl"
Write-Host "  Nacos      $NacosUrl/nacos  (nacos/nacos)"
Write-Host "  启动全部   cd backend/meowflow; ./start-all.ps1"
Write-Host "  停止全部   cd backend/meowflow; ./stop-all.ps1"
Write-Host ""

exit $(if ($script:failed -eq 0) { 0 } else { 1 })
