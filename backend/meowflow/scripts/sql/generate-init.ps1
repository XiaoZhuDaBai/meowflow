#!/usr/bin/env pwsh
# 从 canonical migrations 生成 scripts/sql/init.sql

$ErrorActionPreference = 'Stop'

$backendRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$migrationDir = Join-Path $backendRoot 'meowflow-common/src/main/resources/db/migration'
$outputPath = Join-Path $PSScriptRoot 'init.sql'

$files = Get-ChildItem -LiteralPath $migrationDir -File -Filter 'V*.sql' |
    Sort-Object { [int]([regex]::Match($_.Name, '^V(\d+)').Groups[1].Value) }

$builder = [System.Text.StringBuilder]::new()
[void]$builder.AppendLine('-- =============================================================')
[void]$builder.AppendLine('-- MeowFlow canonical database initialization')
[void]$builder.AppendLine('-- 由 db/migration/V*.sql 按版本自动生成，请勿手工维护本文件。')
[void]$builder.AppendLine('-- =============================================================')
[void]$builder.AppendLine('')
foreach ($file in $files) {
    [void]$builder.AppendLine("-- ===== $($file.Name) =====")
    [void]$builder.AppendLine((Get-Content -Raw -LiteralPath $file.FullName))
    [void]$builder.AppendLine('')
}
Set-Content -LiteralPath $outputPath -Value $builder.ToString() -Encoding utf8
Write-Host "Generated $outputPath from $($files.Count) migrations"
