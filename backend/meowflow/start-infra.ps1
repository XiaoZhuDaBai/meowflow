#!/usr/bin/env pwsh
# 仅启动基础设施（快速开发模式）
# 微服务在 IDE 中手动启动

$ErrorActionPreference = "Stop"

# 路径基于脚本自身位置推导，不写死到某台机器
$BACKEND_ROOT = $(if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path })
$DOCKER_DIR = Join-Path $BACKEND_ROOT "deploy\docker"

# Windows PowerShell 5.1 在 $ErrorActionPreference = "Stop" 下会把 docker 写到 stderr 的
# 进度/警告输出（例如 "Found orphan containers ..."）升级为终止性错误，仅用 `2>&1` 压不住。
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

Write-Host "==> 启动基础设施（开发模式）" -ForegroundColor Cyan
Set-Location $DOCKER_DIR

$composeFile = if (Test-Path "docker-compose.full.yml") { "docker-compose.full.yml" } else { "docker-compose.dev.yml" }

$dockerOutput = Invoke-Docker @("compose", "-f", $composeFile, "up", "-d")
$dockerOutput | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }

if ($script:LastDockerExit -eq 0) {
    Write-Host ""
    Write-Host "==> 基础设施已启动" -ForegroundColor Green
    Write-Host ""
    Write-Host "服务地址:" -ForegroundColor Cyan
    Write-Host "  - Postgres: localhost:5432 (meowflow/meowflow123)"
    Write-Host "  - Redis:    localhost:6379"
    Write-Host "  - Nacos:    http://localhost:8848/nacos"
    Write-Host "  - RabbitMQ: http://localhost:15672 (guest/guest)"
    Write-Host "  - MinIO:    http://localhost:9001 (minioadmin/minioadmin)"
    Write-Host ""
    Write-Host "数据库表结构由 Flyway 在微服务启动时自动创建，无需手工建表。" -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "下一步：启动微服务（./start-all.ps1 -SkipBuild，或在 IDE 中逐个启动）" -ForegroundColor Yellow
} else {
    Write-Error "启动失败（docker compose 退出码 $script:LastDockerExit）"
    exit 1
}
