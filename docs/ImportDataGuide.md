# 数据导入指南

## 📋 功能说明

**本工具的作用**：将 `Sample_SO_data` 目录中已有的 JSON 文件导入到 PostgreSQL 数据库中。

### ✅ 会做什么
- 读取本地 JSON 文件（`thread_01.json`, `thread_02.json` 等）
- 解析 JSON 数据
- 将数据写入 PostgreSQL 数据库表

### ❌ 不会做什么
- **不会**从 Stack Overflow API 拉取数据
- **不会**创建新的 JSON 文件
- **不会**修改现有的 JSON 文件

## 🔄 数据流程

```
Sample_SO_data/
  ├── thread_01.json  ──┐
  ├── thread_02.json  ──┤
  ├── thread_03.json  ──┼──> DataImportService ──> PostgreSQL 数据库
  └── ...              ──┘                        (stackoverflow_java)
```

## 📊 导入的数据

从 JSON 文件导入以下数据到数据库：

1. **用户信息** (`users` 表)
   - 从问题的 `owner`、回答的 `owner`、评论的 `owner` 提取

2. **问题** (`questions` 表)
   - 问题 ID、标题、标签、得分、创建时间等

3. **回答** (`answers` 表)
   - 回答 ID、所属问题、得分、是否采纳等

4. **评论** (`comments` 表)
   - 问题评论和回答评论

5. **标签** (`tags` 表)
   - 所有问题使用的标签

## 🚀 使用方法

### 方式一：使用导入脚本（推荐）

```cmd
# Windows
import-data.bat Sample_SO_data

# Linux/Mac
./import-data.sh Sample_SO_data
```

### 方式二：使用 Maven

```bash
# 设置环境变量
set IMPORT_DIRECTORY=Sample_SO_data

# 运行导入
mvnw.cmd exec:java -Dexec.mainClass="cs209a.finalproject_demo.importer.DataImporterApplication"
```

### 方式三：使用 JAR 包

```bash
# 构建
mvnw.cmd clean package

# 运行
java -jar target/FinalProject_demo-0.0.1-SNAPSHOT.jar --import.directory=Sample_SO_data
```

## 📝 导入过程

导入工具会：

1. **扫描目录**：查找所有 `.json` 文件
2. **解析文件**：使用 `ThreadFileLoader` 解析 JSON
3. **导入数据**：
   - 创建或获取用户记录
   - 创建或获取标签记录
   - 创建问题记录
   - 创建回答记录
   - 创建评论记录
4. **统计结果**：显示成功、失败、跳过的数量

## ⚠️ 注意事项

1. **幂等性**：重复导入不会创建重复数据
   - 如果问题已存在，会跳过
   - 如果用户已存在，会复用现有记录

2. **事务处理**：每个线程的导入在一个事务中
   - 如果某个线程导入失败，不会影响其他线程

3. **数据完整性**：外键约束确保数据完整性
   - 用户必须先存在才能创建问题/回答/评论

## 🔍 验证导入结果

导入完成后，使用 SQL 验证：

```sql
-- 连接到数据库
psql -U postgres -d stackoverflow_java

-- 查看统计
SELECT COUNT(*) as questions FROM questions;
SELECT COUNT(*) as answers FROM answers;
SELECT COUNT(*) as comments FROM comments;
SELECT COUNT(*) as users FROM users;
SELECT COUNT(*) as tags FROM tags;

-- 查看示例数据
SELECT question_id, title, score, answer_count 
FROM questions 
ORDER BY creation_date DESC 
LIMIT 10;
```

## 🆘 常见问题

### Q: 导入后数据不完整？

**A**: 
- 检查 JSON 文件格式是否正确
- 查看日志中的错误信息
- 确认数据库连接正常

### Q: 如何重新导入？

**A**: 
- 重复导入是幂等的，不会创建重复数据
- 如果要完全重新导入，需要先清空数据库表

### Q: 导入速度慢？

**A**: 
- 这是正常的，每个线程需要多次数据库操作
- 1000 个线程大约需要 5-10 分钟

---

**总结**：这是一个**本地数据导入工具**，将已有的 JSON 文件导入到 PostgreSQL 数据库，不会从 API 拉取新数据。




```
mvn "-Dflyway.url=jdbc:postgresql://localhost:5432/stackoverflow_java" "-Dflyway.user=postgres" "-Dflyway.password=123456" flyway:repair
```


