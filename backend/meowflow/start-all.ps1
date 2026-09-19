#!/usr/bin/env pwsh
# MeowFlow 完整启动脚本
# 用途：按依赖顺序启动基础设施和所有微服务

param(
    [switch]$SkipBuild,
    [switch]$InfraOnly
)

$ErrorActionPreference = "Stop"

# 路径自动探测，避免硬编码到某台机器（原实现写死了 D:\Code\喵流 与某个 JDK/Maven 安装路径）
$BACKEND_ROOT = $(if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path })
$DOCKER_DIR = Join-Path $BACKEND_ROOT "deploy\docker"

# 调用 docker：docker 会把进度与警告（如 "Found orphan containers ..."）写到 stderr，
# 而 Windows PowerShell 5.1 在 $ErrorActionPreference = "Stop" 下会把**任何** stderr 输出
# 升级为终止性错误（仅靠 `2>&1` 压不住），于是脚本会在容器其实已正常启动的情况下中断。
# 这里临时把偏好改成 Continue，只以退出码判定成败。
function Invoke-Docker {
    param([string[]]$DockerArgs)
    $previous = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & docker @DockerArgs 2>&1
        $script:LastDockerExit = $LASTEXITCODE
        return $output
    } finally {
        $ErrorActionPreference = $previous
    }
}

# JDK / Maven 探测见 env.ps1（build.ps1 复用同一份逻辑）
. (Join-Path $BACKEND_ROOT "env.ps1")

Write-Host "  JDK:    $JAVA_HOME" -ForegroundColor DarkGray
Write-Host "  Maven:  $MVN" -ForegroundColor DarkGray

Write-Host "==> MeowFlow 启动脚本" -ForegroundColor Cyan
Write-Host ""

# ============================================================================
# 1. 启动基础设施（Docker Compose）
# ============================================================================
Write-Host "[1/3] 启动基础设施容器..." -ForegroundColor Yellow
Set-Location $DOCKER_DIR

$composeFile = if (Test-Path "docker-compose.full.yml") { "docker-compose.full.yml" } else { "docker-compose.dev.yml" }

# docker compose 会把进度/警告（例如 "Found orphan containers ..."）写到 stderr，
# 用 Invoke-Docker 包一层，避免被 $ErrorActionPreference = "Stop" 当成致命错误。
$dockerOutput = Invoke-Docker @("compose", "-f", $composeFile, "up", "-d")
$dockerOutput | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }

if ($script:LastDockerExit -ne 0) {
    Write-Error "基础设施启动失败（docker compose 退出码 $script:LastDockerExit）"
    exit 1
}

Write-Host "等待基础设施就绪（30 秒）..." -ForegroundColor Gray
Start-Sleep -Seconds 30

if ($InfraOnly) {
    Write-Host ""
    Write-Host "==> 基础设施已启动，跳过应用构建" -ForegroundColor Green
    Write-Host "    - Postgres: localhost:5432"
    Write-Host "    - Redis:    localhost:6379"
    Write-Host "    - Nacos:    http://localhost:8848/nacos"
    Write-Host "    - RabbitMQ: http://localhost:15672 (guest/guest)"
    Write-Host "    - MinIO:    http://localhost:9001 (minioadmin/minioadmin)"
    exit 0
}

# ============================================================================
# 2. 构建后端模块
# ============================================================================
if (-not $SkipBuild) {
    Write-Host ""
    Write-Host "[2/3] 构建后端模块..." -ForegroundColor Yellow
    Set-Location $BACKEND_ROOT
    $env:JAVA_HOME = $JAVA_HOME

    & (Join-Path $BACKEND_ROOT "build.ps1") -Clean -SkipTests

    if ($LASTEXITCODE -ne 0) {
        Write-Error "Maven 构建失败"
        exit 1
    }
} else {
    Write-Host ""
    Write-Host "[2/3] 跳过构建（使用已有 JAR 包）" -ForegroundColor Gray
}

# ============================================================================
# 3. 启动微服务（按依赖顺序）
# ============================================================================
Write-Host ""
Write-Host "[3/3] 启动微服务..." -ForegroundColor Yellow

$services = @(
    @{Name="gateway";   Port=8080; Jar="meowflow-gateway\target\meowflow-gateway-exec.jar"},
    @{Name="user";      Port=8081; Jar="meowflow-user\target\meowflow-user-exec.jar"},
    @{Name="workflow";  Port=8082; Jar="meowflow-workflow\target\meowflow-workflow-exec.jar"},
    @{Name="infra";     Port=8086; Jar="meowflow-infra\target\meowflow-infra-exec.jar"},
    @{Name="monitor";   Port=8085; Jar="meowflow-monitor\target\meowflow-monitor-exec.jar"},
    @{Name="template";  Port=8084; Jar="meowflow-template\target\meowflow-template-exec.jar"},
    @{Name="executor";  Port=8083; Jar="meowflow-executor\target\meowflow-executor-exec.jar"}
)

