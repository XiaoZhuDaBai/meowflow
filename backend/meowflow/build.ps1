#!/usr/bin/env pwsh
# MeowFlow 后端构建脚本
# 替代原先散落在 backend/meowflow/ 下的 18 个一次性 .bat（它们把 JDK 与 IntelliJ
# 内置 Maven 的绝对路径写死，换台机器必然失败）。
#
# 用法：
#   ./build.ps1                             # 全量编译 + 跑测试
#   ./build.ps1 -SkipTests                  # 全量编译，跳过测试（仍会编译测试代码）
#   ./build.ps1 -Module meowflow-workflow   # 只构建指定模块（含其依赖）
#   ./build.ps1 -Clean                      # 先 clean
#
# 说明：根 pom 把 spring-boot-maven-plugin:repackage 以 classifier=exec 绑定在
# package 阶段，因此 package 与 install 都会产出 *-exec.jar（start-all.ps1 依赖它）。
# 这里用 install：本仓库存在模块间依赖（如 meowflow-workflow 依赖 meowflow-common），
# install 才能保证后续单独构建某个模块时能解析到最新版本。

param(
    [switch]$SkipTests,
    [switch]$Clean,
    [string[]]$Module = @()
)

$ErrorActionPreference = "Stop"

$BACKEND_ROOT = $(if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path })
. (Join-Path $BACKEND_ROOT "env.ps1")

Write-Host "==> MeowFlow 后端构建" -ForegroundColor Cyan
Write-Host "  JDK:    $JAVA_HOME" -ForegroundColor DarkGray
Write-Host "  Maven:  $MVN" -ForegroundColor DarkGray
Write-Host ""

Set-Location $BACKEND_ROOT

$mvnArgs = @()
if ($Clean) { $mvnArgs += "clean" }
$mvnArgs += "install"

if ($Module.Count -gt 0) {
    $mvnArgs += @("-pl", ($Module -join ","))
} else {
    # 显式列出模块，避免带上 deploy/ 等非 Java 目录
    $mvnArgs += @(
        "-pl",
        "meowflow-common,meowflow-gateway,meowflow-user,meowflow-workflow," +
        "meowflow-template,meowflow-infra,meowflow-monitor,meowflow-executor"
    )
}
$mvnArgs += "-am"

if ($SkipTests) {
    # 用 -DskipTests（而非 -Dmaven.test.skip=true）：只跳过执行、保留测试代码编译。
    # 部分模块用 build-helper 之类插件把 test-classes 加进主构建，完全跳过测试编译会失败。
    $mvnArgs += @("-DskipTests", "-B")
} else {
    $mvnArgs += "-B"
}

Write-Host "  mvn $($mvnArgs -join ' ')" -ForegroundColor DarkGray
Write-Host ""

& $MVN @mvnArgs
$code = $LASTEXITCODE

if ($code -ne 0) {
    Write-Host ""
    Write-Error "构建失败（退出码 $code）"
    exit $code
}

Write-Host ""
Write-Host "==> 构建成功" -ForegroundColor Green

if (-not $SkipTests) {
    Write-Host "    覆盖率报告：各模块 target/site/jacoco/index.html" -ForegroundColor Gray
}
