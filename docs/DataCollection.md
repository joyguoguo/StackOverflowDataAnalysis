# 数据采集指南

本文档说明如何从 Stack Overflow API 采集 Java 相关的问答数据。

## 📋 前置要求

1. **Stack Overflow 账户**（可选但推荐）
   - 未认证用户：每分钟最多 300 个请求
   - 已认证用户：每分钟最多 10000 个请求
   - 注册地址：https://stackoverflow.com/users/signup
   - 📖 **详细认证步骤**：请参考 [Stack Overflow 认证指南](StackOverflowAuthentication.md)

2. **访问令牌（Access Token）或 Key**（可选）
   - 如果已注册账户，可以创建 OAuth 应用并获取 Key/Token 以提升配额
   - 创建地址：https://stackoverflow.com/oauth/apps
   - 📖 **详细步骤**：请参考 [Stack Overflow 认证指南](StackOverflowAuthentication.md)

3. **网络连接**
   - 需要能够访问 Stack Exchange API（https://api.stackexchange.com）

## 🚀 快速开始

### 方式一：作为独立工具运行

1. **构建项目**
   ```bash
   mvn clean package
   ```

2. **运行采集工具**
   ```bash
   # 基本用法（采集 1000 个线程）
   java -jar target/FinalProject_demo-0.0.1-SNAPSHOT.jar \
       --collect.count=1000 \
       --collect.output=Sample_SO_data

   # 使用访问令牌
   java -jar target/FinalProject_demo-0.0.1-SNAPSHOT.jar \
       --collect.count=1000 \
       --collect.output=Sample_SO_data \
       --collect.token=YOUR_ACCESS_TOKEN

   # 指定日期范围
   java -jar target/FinalProject_demo-0.0.1-SNAPSHOT.jar \
       --collect.count=1000 \
       --collect.output=Sample_SO_data \
       --collect.from=2024-01-01 \
       --collect.to=2024-12-31
   ```

### 方式二：使用环境变量

```bash
export COLLECT_COUNT=1000
export COLLECT_OUTPUT=Sample_SO_data
export SO_ACCESS_TOKEN=your_token_here  # 可选
export COLLECT_FROM=2024-01-01          # 可选
export COLLECT_TO=2024-12-31            # 可选

java -jar target/FinalProject_demo-0.0.1-SNAPSHOT.jar
```

### 方式三：在 Spring Boot 应用中使用

如果要在现有的 Spring Boot 应用中集成采集功能：

1. **配置 application.properties**
   ```properties
   # Stack Overflow API 配置
   so.api.access-token=your_access_token_here
   
   # 数据采集配置
   collect.output=Sample_SO_data
   collect.default-count=1000
   ```

2. **在代码中调用**
   ```java
   @Autowired
   private DataCollectorService collectorService;
   
   public void collectData() {
       DataCollectorService.CollectionResult result = 
           collectorService.collectThreads(1000, "Sample_SO_data", null, null);
       log.info("Collected {} threads", result.getTotalCollected());
   }
   ```

## ⚙️ 配置参数

| 参数 | 环境变量 | 系统属性 | 说明 | 默认值 |
|------|---------|---------|------|--------|
| 采集数量 | `COLLECT_COUNT` | `collect.count` | 目标采集的线程数量 | `1000` |
| 输出目录 | `COLLECT_OUTPUT` | `collect.output` | 保存 JSON 文件的目录 | `Sample_SO_data` |
| 访问令牌 | `SO_ACCESS_TOKEN` | `collect.token` | Stack Overflow API 访问令牌 | 无 |
| 起始日期 | `COLLECT_FROM` | `collect.from` | 起始日期（YYYY-MM-DD 或 Unix 时间戳） | 过去一年 |
| 结束日期 | `COLLECT_TO` | `collect.to` | 结束日期（YYYY-MM-DD 或 Unix 时间戳） | 当前时间 |

## 📅 日期格式

支持以下日期格式：

- **ISO 日期**：`2024-01-01`
- **Unix 时间戳（秒）**：`1704067200`
- **Unix 时间戳（毫秒）**：`1704067200000`

## 📁 输出格式

采集的数据将保存为 JSON 文件，格式与 `Sample_SO_data/thread_XX.json` 一致：