$processes = @()
$env:NACOS_ENABLED = "true"
$env:NACOS_CONFIG_ENABLED = "false"
$env:MEOWFLOW_USER_URI = "http://localhost:8081"
$env:MEOWFLOW_WORKFLOW_URI = "http://localhost:8082"
$env:MEOWFLOW_EXECUTOR_URI = "http://localhost:8083"
$env:MEOWFLOW_TEMPLATE_URI = "http://localhost:8084"
$env:MEOWFLOW_MONITOR_URI = "http://localhost:8085"
$env:MEOWFLOW_INFRA_URI = "http://localhost:8086"
# 知识库归档目录固定到 backend/meowflow/knowledge-files：
# 配置里默认是相对路径 ./knowledge-files，会被服务工作目录影响。
# （曾经因为启动脚本先 cd 到 deploy/docker，归档被写到了 deploy/docker/knowledge-files，
#   导致"上传的文件找不到、下载 404"。显式给绝对路径后不再依赖启动位置。）
$env:KNOWLEDGE_LOCAL_DIR = Join-Path $BACKEND_ROOT "knowledge-files"
if (-not (Test-Path $env:KNOWLEDGE_LOCAL_DIR)) {
    New-Item -ItemType Directory -Path $env:KNOWLEDGE_LOCAL_DIR -Force | Out-Null
}

foreach ($svc in $services) {
    $jarPath = Get-ChildItem -Path "$BACKEND_ROOT\$($svc.Jar)" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $jarPath) {
        Write-Warning "未找到 $($svc.Name) 的 JAR 包，跳过"
        continue
    }

    Write-Host "  启动 $($svc.Name) (端口 $($svc.Port))..." -ForegroundColor Gray

    $logFile = "$BACKEND_ROOT\logs\$($svc.Name).log"
    $logDir = Split-Path $logFile -Parent
    if (-not (Test-Path $logDir)) {
        New-Item -ItemType Directory -Path $logDir -Force | Out-Null
    }

    $proc = Start-Process -FilePath "$JAVA_HOME\bin\java.exe" `
        -ArgumentList @(
            "-Dspring.profiles.active=dev",
            "-Dserver.port=$($svc.Port)",
            # 每个进程显式限制堆：不加约束时 JVM 会按宿主机内存（如 24G）预留堆，
            # 7 个服务并发启动很容易把内存吃光，最后启动的服务会因分配失败而启动不了
            # （表现为 "Failed to start bean 'webServerStartStop'"，且异常本身还常被日志框架盖住）。
            "-Xms128m",
            "-Xmx512m",
            "-jar",
            "$($jarPath.FullName)"
        ) `
        -WorkingDirectory $BACKEND_ROOT `
        -WindowStyle Hidden `
        -PassThru `
        -RedirectStandardOutput $logFile `
        -RedirectStandardError "$logDir\$($svc.Name)-error.log"

    $processes += @{Name=$svc.Name; Process=$proc; Port=$svc.Port; Log=$logFile; ErrorLog="$logDir\$($svc.Name)-error.log"; Jar=$jarPath.FullName}
    Start-Sleep -Seconds 3
}

# ============================================================================
# 完成 —— 健康校验
# ============================================================================
Write-Host ""
Write-Host "==> 服务已拉起，正在校验健康状态..." -ForegroundColor Yellow

function Wait-ServiceHealthy($svc, [int]$Attempts = 45) {
    # 45 次 × 4 秒 ≈ 最多等 3 分钟（网关冷启动最慢）
    for ($i = 0; $i -lt $Attempts; $i++) {
        if ($svc.Process.HasExited) { return $false }
        try {
            $r = Invoke-WebRequest -Uri "http://localhost:$($svc.Port)/actuator/health" -UseBasicParsing -TimeoutSec 4
            if ($r.StatusCode -eq 200) { return $true }
        } catch { }
        Start-Sleep -Seconds 4
    }
    return $false
}

