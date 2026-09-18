#!/usr/bin/env pwsh
# 仅启动基础设施（快速开发模式）
# 微服务在 IDE 中手动启动

$ErrorActionPreference = "Stop"

$DOCKER_DIR = "D:\Code\喵流\backend\meowflow\deploy\docker"

Write-Host "==> 启动基础设施（开发模式）" -ForegroundColor Cyan
Set-Location $DOCKER_DIR

docker compose -f docker-compose.full.yml up -d

if ($LASTEXITCODE -eq 0) {
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
    Write-Host "下一步：在 IDE 中启动微服务" -ForegroundColor Yellow
} else {
    Write-Error "启动失败"
}
