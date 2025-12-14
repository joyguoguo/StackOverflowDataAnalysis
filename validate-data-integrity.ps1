# 数据完整性验证脚本 (PowerShell)
# 用于快速检查数据库中的数据一致性问题

param(
    [string]$dbHost = "localhost",
    [int]$dbPort = 5432,
    [string]$dbName = "stackoverflow_java",
    [string]$dbUser = "postgres",
    [string]$dbPassword = "123456",
    [switch]$Summary = $false  # 是否只显示摘要
)

$env:PGPASSWORD = $dbPassword

Write-Host "========================================"
Write-Host "数据完整性验证"
Write-Host "========================================"
Write-Host "数据库: $dbName @ $dbHost`:$dbPort"
Write-Host ""

if ($Summary) {
    Write-Host "运行摘要检查（快速模式）..."
    Write-Host ""
    
    $scriptFile = "validate-data-integrity-summary.sql"
    if (Test-Path $scriptFile) {
        & psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -f $scriptFile
    } else {
        Write-Host "错误: 找不到 $scriptFile 文件"
        exit 1
    }
} else {
    Write-Host "运行完整检查..."
    Write-Host ""
    
    $scriptFile = "validate-data-integrity.sql"
    if (Test-Path $scriptFile) {
        & psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -f $scriptFile
    } else {
        Write-Host "错误: 找不到 $scriptFile 文件"
        exit 1
    }
}

Write-Host ""
Write-Host "========================================"
Write-Host "验证完成"
Write-Host "========================================"
Write-Host ""
Write-Host "使用说明:"
Write-Host "  .\validate-data-integrity.ps1              # 完整检查"
Write-Host "  .\validate-data-integrity.ps1 -Summary     # 只显示摘要"

