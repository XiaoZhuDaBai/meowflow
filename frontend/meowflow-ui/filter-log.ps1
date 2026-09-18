$content = Get-Content 'D:\Code\猪小能\frontend\meowflow-ui\build_log.txt' -Encoding UTF8
$matches = $content | Select-String -Pattern 'rewrite-ep' | Where-Object { $_.Line -match 'element-plus.: true' }
Write-Host "Total rewrite-ep lines: $($content | Select-String -Pattern 'rewrite-ep' | Measure-Object).Count"
Write-Host "Lines with element-plus=true: $($matches.Count)"
Write-Host '---'
$matches | ForEach-Object { Write-Host $_.Line }