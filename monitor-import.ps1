# 监控数据导入进度
# 每30秒检查一次数据库中的数据量

$dbHost = "localhost"
$dbPort = "5432"
$dbName = "stackoverflow_java"
$dbUser = "postgres"
$dbPassword = "123456"

$maxChecks = 120  # 最多检查120次（约1小时）
$checkInterval = 30  # 每30秒检查一次

Write-Host "========================================"
Write-Host "数据导入监控"
Write-Host "========================================"
Write-Host "将每 $checkInterval 秒检查一次数据库"
Write-Host "最多监控 $($maxChecks * $checkInterval / 60) 分钟"
Write-Host "按 Ctrl+C 停止监控"
Write-Host "========================================"
Write-Host ""

$env:PGPASSWORD = $dbPassword

for ($i = 1; $i -le $maxChecks; $i++) {
    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    Write-Host "[$timestamp] 检查 #$i - 查询数据库统计..."
    
    try {
        # 查询数据统计
        $query = "SELECT 'Questions: ' || COUNT(*) FROM questions UNION ALL SELECT 'Answers: ' || COUNT(*) FROM answers UNION ALL SELECT 'Question Comments: ' || COUNT(*) FROM question_comments UNION ALL SELECT 'Answer Comments: ' || COUNT(*) FROM answer_comments;"
        
        $result = & psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -t -A -c $query 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host $result
            Write-Host ""
            
            # 检查是否导入完成（假设有3700个问题）
            $questionsCount = ($result | Select-String "Questions:" | ForEach-Object { ($_ -split ":")[1].Trim() })
            if ($questionsCount -and [int]$questionsCount -ge 3700) {
                Write-Host "========================================"
                Write-Host "导入可能已完成！检测到 $questionsCount 个问题"
                Write-Host "========================================"
                break
            }
        } else {
            Write-Host "警告: 无法查询数据库，可能数据库未准备好或导入未开始"
        }
    } catch {
        Write-Host "错误: $_"
    }
    
    if ($i -lt $maxChecks) {
        Write-Host "等待 $checkInterval 秒后再次检查..."
        Write-Host ""
        Start-Sleep -Seconds $checkInterval
    }
}

Write-Host ""
Write-Host "监控结束"

