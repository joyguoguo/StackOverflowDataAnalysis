# PostgreSQL 数据库设置指南

本文档说明如何设置 PostgreSQL 数据库并导入 Stack Overflow 数据。

## 📋 前置要求

1. **PostgreSQL 数据库**（版本 12 或更高）
   - 下载地址：https://www.postgresql.org/download/
   - 或使用 Docker：`docker run -d -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:15`

2. **数据库已安装并运行**

## 🚀 快速开始

### 步骤 1：创建数据库

```sql
-- 连接到 PostgreSQL
psql -U postgres

-- 创建数据库
CREATE DATABASE stackoverflow_java;

-- 退出
\q
```

### 步骤 2：配置数据库连接

编辑 `src/main/resources/application.properties`：

```properties
# PostgreSQL 数据库配置
spring.datasource.url=jdbc:postgresql://localhost:5432/stackoverflow_java
spring.datasource.username=postgres
spring.datasource.password=你的密码
spring.datasource.driver-class-name=org.postgresql.Driver
```

### 步骤 3：运行应用（自动创建表结构）

```bash
# 编译项目
mvnw.cmd clean package

# 运行应用（Flyway 会自动创建表结构）
java -jar target/FinalProject_demo-0.0.1-SNAPSHOT.jar
```

### 步骤 4：导入数据

#### 方式一：使用命令行参数

```bash
java -jar target/FinalProject_demo-0.0.1-SNAPSHOT.jar \
    --import.data=true \
    --import.directory=Sample_SO_data
```

#### 方式二：使用环境变量

```bash
# Windows
set IMPORT_DATA=true
set IMPORT_DIRECTORY=Sample_SO_data
java -jar target/FinalProject_demo-0.0.1-SNAPSHOT.jar

# Linux/Mac
export IMPORT_DATA=true
export IMPORT_DIRECTORY=Sample_SO_data
java -jar target/FinalProject_demo-0.0.1-SNAPSHOT.jar
```

#### 方式三：在代码中调用

```java
@Autowired
private DataImportService importService;

public void importData() {
    DataImportService.ImportResult result = 
        importService.importFromDirectory("Sample_SO_data");
    log.info("Imported {} threads", result.getSuccessCount());
}
```

## 📊 数据库结构

### 表结构

1. **users** - 用户信息
   - `id` (主键)
   - `account_id` (唯一)
   - `user_id`
   - `display_name`
   - `reputation`
   - `user_type`

2. **tags** - 标签
   - `id` (主键)
   - `name` (唯一)

3. **questions** - 问题
   - `question_id` (主键)
   - `title`
   - `body` (TEXT)
   - `answered`
   - `answer_count`
   - `score`
   - `creation_date`
   - `last_activity_date`
   - `accepted_answer_id`
   - `view_count`
   - `owner_account_id` (外键 -> users)

4. **question_tags** - 问题标签关联
   - `question_id` (外键 -> questions)
   - `tag_name` (外键 -> tags)

5. **answers** - 回答
   - `answer_id` (主键)
   - `question_id` (外键 -> questions)
   - `body` (TEXT)
   - `score`
   - `accepted`
   - `creation_date`
   - `owner_account_id` (外键 -> users)

6. **comments** - 评论
   - `comment_id` (主键)
   - `post_id`
   - `post_type` ("question" 或 "answer")
   - `body` (TEXT)
   - `score`
   - `creation_date`
   - `owner_account_id` (外键 -> users)
   - `question_id` (外键 -> questions, 可选)
   - `answer_id` (外键 -> answers, 可选)

### 索引

- `users`: `account_id`, `user_id`
- `tags`: `name`
- `questions`: `creation_date`, `score`, `answered`
- `answers`: `question_id`, `accepted`, `score`
- `comments`: `post_id`, `post_type`

## 🔧 配置说明

### application.properties

```properties
# 数据库连接
spring.datasource.url=jdbc:postgresql://localhost:5432/stackoverflow_java
spring.datasource.username=postgres
spring.datasource.password=postgres

# JPA 配置
spring.jpa.hibernate.ddl-auto=validate  # 使用 Flyway 管理 schema，不自动创建
spring.jpa.show-sql=false               # 生产环境设为 false

# Flyway 配置
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

### 环境变量

- `IMPORT_DATA`: 设置为 `true` 时自动导入数据
- `IMPORT_DIRECTORY`: 数据文件目录（默认：`Sample_SO_data`）

## 📝 使用示例

### 完整导入流程

```bash
# 1. 创建数据库
psql -U postgres -c "CREATE DATABASE stackoverflow_java;"

# 2. 配置数据库连接（编辑 application.properties）

# 3. 编译项目
mvnw.cmd clean package

# 4. 运行应用并导入数据
java -jar target/FinalProject_demo-0.0.1-SNAPSHOT.jar \
    --import.data=true \
    --import.directory=Sample_SO_data
```

### 验证导入结果

```sql
-- 连接到数据库
psql -U postgres -d stackoverflow_java

-- 查看统计
SELECT COUNT(*) FROM questions;
SELECT COUNT(*) FROM answers;
SELECT COUNT(*) FROM comments;
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM tags;

-- 查看示例数据
SELECT question_id, title, score, answer_count 
FROM questions 
ORDER BY creation_date DESC 
LIMIT 10;
```

## ⚠️ 注意事项

1. **数据导入是幂等的**
   - 重复导入会跳过已存在的记录
   - 不会创建重复数据

2. **事务处理**
   - 每个线程的导入在一个事务中
   - 如果某个线程导入失败，不会影响其他线程

3. **性能优化**
   - 大量数据导入时，考虑批量处理
   - 可以调整 JPA 的批量大小

4. **数据完整性**
   - 外键约束确保数据完整性
   - 删除问题时会级联删除相关回答和评论

## 🆘 常见问题

### Q1: 连接数据库失败

**A**: 检查：
- PostgreSQL 是否运行：`pg_isready`
- 数据库名称、用户名、密码是否正确
- 端口是否正确（默认 5432）

### Q2: Flyway 迁移失败

**A**: 
- 检查数据库是否已存在表结构
- 如果表已存在，设置 `spring.flyway.baseline-on-migrate=true`
- 或手动执行迁移脚本

### Q3: 导入速度慢

**A**:
- 考虑禁用 JPA 的 SQL 日志：`spring.jpa.show-sql=false`
- 使用批量插入（需要额外配置）
- 确保数据库有足够的资源

### Q4: 内存不足

**A**:
- 增加 JVM 内存：`-Xmx2g`
- 分批导入数据
- 使用流式处理

---

**提示**：导入完成后，可以更新 `LocalDatasetRepository` 以从数据库读取数据，而不是从文件读取。


























