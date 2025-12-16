# 数据完整性验证说明

## 概述

本目录包含了用于验证数据库中数据完整性的SQL脚本和PowerShell脚本。

## 验证内容

验证脚本会检查以下数据完整性问题：

### 1. 问题相关的一致性检查
- ✅ 问题标记为已回答(`answered=true`)但没有对应的答案
- ✅ 问题的`answer_count`字段与实际答案数量不一致
- ✅ 问题的`accepted_answer_id`指向不存在的答案
- ✅ 问题的`accepted_answer_id`指向的答案不属于该问题
- ✅ 问题的`accepted_answer_id`指向的答案的`accepted`字段不为`true`

### 2. 外键完整性检查
- ✅ 孤立的`question_comments`（指向不存在的问题）
- ✅ 孤立的`answer_comments`（指向不存在的答案）
- ✅ 孤立的`answers`（指向不存在的问题）
- ✅ 各种实体的`owner_account_id`指向不存在的用户

## 使用方法

### 方法1：使用PowerShell脚本（推荐）

**快速摘要检查（推荐）：**
```powershell
.\validate-data-integrity.ps1 -Summary
```

**完整详细检查：**
```powershell
.\validate-data-integrity.ps1
```

**自定义数据库连接：**
```powershell
.\validate-data-integrity.ps1 -dbHost localhost -dbPort 5432 -dbName stackoverflow_java -dbUser checker -dbPassword 123456
```

### 方法2：直接使用psql命令

**快速摘要：**
```bash
psql -h localhost -p 5432 -U postgres -d stackoverflow_java -f validate-data-integrity-summary.sql
```

**完整检查：**
```bash
psql -h localhost -p 5432 -U postgres -d stackoverflow_java -f validate-data-integrity.sql
```

### 方法3：在PostgreSQL客户端中运行

1. 打开pgAdmin、DBeaver或其他PostgreSQL客户端
2. 连接到数据库 `stackoverflow_java`
3. 打开 `validate-data-integrity-summary.sql` 或 `validate-data-integrity.sql`
4. 执行脚本

## 输出说明

### 摘要模式输出

摘要模式会显示每个检查项的问题数量，格式如下：

```
issue                                    | count
-----------------------------------------|------
标记为已回答但没有答案的问题              | 5
answer_count与实际答案数量不一致的问题    | 12
accepted_answer_id指向不存在的答案        | 0
...
```

**理想情况：所有count都为0**

### 完整模式输出

完整模式会显示每个问题的详细记录，包括：
- 问题ID
- 标题
- 相关字段值
- 实际值与记录值的差异

## 常见问题

### Q: 如果发现有数据完整性问题怎么办？

**A:** 根据问题类型采取不同措施：

1. **answer_count不一致**
   - 这可能是导入时的数据问题
   - 可以考虑重新导入该问题，或者手动更新`answer_count`字段

2. **标记为已回答但没有答案**
   - 这可能是原始数据的问题，或者导入过程中答案丢失
   - 检查原始JSON文件确认是否有答案
   - 如果有答案但未导入，重新导入该问题

3. **accepted_answer_id指向不存在的答案**
   - 这是严重的数据完整性问题
   - 需要检查导入逻辑
   - 可能需要重新导入该问题

4. **孤立的评论或回答**
   - 这些记录的外键指向不存在的主记录
   - 需要删除这些孤立记录，或修复外键引用

### Q: 如何修复发现的问题？

**A:** 修复方法取决于问题类型：

1. **修复answer_count不一致：**
```sql
UPDATE questions q
SET answer_count = (
    SELECT COUNT(*) 
    FROM answers a 
    WHERE a.question_id = q.question_id
)
WHERE q.answer_count != (
    SELECT COUNT(*) 
    FROM answers a 
    WHERE a.question_id = q.question_id
);
```

2. **删除孤立的评论：**
```sql
-- 删除孤立的question_comments
DELETE FROM question_comments qc
WHERE NOT EXISTS (
    SELECT 1 FROM questions q WHERE q.question_id = qc.question_id
);

-- 删除孤立的answer_comments
DELETE FROM answer_comments ac
WHERE NOT EXISTS (
    SELECT 1 FROM answers a WHERE a.answer_id = ac.answer_id
);
```

3. **修复accepted_answer_id：**
```sql
-- 如果accepted_answer_id指向的答案不属于该问题，清空该字段
UPDATE questions q
SET accepted_answer_id = NULL
WHERE q.accepted_answer_id IS NOT NULL
  AND EXISTS (
      SELECT 1 FROM answers a 
      WHERE a.answer_id = q.accepted_answer_id 
        AND a.question_id != q.question_id
  );
```

### Q: 验证需要多长时间？

**A:** 
- **摘要模式**：通常几秒钟
- **完整模式**：取决于数据量，3700个问题通常需要10-30秒

### Q: 验证会修改数据吗？

**A:** 不会。所有验证脚本都是只读查询，不会修改任何数据。

## 建议

1. **定期验证**：在数据导入后、系统运行前进行验证
2. **先看摘要**：使用`-Summary`模式快速了解整体情况
3. **详细分析**：如果摘要显示有问题，再使用完整模式查看详细信息
4. **记录问题**：将发现的问题记录下来，便于后续修复

## 注意事项

- 确保PostgreSQL服务正在运行
- 确保数据库连接配置正确
- 如果使用psql，确保已安装PostgreSQL客户端工具
- 大规模数据验证可能需要一些时间，请耐心等待

