# 验证数据导入结果
# 需要先安装 psql 或使用其他PostgreSQL客户端工具

$dbHost = "localhost"
$dbPort = "5432"
$dbName = "stackoverflow_java"
$dbUser = "postgres"
$dbPassword = "123456"

Write-Host "========================================"
Write-Host "验证数据导入结果"
Write-Host "========================================"

# 使用 psql 连接数据库并查询统计信息
$env:PGPASSWORD = $dbPassword

$query = @"
SELECT 
    'Questions' as table_name, COUNT(*) as count FROM questions
UNION ALL
SELECT 
    'Answers' as table_name, COUNT(*) as count FROM answers
UNION ALL
SELECT 
    'Question Comments' as table_name, COUNT(*) as count FROM question_comments
UNION ALL
SELECT 
    'Answer Comments' as table_name, COUNT(*) as count FROM answer_comments
UNION ALL
SELECT 
    'Users' as table_name, COUNT(*) as count FROM users
UNION ALL
SELECT 
    'Tags' as table_name, COUNT(*) as count FROM tags
ORDER BY table_name;
"@

Write-Host "查询数据库统计信息..."
Write-Host ""

try {
    $result = & psql -h $dbHost -p $dbPort -U $dbUser -d $dbName -t -A -F "|" -c $query 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host $result
        Write-Host ""
        Write-Host "========================================"
        Write-Host "验证完成！"
        Write-Host "========================================"
    } else {
        Write-Host "错误: 无法连接到数据库"
        Write-Host $result
        Write-Host ""
        Write-Host "请确保:"
        Write-Host "1. PostgreSQL 服务正在运行"
        Write-Host "2. 数据库 'stackoverflow_java' 已创建"
        Write-Host "3. psql 命令可用（或使用其他PostgreSQL客户端）"
    }
} catch {
    Write-Host "错误: $_"
    Write-Host ""
    Write-Host "如果 psql 不可用，请手动连接到数据库并运行以下SQL:"
    Write-Host $query
}