$unhealthy = @()
foreach ($p in $processes) {
    if (Wait-ServiceHealthy $p) { continue }

    # 启动失败重试一次：并发启动时可能因内存/端口竞争而抢不到资源，
    # 等其它服务稳定后再拉一次通常就能起来。
    if ($p.Process.HasExited) {
        Write-Host "  $($p.Name) 启动失败，重试一次..." -ForegroundColor Yellow
        $retry = Start-Process -FilePath "$JAVA_HOME\bin\java.exe" `
            -ArgumentList @(
                "-Dspring.profiles.active=dev",
                "-Dserver.port=$($p.Port)",
                "-Xms128m", "-Xmx512m",
                "-jar", "$($p.Jar)"
            ) `
            -WorkingDirectory $BACKEND_ROOT `
            -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput $p.Log -RedirectStandardError $p.ErrorLog
        $p.Process = $retry
        Start-Sleep -Seconds 5
        if (Wait-ServiceHealthy $p) { continue }
    }
    $unhealthy += $p
}

Write-Host ""
Write-Host "基础设施:" -ForegroundColor Cyan
Write-Host "  - Postgres: localhost:5432"
Write-Host "  - Redis:    localhost:6379"
Write-Host "  - Nacos:    http://localhost:8848/nacos"
Write-Host "  - RabbitMQ: http://localhost:15672 (guest/guest)"
Write-Host "  - MinIO:    http://localhost:9001 (minioadmin/minioadmin)"
Write-Host ""
Write-Host "微服务:" -ForegroundColor Cyan
foreach ($p in $processes) {
    $status = if ($p.Process.HasExited) { "Exited" } elseif ($unhealthy.Name -contains $p.Name) { "UNHEALTHY" } else { "Running" }
    $color = if ($status -eq "Running") { "Green" } else { "Red" }
    Write-Host ("  - {0} http://localhost:{1}  [{2}]" -f $p.Name.PadRight(10), $p.Port, $status) -ForegroundColor $color
}
Write-Host ""
Write-Host "网关入口: http://localhost:8080" -ForegroundColor Green
Write-Host "前端入口: http://127.0.0.1:5173 (需另开终端 cd frontend/meowflow-ui && npm run dev)" -ForegroundColor Green
Write-Host ""
Write-Host "日志目录: $BACKEND_ROOT\logs" -ForegroundColor Gray

if ($unhealthy.Count -gt 0) {
    Write-Host ""
    Write-Warning "以下服务未通过健康检查：$($unhealthy.Name -join ', ')"
    Write-Host "  若某端口被占用但 /actuator/health 无响应，说明该端口上是**残留的僵尸进程**。" -ForegroundColor Yellow
    Write-Host "  这会让 Nacos 摘掉实例、网关对该服务返回 503，前端相应功能不可用。" -ForegroundColor Yellow
    Write-Host "  处理：停掉占用进程后重启，例如：" -ForegroundColor Yellow
    foreach ($p in $unhealthy) {
        Write-Host "    Get-NetTCPConnection -LocalPort $($p.Port) -State Listen | ForEach-Object { Stop-Process -Id `$_.OwningProcess -Force }" -ForegroundColor Gray
    }
    Write-Host "  再校验注册情况：" -ForegroundColor Yellow
    Write-Host "    curl.exe -s `"http://localhost:8848/nacos/v1/ns/instance/list?serviceName=meowflow-<svc>&namespaceId=public`"" -ForegroundColor Gray
}
Write-Host ""
Write-Host "使用 Ctrl+C 停止所有服务，或运行 stop-all.ps1" -ForegroundColor Yellow

# 保持脚本运行，监听 Ctrl+C。
# [Console]::TreatControlCAsInput 在无控制台的场景（后台作业、重定向）会抛
# "The handle is invalid"，原实现因此让整个脚本以退出码 1 结束。这里做成可选能力。
$stopRequested = $false
$interactive = $true
try { [Console]::TreatControlCAsInput = $true } catch { $interactive = $false }

if (-not $interactive) {
    Write-Host "（当前无交互控制台，脚本到此结束；服务已在后台运行，用 stop-all.ps1 停止）" -ForegroundColor DarkGray
    exit 0
}

try {
    while (-not $stopRequested) {
        if ([Console]::KeyAvailable) {
            $key = [Console]::ReadKey($true)
            if ($key.Key -eq "C" -and $key.Modifiers -band [ConsoleModifiers]::Control) {
                $stopRequested = $true
            }
        }
        Start-Sleep -Milliseconds 250
    }
} finally {
    try { [Console]::TreatControlCAsInput = $false } catch { }
    Write-Host ""
    Write-Host "正在停止所有服务..." -ForegroundColor Yellow
    foreach ($p in $processes) {
        if (-not $p.Process.HasExited) {
            Stop-Process -Id $p.Process.Id -Force -ErrorAction SilentlyContinue
        }
    }
    Set-Location $DOCKER_DIR
    # 同 Invoke-Docker 的理由：docker 向 stderr 写进度，直接调用会中断清理流程。
    $downOutput = Invoke-Docker @("compose", "-f", $composeFile, "down")
    $downOutput | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }
    Write-Host "所有服务已停止" -ForegroundColor Green
}

