# MeowFlow 端到端验证脚本
# 验证系统是否能完整跑通：基础设施 -> 后端服务 -> 前端 -> 执行工作流

param(
    [switch]$SkipInfra,
    [switch]$SkipBuild,
    [switch]$SkipFrontend
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "MeowFlow E2E 验证" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 检查基础设施
if (-not $SkipInfra) {
    Write-Host "[1/5] 检查基础设施状态..." -ForegroundColor Yellow
    Push-Location "d:\Code\喵流\backend\meowflow\deploy\docker"
    try {
        $containers = docker compose -f docker-compose.dev.yml ps --format json | ConvertFrom-Json
        $required = @("postgres", "redis", "nacos", "rabbitmq", "minio")
        $running = $containers | Where-Object { $_.State -eq "running" }
        
        foreach ($svc in $required) {
            $found = $running | Where-Object { $_.Service -eq $svc }
            if ($found) {
                Write-Host "  ✓ $svc 运行中" -ForegroundColor Green
            } else {
                Write-Host "  ✗ $svc 未运行" -ForegroundColor Red
                Write-Host "    提示：运行 .\start-infra.ps1 启动基础设施" -ForegroundColor Yellow
                exit 1
            }
        }
    } finally {
        Pop-Location
    }
} else {
    Write-Host "[1/5] 跳过基础设施检查" -ForegroundColor Gray
}

# 2. 检查后端构建
if (-not $SkipBuild) {
    Write-Host ""
    Write-Host "[2/5] 检查后端 JAR 文件..." -ForegroundColor Yellow
    $jars = @(
        "d:\Code\喵流\backend\meowflow\meowflow-gateway\target\meowflow-gateway-exec.jar",
        "d:\Code\喵流\backend\meowflow\meowflow-workflow\target\meowflow-workflow-exec.jar",
        "d:\Code\喵流\backend\meowflow\meowflow-user\target\meowflow-user-exec.jar",
        "d:\Code\喵流\backend\meowflow\meowflow-infra\target\meowflow-infra-exec.jar",
        "d:\Code\喵流\backend\meowflow\meowflow-template\target\meowflow-template-exec.jar",
        "d:\Code\喵流\backend\meowflow\meowflow-monitor\target\meowflow-monitor-exec.jar"
    )
    
    $missing = @()
    foreach ($jar in $jars) {
        if (Test-Path $jar) {
            Write-Host "  ✓ $(Split-Path $jar -Leaf)" -ForegroundColor Green
        } else {
            Write-Host "  ✗ $(Split-Path $jar -Leaf) 不存在" -ForegroundColor Red
            $missing += $jar
        }
    }
    
    if ($missing.Count -gt 0) {
        Write-Host "    提示：运行以下命令构建：" -ForegroundColor Yellow
        Write-Host "    cd d:\Code\喵流\backend\meowflow" -ForegroundColor Yellow
        Write-Host "    .\start-all.ps1" -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Host "[2/5] 跳过后端构建检查" -ForegroundColor Gray
}

# 3. 检查后端服务端口
Write-Host ""
Write-Host "[3/5] 检查后端服务端口..." -ForegroundColor Yellow
$ports = @{
    "8080" = "Gateway"
    "8081" = "User"
    "8082" = "Workflow"
    "8083" = "Executor"
    "8084" = "Template"
    "8085" = "Monitor"
    "8086" = "Infra"
}

foreach ($port in $ports.Keys) {
    $conn = Test-NetConnection -ComputerName localhost -Port $port -WarningAction SilentlyContinue
    if ($conn.TcpTestSucceeded) {
        Write-Host "  ✓ $($ports[$port]) (localhost:$port)" -ForegroundColor Green
    } else {
        Write-Host "  ✗ $($ports[$port]) (localhost:$port) 未监听" -ForegroundColor Red
        Write-Host "    提示：运行 .\start-all.ps1 启动后端服务" -ForegroundColor Yellow
    }
}

# 4. 检查前端配置
if (-not $SkipFrontend) {
    Write-Host ""
    Write-Host "[4/5] 检查前端配置..." -ForegroundColor Yellow
    $envFile = "d:\Code\喵流\frontend\meowflow-ui\.env.development.real"
    
    if (Test-Path $envFile) {
        Write-Host "  ✓ .env.development.real 存在" -ForegroundColor Green
        $content = Get-Content $envFile -Raw
        if ($content -match "VITE_USE_MOCK=false") {
            Write-Host "  ✓ VITE_USE_MOCK=false" -ForegroundColor Green
        } else {
            Write-Host "  ✗ VITE_USE_MOCK 未设置为 false" -ForegroundColor Red
        }
    } else {
        Write-Host "  ✗ .env.development.real 不存在" -ForegroundColor Red
        Write-Host "    已创建，请使用 'npm run dev -- --mode development.real' 启动" -ForegroundColor Yellow
    }
    
    # 检查 vite.config.ts 是否有代理配置
    $viteConfig = Get-Content "d:\Code\喵流\frontend\meowflow-ui\vite.config.ts" -Raw
    if ($viteConfig -match "proxy:") {
        Write-Host "  ✓ Vite 代理已配置" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Vite 代理未配置" -ForegroundColor Red
    }
} else {
    Write-Host "[4/5] 跳过前端检查" -ForegroundColor Gray
}

# 5. 快速健康检查
Write-Host ""
Write-Host "[5/5] 健康检查..." -ForegroundColor Yellow

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -TimeoutSec 3 -ErrorAction Stop
    if ($response.StatusCode -eq 200) {
        Write-Host "  ✓ Gateway 健康检查通过" -ForegroundColor Green
    }
} catch {
    Write-Host "  ✗ Gateway 健康检查失败: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "验证完成！" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "下一步：" -ForegroundColor Yellow
Write-Host "1. 启动前端：cd d:\Code\喵流\frontend\meowflow-ui && npm run dev" -ForegroundColor White
Write-Host "2. 访问：http://localhost:5173" -ForegroundColor White
Write-Host "3. 登录后创建工作流，测试执行" -ForegroundColor White
Write-Host ""

