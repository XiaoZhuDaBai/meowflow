#!/usr/bin/env pwsh
# MeowFlow 完整启动脚本
# 用途：按依赖顺序启动基础设施和所有微服务

param(
    [switch]$SkipBuild,
    [switch]$InfraOnly
)

$ErrorActionPreference = "Stop"

$BACKEND_ROOT = "D:\Code\喵流\backend\meowflow"
$DOCKER_DIR = "$BACKEND_ROOT\deploy\docker"
$JAVA_HOME = "C:\Users\11057\.jdks\ms-17.0.19"
$MVN = "D:\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"

Write-Host "==> MeowFlow 启动脚本" -ForegroundColor Cyan
Write-Host ""

# ============================================================================
# 1. 启动基础设施（Docker Compose）
# ============================================================================
Write-Host "[1/3] 启动基础设施容器..." -ForegroundColor Yellow
Set-Location $DOCKER_DIR

if (Test-Path "docker-compose.full.yml") {
    docker compose -f docker-compose.full.yml up -d
} else {
    docker compose -f docker-compose.dev.yml up -d
}

if ($LASTEXITCODE -ne 0) {
    Write-Error "基础设施启动失败"
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
    
    & $MVN clean package `
        -pl meowflow-common,meowflow-gateway,meowflow-user,meowflow-workflow,meowflow-template,meowflow-infra,meowflow-monitor,meowflow-executor `
        -am `
        -Dmaven.test.skip=true
    
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
            "-jar",
            "$($jarPath.FullName)"
        ) `
        -WindowStyle Hidden `
        -PassThru `
        -RedirectStandardOutput $logFile `
        -RedirectStandardError "$logDir\$($svc.Name)-error.log"
    
    $processes += @{Name=$svc.Name; Process=$proc; Port=$svc.Port}
    Start-Sleep -Seconds 3
}

# ============================================================================
# 完成
# ============================================================================
Write-Host ""
Write-Host "==> 所有服务已启动" -ForegroundColor Green
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
    $status = if ($p.Process.HasExited) { "Exited" } else { "Running" }
    Write-Host "  - $($p.Name.PadRight(10)) http://localhost:$($p.Port)  [$status]"
}
Write-Host ""
Write-Host "网关入口: http://localhost:8080" -ForegroundColor Green
Write-Host ""
Write-Host "日志目录: $BACKEND_ROOT\logs" -ForegroundColor Gray
Write-Host ""
Write-Host "使用 Ctrl+C 停止所有服务，或运行 stop-all.ps1" -ForegroundColor Yellow

# 保持脚本运行，监听 Ctrl+C
$stopRequested = $false
[Console]::TreatControlCAsInput = $true
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
    [Console]::TreatControlCAsInput = $false
    Write-Host ""
    Write-Host "正在停止所有服务..." -ForegroundColor Yellow
    foreach ($p in $processes) {
        if (-not $p.Process.HasExited) {
            Stop-Process -Id $p.Process.Id -Force -ErrorAction SilentlyContinue
        }
    }
    Set-Location $DOCKER_DIR
    docker compose -f docker-compose.full.yml down
    Write-Host "所有服务已停止" -ForegroundColor Green
}