```json
{
  "question": {
    "question_id": 123456,
    "title": "...",
    "body": "...",
    "tags": ["java", "spring"],
    "owner": {...},
    "creation_date": 1234567890,
    ...
  },
  "answers": [...],
  "question_comments": [...],
  "answer_comments": {
    "answer_id_1": [...],
    "answer_id_2": [...]
  }
}
```

文件命名：`thread_01.json`, `thread_02.json`, ..., `thread_NN.json`

## ⚠️ 速率限制与配额

### Stack Overflow API 速率限制

- **未认证用户**：每分钟 300 个请求
- **已认证用户**：每分钟 10000 个请求
- **每日配额**：取决于账户类型，通常在 10000 以上

### 工具内置保护

- ✅ 自动速率限制：每次请求间隔至少 100ms
- ✅ 自动重试：失败请求最多重试 3 次（指数退避）
- ✅ Backoff 处理：如果触发速率限制，自动等待
- ✅ 配额监控：实时显示剩余配额

### 建议

1. **不要一次性采集过多数据**：建议分批次采集
2. **使用访问令牌**：可以显著提高配额
3. **避开高峰期**：API 可能在高峰期响应较慢
4. **监控配额**：注意日志中的配额警告

## 🔧 故障排查

### 问题：采集速度很慢

- **原因**：速率限制或网络延迟
- **解决**：
  - 使用访问令牌提升配额
  - 检查网络连接
  - 减少并发请求（当前为单线程顺序采集）

### 问题：API 配额耗尽

- **原因**：每日请求次数超限
- **解决**：
  - 等待配额重置（通常为 UTC 0 点）
  - 使用其他账户的访问令牌
  - 减少采集数量

### 问题：某些线程采集失败

- **原因**：网络不稳定或 API 临时错误
- **解决**：
  - 工具会自动重试失败请求
  - 查看日志了解具体错误
  - 可以重新运行采集工具（已采集的数据不会重复）

### 问题：日期范围无效

- **原因**：日期格式不正确或范围不合理
- **解决**：
  - 检查日期格式（推荐使用 YYYY-MM-DD）
  - 确保起始日期早于结束日期
  - 如果不指定日期范围，工具会使用默认值（过去一年）

## 📊 采集进度

采集过程中会输出详细的日志信息：

```
INFO  - Starting data collection. Target: 1000 threads
INFO  - Fetching page 1 (collected: 0/1000)
INFO  - Collected thread 1/1000 (question_id: 123456)
INFO  - Progress: 10/1000 threads collected
...
INFO  - Collection completed. Total: 1000 threads
```

## 📝 示例脚本

### Windows (PowerShell)

```powershell
# 设置环境变量
$env:COLLECT_COUNT = 1000
$env:COLLECT_OUTPUT = "Sample_SO_data"
$env:SO_ACCESS_TOKEN = "your_token"

# 运行采集
java -jar target/FinalProject_demo-0.0.1-SNAPSHOT.jar
```

### Linux/Mac (Bash)

```bash
#!/bin/bash
export COLLECT_COUNT=1000
export COLLECT_OUTPUT=Sample_SO_data
export SO_ACCESS_TOKEN=your_token

java -jar target/FinalProject_demo-0.0.1-SNAPSHOT.jar
```

## 🔗 相关资源

- [Stack Exchange API 文档](https://api.stackexchange.com/docs)
- [Stack Overflow OAuth 应用](https://stackoverflow.com/oauth/apps)
- [API 速率限制说明](https://api.stackexchange.com/docs/throttle)

## 📌 注意事项

1. **数据使用协议**：采集的数据需遵守 Stack Overflow 的内容使用协议
2. **尊重速率限制**：不要过度请求，以免被限制访问
3. **数据备份**：建议定期备份采集的数据
4. **离线使用**：采集完成后，应用会使用本地数据，不再访问 API

## ✅ 验证采集结果

采集完成后，可以检查：

1. **文件数量**：确认输出目录中有足够数量的 JSON 文件
2. **文件格式**：随机打开几个文件，确认格式正确
3. **数据完整性**：检查是否包含问题、回答、评论等完整信息
4. **运行应用**：启动 Spring Boot 应用，访问 `/api/metadata/status` 查看数据统计

---

**提示**：如果遇到问题，请查看应用日志或参考故障排查章节。

