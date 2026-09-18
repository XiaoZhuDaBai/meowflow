# MeowFlow HTTP API Test Suite
# Run: powershell -ExecutionPolicy Bypass -File run-api-tests.ps1

param(
    [string]$UserService = "http://127.0.0.1:8081",
    [string]$GatewayService = "http://127.0.0.1:8080",
    [string]$Username = "admin",
    [string]$Password = "meow@2026"
)

$ErrorActionPreference = "Continue"
$script:Token = $null
$script:Results = @()

function Write-TestResult {
    param(
        [string]$Module,
        [string]$TestName,
        [bool]$Passed,
        [string]$Message = ""
    )
    
    $result = [PSCustomObject]@{
        Module = $Module
        Test = $TestName
        Status = if ($Passed) { "PASS" } else { "FAIL" }
        Message = $Message
        Time = Get-Date -Format "HH:mm:ss"
    }
    $script:Results += $result
    
    $icon = if ($Passed) { "[PASS]" } else { "[FAIL]" }
    $color = if ($Passed) { "Green" } else { "Red" }
    Write-Host "$icon [$Module] $TestName" -ForegroundColor $color
    if ($Message) { Write-Host "      $Message" -ForegroundColor Gray }
}

function Invoke-MfApi {
    param(
        [string]$Uri,
        [string]$Method = "GET",
        [string]$Body = $null
    )
    
    $headers = @{
        "Authorization" = "Bearer $script:Token"
        "Content-Type" = "application/json"
    }
    
    try {
        if ($Body) {
            $Body | Out-File -Encoding UTF8 -FilePath "$env:TEMP\mf_body.json"
            $resp = Invoke-RestMethod -Uri $Uri -Method $Method -Headers $headers -Body "$env:TEMP\mf_body.json" -TimeoutSec 15
            Remove-Item "$env:TEMP\mf_body.json" -Force -ErrorAction SilentlyContinue
        }
        else {
            $resp = Invoke-RestMethod -Uri $Uri -Method $Method -Headers $headers -TimeoutSec 15
        }
        return $resp
    }
    catch {
        throw $_.Exception.Message
    }
}

# Header
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  MeowFlow API Test Suite" -ForegroundColor Cyan
Write-Host "  Time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. Login
Write-Host "[1/10] Auth..." -ForegroundColor Yellow
try {
    $body = @{ username = $Username; password = $Password } | ConvertTo-Json
    $body | Out-File -Encoding UTF8 -FilePath "$env:TEMP\mf_login.json"
    $resp = Invoke-RestMethod -Uri "$UserService/api/v1/auth/login" -Method Post -ContentType "application/json" -Body "$env:TEMP\mf_login.json" -TimeoutSec 10
    Remove-Item "$env:TEMP\mf_login.json" -Force -ErrorAction SilentlyContinue
    if ($resp.code -eq 200 -and $resp.data.accessToken) {
        $script:Token = $resp.data.accessToken
        Write-TestResult -Module "Auth" -TestName "Login" -Passed $true -Message "Token received"
    }
    else {
        Write-TestResult -Module "Auth" -TestName "Login" -Passed $false -Message $resp.message
    }
}
catch {
    Remove-Item "$env:TEMP\mf_login.json" -Force -ErrorAction SilentlyContinue
    Write-TestResult -Module "Auth" -TestName "Login" -Passed $false -Message $_.Exception.Message
}

# 2. Current User
Write-Host "[2/10] User Info..." -ForegroundColor Yellow
try {
    $resp = Invoke-MfApi -Uri "$UserService/api/v1/auth/me"
    if ($resp.code -eq 200) {
        $nickname = if ($resp.data.nickName) { $resp.data.nickName } else { $resp.data.nickname }
        Write-TestResult -Module "User" -TestName "Get Current User" -Passed $true -Message "User: $nickname"
    }
    else {
        Write-TestResult -Module "User" -TestName "Get Current User" -Passed $false
    }
}
catch {
    Write-TestResult -Module "User" -TestName "Get Current User" -Passed $false -Message $_
}

