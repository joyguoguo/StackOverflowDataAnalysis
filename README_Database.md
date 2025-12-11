# PostgreSQL 数据库集成说明

## ✅ 已完成的功能

项目已成功集成 PostgreSQL 数据库支持，可以将 JSON 文件数据导入到数据库中。

### 1. 数据库实体类

- ✅ `UserEntity` - 用户信息
- ✅ `QuestionEntity` - 问题
- ✅ `AnswerEntity` - 回答
- ✅ `CommentEntity` - 评论
- ✅ `TagEntity` - 标签

### 2. Repository 接口

- ✅ `UserRepository`
- ✅ `QuestionRepository`
- ✅ `AnswerRepository`
- ✅ `CommentRepository`
- ✅ `TagRepository`

### 3. 数据导入服务

- ✅ `DataImportService` - 从 JSON 文件导入到数据库
- ✅ `DataImportRunner` - 命令行导入工具

### 4. 数据库迁移

- ✅ Flyway 迁移脚本 (`V1__Create_initial_schema.sql`)
- ✅ 自动创建表结构和索引

## 🚀 快速开始

### 步骤 1：安装 PostgreSQL

```bash
# 使用 Docker（推荐）
docker run -d -p 5432:5432 \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=stackoverflow_java \
  --name postgres-so \
  postgres:15

# 或使用本地安装的 PostgreSQL
```

### 步骤 2：配置数据库连接

编辑 `src/main/resources/application.properties`：

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/stackoverflow_java
spring.datasource.username=postgres
spring.datasource.password=postgres
```

### 步骤 3：编译并运行

```bash
# 编译项目
mvnw.cmd clean package

# 运行应用（自动创建表结构）
java -jar target/FinalProject_demo-0.0.1-SNAPSHOT.jar
```

### 步骤 4：导入数据

```bash
# 方式一：使用命令行参数
java -jar target/FinalProject_demo-0.0.1-SNAPSHOT.jar \
    --import.data=true \
    --import.directory=Sample_SO_data

# 方式二：使用环境变量
set IMPORT_DATA=true
set IMPORT_DIRECTORY=Sample_SO_data
java -jar target/FinalProject_demo-0.0.1-SNAPSHOT.jar
```

## 📊 数据库结构

### 表关系图

```
users (用户)
  ├── questions (问题) - 1:N
  ├── answers (回答) - 1:N
  └── comments (评论) - 1:N

questions (问题)
  ├── answers (回答) - 1:N
  ├── comments (评论) - 1:N
  └── tags (标签) - N:M (通过 question_tags)

answers (回答)
  └── comments (评论) - 1:N
```

### 主要表

1. **users** - 存储用户信息
2. **questions** - 存储问题
3. **answers** - 存储回答
4. **comments** - 存储评论
5. **tags** - 存储标签
6. **question_tags** - 问题标签关联表

## 📝 使用示例

### 在代码中使用 Repository

```java
@Autowired
private QuestionRepository questionRepository;

public void queryQuestions() {
    // 查询所有问题
    List<QuestionEntity> questions = questionRepository.findAll();
    
    // 按标签查询
    List<QuestionEntity> javaQuestions = questionRepository.findByTagName("java");
    
    // 按时间范围查询
    Instant from = Instant.now().minus(365, ChronoUnit.DAYS);
    Instant to = Instant.now();
    List<QuestionEntity> recentQuestions = 
        questionRepository.findByCreationDateBetween(from, to);
}
```

### 导入数据

```java
@Autowired
private DataImportService importService;

public void importData() {
    DataImportService.ImportResult result = 
        importService.importFromDirectory("Sample_SO_data");
    
    System.out.println("Success: " + result.getSuccessCount());
    System.out.println("Failed: " + result.getFailedCount());
}
```

## ⚙️ 配置说明

### application.properties

```properties
# 数据库连接
spring.datasource.url=jdbc:postgresql://localhost:5432/stackoverflow_java
spring.datasource.username=postgres
spring.datasource.password=postgres

# JPA 配置
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Flyway 配置
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

## 🔄 下一步

1. **更新服务层**：将现有的分析服务改为从数据库读取数据
2. **性能优化**：添加缓存、优化查询
3. **数据同步**：定期从 API 采集新数据并导入数据库

## 📚 相关文档

- [数据库设置指南](docs/DatabaseSetup.md) - 详细的设置说明
- [设计文档](docs/Design.md) - 系统架构设计

---

**状态**: ✅ PostgreSQL 集成已完成，可以开始使用数据库存储数据







