#!/usr/bin/env pwsh
# MeowFlow 停止脚本
# 用途：停止所有运行中的 MeowFlow 服务

$ErrorActionPreference = "Stop"

# 路径自动探测（原实现写死了 D:\Code\喵流）
$BACKEND_ROOT = $(if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path })
$DOCKER_DIR = Join-Path $BACKEND_ROOT "deploy\docker"

# 同 start-all.ps1：Windows PowerShell 5.1 在 $ErrorActionPreference = "Stop" 下会把
# docker 写到 stderr 的进度输出升级为终止性错误，仅用 `2>&1` 压不住。
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

Write-Host "==> 停止 MeowFlow 所有服务" -ForegroundColor Cyan
Write-Host ""

# 停止 Java 进程
Write-Host "[1/2] 停止微服务..." -ForegroundColor Yellow
# 注意：Get-Process 返回的 Process 对象没有 CommandLine 属性，
# 原实现用 $_.CommandLine -like 过滤，条件恒为 false —— 实际一个服务都停不掉。
# 这里改用 Get-CimInstance 读取命令行。
$javaProcesses = Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -and $_.CommandLine -like "*meowflow*" }

if ($javaProcesses) {
    foreach ($proc in $javaProcesses) {
        Write-Host "  停止进程 $($proc.ProcessId)..." -ForegroundColor Gray
        Stop-Process -Id $proc.ProcessId -Force -ErrorAction SilentlyContinue
    }
    Write-Host "  微服务已停止" -ForegroundColor Green
} else {
    Write-Host "  未找到运行中的微服务" -ForegroundColor Gray
}

# 停止 Docker 容器
Write-Host ""
Write-Host "[2/2] 停止基础设施容器..." -ForegroundColor Yellow
Set-Location $DOCKER_DIR

$composeFile = if (Test-Path "docker-compose.full.yml") { "docker-compose.full.yml" } else { "docker-compose.dev.yml" }

# docker compose 会把 "Container xxx Stopping" 之类进度写到 stderr，
# 用 Invoke-Docker 包一层，避免被 $ErrorActionPreference = "Stop" 当成致命错误。
$dockerOutput = Invoke-Docker @("compose", "-f", $composeFile, "down")
$dockerOutput | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }

if ($script:LastDockerExit -ne 0) {
    Write-Warning "docker compose down 返回非 0（退出码 $script:LastDockerExit）；若容器实际已停止可忽略。"
    Write-Host "  手动确认：docker compose -f $composeFile ps" -ForegroundColor DarkGray
} else {
    Write-Host "  基础设施容器已停止" -ForegroundColor Green
}

Write-Host ""
Write-Host "==> 所有服务已停止" -ForegroundColor Green