# 3. Permissions
Write-Host "[3/10] Permissions..." -ForegroundColor Yellow
try {
    $resp = Invoke-MfApi -Uri "$UserService/api/v1/auth/permissions"
    if ($resp.code -eq 200 -and $resp.data) {
        $permCount = $resp.data.Count
        Write-TestResult -Module "Auth" -TestName "Get Permissions" -Passed $true -Message "Count: $permCount"
    }
    else {
        Write-TestResult -Module "Auth" -TestName "Get Permissions" -Passed $false
    }
}
catch {
    Write-TestResult -Module "Auth" -TestName "Get Permissions" -Passed $false -Message $_
}

# 4. Workflow List
Write-Host "[4/10] Workflow..." -ForegroundColor Yellow
try {
    $resp = Invoke-MfApi -Uri "$GatewayService/workflow/api/v1/workflows"
    if ($resp.code -eq 200) {
        Write-TestResult -Module "Workflow" -TestName "List Workflows" -Passed $true
    }
    else {
        Write-TestResult -Module "Workflow" -TestName "List Workflows" -Passed $false
    }
}
catch {
    Write-TestResult -Module "Workflow" -TestName "List Workflows" -Passed $false -Message $_
}

# 5. Workflow Category
try {
    $resp = Invoke-MfApi -Uri "$GatewayService/workflow/api/v1/categories"
    if ($resp.code -eq 200 -and $resp.data) {
        $catCount = @($resp.data).Count
        Write-TestResult -Module "Workflow" -TestName "List Categories" -Passed $true -Message "Count: $catCount"
    }
    else {
        Write-TestResult -Module "Workflow" -TestName "List Categories" -Passed $false
    }
}
catch {
    Write-TestResult -Module "Workflow" -TestName "List Categories" -Passed $false -Message $_
}

# 6. Template
Write-Host "[5/10] Template..." -ForegroundColor Yellow
try {
    $resp = Invoke-MfApi -Uri "$GatewayService/template/api/v1/templates"
    if ($resp.code -eq 200) {
        $tplCount = if ($resp.data.records) { @($resp.data.records).Count } else { 0 }
        Write-TestResult -Module "Template" -TestName "List Templates" -Passed $true -Message "Count: $tplCount"
    }
    else {
        Write-TestResult -Module "Template" -TestName "List Templates" -Passed $false
    }
}
catch {
    Write-TestResult -Module "Template" -TestName "List Templates" -Passed $false -Message $_
}

# 7. Template Tags
try {
    $resp = Invoke-MfApi -Uri "$GatewayService/template/api/v1/tags"
    if ($resp.code -eq 200 -and $resp.data) {
        $tagCount = @($resp.data).Count
        Write-TestResult -Module "Template" -TestName "List Tags" -Passed $true -Message "Count: $tagCount"
    }
    else {
        Write-TestResult -Module "Template" -TestName "List Tags" -Passed $false
    }
}
catch {
    Write-TestResult -Module "Template" -TestName "List Tags" -Passed $false -Message $_
}

# 8. Alert
Write-Host "[6/10] Alert..." -ForegroundColor Yellow
try {
    $resp = Invoke-MfApi -Uri "$GatewayService/monitor/api/v1/alerts/rules"
    if ($resp.code -eq 200) {
        Write-TestResult -Module "Alert" -TestName "List Alert Rules" -Passed $true
    }
    else {
        Write-TestResult -Module "Alert" -TestName "List Alert Rules" -Passed $false
    }
}
catch {
    Write-TestResult -Module "Alert" -TestName "List Alert Rules" -Passed $false -Message $_
}

# 9. AI Model
Write-Host "[7/10] AI Model..." -ForegroundColor Yellow
try {
    $resp = Invoke-MfApi -Uri "$GatewayService/infra/api/v1/models"
    if ($resp.code -eq 200 -and $resp.data) {
        $modelCount = @($resp.data).Count
        Write-TestResult -Module "AI" -TestName "List Models" -Passed $true -Message "Count: $modelCount"
    }
    else {
        Write-TestResult -Module "AI" -TestName "List Models" -Passed $false
    }
}
catch {
    Write-TestResult -Module "AI" -TestName "List Models" -Passed $false -Message $_
}

