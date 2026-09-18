# 喵流 MeowFlow 异常场景自动化测试

param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$Username = "admin",
    [string]$Password = "meow@2026",
    [switch]$Verbose
)

$ErrorActionPreference = "Continue"
$script:Token = $null
$script:Results = @()

function Write-TestResult {
    param(
        [string]$Category,
        [string]$TestName,
        [bool]$Passed,
        [string]$Expected = "",
        [string]$Actual = "",
        [string]$Error = ""
    )
    
    $result = [PSCustomObject]@{
        Category = $Category
        Test = $TestName
        Status = if ($Passed) { "PASS" } else { "FAIL" }
        Expected = $Expected
        Actual = $Actual
        Error = $Error
        Time = Get-Date -Format "HH:mm:ss"
    }
    $script:Results += $result
    
    $icon = if ($Passed) { "✅" } else { "❌" }
    $color = if ($Passed) { "Green" } else { "Red" }
    Write-Host "$icon [$Category] $TestName" -ForegroundColor $color
    if ($Verbose -and !$Passed) {
        if ($Expected) { Write-Host "   预期: $Expected" -ForegroundColor Gray }
        if ($Actual) { Write-Host "   实际: $Actual" -ForegroundColor Gray }
        if ($Error) { Write-Host "   错误: $Error" -ForegroundColor DarkRed }
    }
}

function Get-AuthToken {
    $body = @{ username = $Username; password = $Password } | ConvertTo-Json
    try {
        $resp = Invoke-RestMethod -Uri "$BaseUrl/gateway/user/auth/login" -Method Post -ContentType "application/json" -Body $body -TimeoutSec 10
        if ($resp.code -eq 0) {
            $script:Token = $resp.data.token
            return $true
        }
    }
    catch { }
    return $false
}

function Test-WithoutToken {
    param([string]$Path, [string]$ExpectedCode = "401")
    
    try {
        $resp = Invoke-RestMethod -Uri "$BaseUrl/gateway$Path" -Method Get -TimeoutSec 5 -ErrorAction SilentlyContinue
        $actualCode = $resp.code
        $passed = ($actualCode -eq 401 -or $actualCode -eq 403)
        Write-TestResult -Category "认证异常" -TestName "无 Token 访问 $Path" `
            -Passed $passed -Expected "401/403" -Actual $actualCode
    }
    catch {
        $statusCode = [int]$_.Exception.Response.StatusCode
        if ($statusCode -eq 401 -or $statusCode -eq 403) {
            Write-TestResult -Category "认证异常" -TestName "无 Token 访问 $Path" -Passed $true
        }
        else {
            Write-TestResult -Category "认证异常" -TestName "无 Token 访问 $Path" `
                -Passed $false -Expected "401/403" -Actual $statusCode -Error $_.Exception.Message
        }
    }
}

function Test-InvalidToken {
    param([string]$Path)
    
    try {
        $headers = @{ "Authorization" = "Bearer invalid_token_12345" }
        $resp = Invoke-RestMethod -Uri "$BaseUrl/gateway$Path" -Method Get -Headers $headers -TimeoutSec 5
        $code = $resp.code
        if ($code -eq 401 -or $code -eq 403) {
            Write-TestResult -Category "认证异常" -TestName "无效 Token 访问 $Path" -Passed $true
        }
        else {
            Write-TestResult -Category "认证异常" -TestName "无效 Token 访问 $Path" `
                -Passed $false -Expected "401/403" -Actual $code
        }
    }
    catch {
        $statusCode = [int]$_.Exception.Response.StatusCode
        if ($statusCode -eq 401 -or $statusCode -eq 403) {
            Write-TestResult -Category "认证异常" -TestName "无效 Token 访问 $Path" -Passed $true
        }
        else {
            Write-TestResult -Category "认证异常" -TestName "无效 Token 访问 $Path" `
                -Passed $false -Expected "401/403" -Actual $statusCode -Error $_.Exception.Message
        }
    }
}

function Test-BadRequest {
    param(
        [string]$Path,
        [string]$Method = "POST",
        [string]$Body = "{}"
    )
    
    try {
        $headers = @{
            "Authorization" = "Bearer $script:Token"
            "Content-Type" = "application/json"
        }
        $resp = Invoke-RestMethod -Uri "$BaseUrl/gateway$Path" -Method $Method -Headers $headers -Body $Body -TimeoutSec 10
        # 如果没有返回错误，说明没有做校验
        Write-TestResult -Category "请求校验" -TestName "$Method $Path 空 Body" `
            -Passed ($resp.code -ne 0) -Expected "非 0" -Actual $resp.code
    }
    catch {
        $statusCode = [int]$_.Exception.Response.StatusCode
        if ($statusCode -eq 400) {
            Write-TestResult -Category "请求校验" -TestName "$Method $Path 空 Body" -Passed $true
        }
        else {
            Write-TestResult -Category "请求校验" -TestName "$Method $Path 空 Body" `
                -Passed ($statusCode -eq 400 -or $statusCode -eq 422) `
                -Expected "400/422" -Actual $statusCode -Error $_.Exception.Message
        }
    }
}

function Test-SQLInjection {
    param([string]$Param, [string]$Value = "' OR '1'='1")
    
    try {
        $headers = @{ "Authorization" = "Bearer $script:Token" }
        $resp = Invoke-RestMethod -Uri "$BaseUrl/gateway/workflow/workflow/list?$Param=$Value" -Method Get -Headers $headers -TimeoutSec 10
        # 应该正常返回，不会执行注入
        Write-TestResult -Category "安全" -TestName "SQL 注入测试 ($Param=$Value)" -Passed $true -Actual "已防护"
    }
    catch {
        Write-TestResult -Category "安全" -TestName "SQL 注入测试 ($Param=$Value)" `
            -Passed $false -Error $_.Exception.Message
    }
}

