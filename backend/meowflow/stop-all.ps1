#!/usr/bin/env pwsh
# MeowFlow 停止脚本
# 用途：停止所有运行中的 MeowFlow 服务

$ErrorActionPreference = "Stop"

$DOCKER_DIR = "D:\Code\喵流\backend\meowflow\deploy\docker"

Write-Host "==> 停止 MeowFlow 所有服务" -ForegroundColor Cyan
Write-Host ""

# 停止 Java 进程
Write-Host "[1/2] 停止微服务..." -ForegroundColor Yellow
$javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
    $_.Path -and $_.CommandLine -like "*meowflow*"
}

if ($javaProcesses) {
    foreach ($proc in $javaProcesses) {
        Write-Host "  停止进程 $($proc.Id)..." -ForegroundColor Gray
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    }
    Write-Host "  微服务已停止" -ForegroundColor Green
} else {
    Write-Host "  未找到运行中的微服务" -ForegroundColor Gray
}

# 停止 Docker 容器
Write-Host ""
Write-Host "[2/2] 停止基础设施容器..." -ForegroundColor Yellow
Set-Location $DOCKER_DIR

if (Test-Path "docker-compose.full.yml") {
    docker compose -f docker-compose.full.yml down
} else {
    docker compose -f docker-compose.dev.yml down
}

Write-Host ""
Write-Host "==> 所有服务已停止" -ForegroundColor Green