# 10. Knowledge Base
Write-Host "[8/10] Knowledge Base..." -ForegroundColor Yellow
try {
    $resp = Invoke-MfApi -Uri "$GatewayService/infra/api/v1/knowledge"
    if ($resp.code -eq 200) {
        $kbCount = if ($resp.data) { @($resp.data).Count } else { 0 }
        Write-TestResult -Module "KB" -TestName "List Knowledge Bases" -Passed $true -Message "Count: $kbCount"
    }
    else {
        Write-TestResult -Module "KB" -TestName "List Knowledge Bases" -Passed $false
    }
}
catch {
    Write-TestResult -Module "KB" -TestName "List Knowledge Bases" -Passed $false -Message $_
}

# 11. Integration
Write-Host "[9/10] Integration..." -ForegroundColor Yellow
try {
    $resp = Invoke-MfApi -Uri "$GatewayService/infra/api/v1/integrations"
    if ($resp.code -eq 200) {
        Write-TestResult -Module "Integration" -TestName "List Integrations" -Passed $true
    }
    else {
        Write-TestResult -Module "Integration" -TestName "List Integrations" -Passed $false
    }
}
catch {
    Write-TestResult -Module "Integration" -TestName "List Integrations" -Passed $false -Message $_
}

# 12. Executor Health
Write-Host "[10/10] Health Check..." -ForegroundColor Yellow
try {
    $resp = Invoke-MfApi -Uri "$GatewayService/executor/api/v1/health"
    if ($resp.code -eq 200 -and $resp.data.status -eq "UP") {
        Write-TestResult -Module "Health" -TestName "Executor Health" -Passed $true -Message "Status: UP"
    }
    elseif ($resp.data.status -eq "UP") {
        Write-TestResult -Module "Health" -TestName "Executor Health" -Passed $true -Message "Status: UP"
    }
    else {
        Write-TestResult -Module "Health" -TestName "Executor Health" -Passed $false -Message "Status: $($resp.data.status)"
    }
}
catch {
    Write-TestResult -Module "Health" -TestName "Executor Health" -Passed $false -Message $_
}

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Test Complete" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$passCount = @($script:Results | Where-Object { $_.Status -eq "PASS" }).Count
$failCount = @($script:Results | Where-Object { $_.Status -eq "FAIL" }).Count
$totalCount = $script:Results.Count

$summaryColor = if ($failCount -eq 0) { "Green" } else { "Yellow" }
Write-Host "Total: $totalCount | Passed: $passCount | Failed: $failCount" -ForegroundColor $summaryColor
Write-Host ""

# By Module
$byModule = $script:Results | Group-Object Module | ForEach-Object {
    $passed = @($_.Group | Where-Object { $_.Status -eq "PASS" }).Count
    $total = $_.Group.Count
    [PSCustomObject]@{
        Module = $_.Name
        Passed = $passed
        Total = $total
        Rate = "$([math]::Round($passed / $total * 100))%"
    }
}

Write-Host "By Module:" -ForegroundColor Cyan
$byModule | ForEach-Object {
    $color = if ($_.Passed -eq $_.Total) { "Green" } else { "Yellow" }
    Write-Host "  $($_.Module): $($_.Passed)/$($_.Total) ($($_.Rate))" -ForegroundColor $color
}

# Failed Tests
$failed = $script:Results | Where-Object { $_.Status -eq "FAIL" }
if ($failed.Count -gt 0) {
    Write-Host ""
    Write-Host "Failed Tests:" -ForegroundColor Red
    $failed | ForEach-Object {
        Write-Host "  [FAIL] [$($_.Module)] $($_.Test)" -ForegroundColor Red
        if ($_.Message) { Write-Host "         $($_.Message)" -ForegroundColor DarkRed }
    }
}

Write-Host ""