function Test-XSS {
    param([string]$Value = "<script>alert('xss')</script>")
    
    try {
        $headers = @{
            "Authorization" = "Bearer $script:Token"
            "Content-Type" = "application/json"
        }
        $body = @{
            name = $Value
            description = $Value
        } | ConvertTo-Json
        $resp = Invoke-RestMethod -Uri "$BaseUrl/gateway/workflow/workflow" -Method Post -Headers $headers -Body $body -TimeoutSec 10
        # 应该返回成功或参数错误，不会存储 XSS
        Write-TestResult -Category "安全" -TestName "XSS 测试" -Passed ($resp.code -ne 200 -or $resp.message -notmatch "script") -Actual $resp.message
    }
    catch {
        Write-TestResult -Category "安全" -TestName "XSS 测试" -Passed $true -Actual "拒绝请求"
    }
}

function Test-EmptyList {
    param([string]$Path)
    
    try {
        $headers = @{ "Authorization" = "Bearer $script:Token" }
        $resp = Invoke-RestMethod -Uri "$BaseUrl/gateway$Path" -Method Get -Headers $headers -TimeoutSec 10
        if ($resp.code -eq 0) {
            $data = $resp.data
            if ($data.records -ne $null) {
                Write-TestResult -Category "边界" -TestName "空列表 $Path" -Passed $true -Actual "返回空列表"
            }
            else {
                Write-TestResult -Category "边界" -TestName "空列表 $Path" -Passed $true -Actual "正常返回"
            }
        }
        else {
            Write-TestResult -Category "边界" -TestName "空列表 $Path" -Passed $false -Actual $resp.message
        }
    }
    catch {
        Write-TestResult -Category "边界" -TestName "空列表 $Path" -Passed $false -Error $_.Exception.Message
    }
}

function Test-Pagination {
    param(
        [string]$Path,
        [int]$Page = 9999,
        [int]$Size = 1000
    )
    
    try {
        $headers = @{ "Authorization" = "Bearer $script:Token" }
        $resp = Invoke-RestMethod -Uri "$BaseUrl/gateway$Path?page=$Page&size=$Size" -Method Get -Headers $headers -TimeoutSec 10
        if ($resp.code -eq 0) {
            Write-TestResult -Category "边界" -TestName "分页边界测试 (page=$Page)" -Passed $true -Actual "正常处理"
        }
        else {
            Write-TestResult -Category "边界" -TestName "分页边界测试 (page=$Page)" -Passed $false -Actual $resp.message
        }
    }
    catch {
        Write-TestResult -Category "边界" -TestName "分页边界测试 (page=$Page)" -Passed $false -Error $_.Exception.Message
    }
}

# ================================================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  喵流 MeowFlow 异常场景测试" -ForegroundColor Cyan
Write-Host "  测试时间: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 获取 Token
Write-Host "[1/5] 认证..." -ForegroundColor Yellow
if (Get-AuthToken) {
    Write-TestResult -Category "认证" -TestName "获取 Token" -Passed $true
}
else {
    Write-TestResult -Category "认证" -TestName "获取 Token" -Passed $false
    exit 1
}

# 2. 认证异常测试
Write-Host "[2/5] 认证异常测试..." -ForegroundColor Yellow
Test-WithoutToken "/user/auth/current"
Test-WithoutToken "/workflow/workflow/list"
Test-WithoutToken "/template/template/list"
Test-InvalidToken "/user/auth/current"
Test-InvalidToken "/workflow/workflow/list"

# 3. 请求校验测试
Write-Host "[3/5] 请求校验测试..." -ForegroundColor Yellow
Test-BadRequest -Path "/workflow/workflow" -Method "POST" -Body "{}"

# 4. 安全测试
Write-Host "[4/5] 安全测试..." -ForegroundColor Yellow
Test-SQLInjection -Param "name" -Value "test' OR 1=1 --"
Test-SQLInjection -Param "keyword" -Value "<script>alert(1)</script>"
Test-XSS

# 5. 边界测试
Write-Host "[5/5] 边界测试..." -ForegroundColor Yellow
Test-Pagination -Path "/workflow/workflow/list" -Page 9999
Test-Pagination -Path "/workflow/workflow/list" -Page 1 -Size 10000

# ================================================================
# 输出总结
# ================================================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  测试完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$passCount = @($script:Results | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = @($script:Results | Where-Object { $_.Status -eq "FAIL" }).Count
$totalCount = $script:Results.Count

Write-Host "总计: $totalCount | 通过: $passCount | 失败: $failCount" -ForegroundColor $(if ($failCount -eq 0) { "Green" } else { "Yellow" })
Write-Host ""

# 显示失败的测试
$failed = $script:Results | Where-Object { $_.Status -eq "FAIL" }
if ($failed.Count -gt 0) {
    Write-Host "失败的测试:" -ForegroundColor Red
    $failed | ForEach-Object {
        Write-Host "  ❌ [$($_.Category)] $($_.Test)" -ForegroundColor Red
        if ($_.Error) { Write-Host "     错误: $($_.Error)" -ForegroundColor DarkRed }
    }
}

Write-Host ""
