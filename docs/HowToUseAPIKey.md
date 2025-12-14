# 如何使用 Stack Overflow API Key

根据你提供的截图，你已经创建了 API Key。本文档说明如何使用它进行数据采集。

## 📋 什么是 API Key？

- **API Key**：用于只读操作的匿名用户访问
- **每日配额**：每个 IP 地址每天 10,000 次请求（所有 API Key 共享）
- **用途**：适合数据采集等只读操作
- **不需要**：OAuth 流程或用户认证

## 🔑 获取完整的 API Key

从截图中可以看到，你的 API Key 显示为 `rl_iyRnJ******`（部分隐藏）。

### 获取完整 Key 的方法：

1. **点击 Key 列中的值**
   - 在 API keys 表格中，点击 Key 列中显示的值
   - 系统可能会显示完整的 Key

2. **查看 Key 详情**
   - 点击 "Actions" 列中的操作（如果有查看选项）
   - 或者尝试复制 Key 值

3. **如果无法查看完整 Key**
   - 可以点击 "Rotate"（轮换）生成新的 Key
   - 新生成的 Key 会显示完整值（仅在生成时显示一次）
   - **注意**：轮换后旧的 Key 会失效

## 🚀 使用方法

### 方式一：通过命令行参数（推荐）

```cmd
# Windows
collect-data.bat 1000 Sample_SO_data2 rl_iyRnJYLzYdHXWn2GhMRt9iSk6

# 或使用 Maven
mvnw.cmd exec:java -Dexec.mainClass="cs209a.finalproject_demo.collector.SimpleDataCollector" -Dexec.args="1000 Sample_SO_data2 rl_iyRnJYLzYdHXWn2GhMRt9iSk6"
```

**参数说明**：
- 第一个参数：`1000` - 要采集的线程数量
- 第二个参数：`Sample_SO_data2` - 输出目录
- 第三个参数：`rl_iyRnJYLzYdHXWn2GhMRt9iSk6` - 你的 API Key

### 方式二：通过环境变量

#### Windows (PowerShell)
```powershell
$env:SO_API_KEY = "rl_iyRnJYLzYdHXWn2GhMRt9iSk6"
collect-data.bat 1000 Sample_SO_data2
```

#### Windows (CMD)
```cmd
set SO_API_KEY=rl_iyRnJYLzYdHXWn2GhMRt9iSk6
collect-data.bat 1000 Sample_SO_data2
```

#### Linux/Mac
```bash
export SO_API_KEY=rl_iyRnJYLzYdHXWn2GhMRt9iSk6
./collect-data.sh 1000 Sample_SO_data2
```

### 方式三：通过配置文件

编辑 `src/main/resources/application.properties`：

```properties
so.api.key=rl_iyRnJYLzYdHXWn2GhMRt9iSk6
```

## ⚠️ 重要提示

### 1. API Key 的安全性

- **不要**将 API Key 提交到 Git 仓库
- **不要**在公开场合分享你的 API Key
- 如果 Key 泄露，立即在管理页面点击 "Rotate" 生成新 Key

### 2. 配额限制

- **每日配额**：10,000 次请求（基于 IP 地址）
- **所有 API Key 共享**：同一个 IP 的所有 API Key 共享这个配额
- **配额重置**：每天 UTC 0 点重置

### 3. 配额计算

采集 1000 个线程大约需要：
- 获取问题列表：~10-20 次请求（每页 100 个问题）
- 获取回答：~1000 次请求（每个问题一次）
- 获取评论：~1000-2000 次请求（每个问题/回答一次）
- **总计**：约 2000-3000 次请求

**结论**：10,000 次请求的配额足够采集 1000 个线程。

## 🔍 验证 API Key 是否生效

运行采集工具后，检查日志输出：

```
INFO  - API request successful. Items: 100, Has more: true, Quota remaining: 9700
```

如果看到：
- **Quota remaining** 值接近 10,000，说明 API Key 生效
- **没有错误信息**，说明 Key 有效

## 🆘 常见问题

### Q1: 如何查看完整的 API Key？

**A**: 
1. 在 API keys 表格中，尝试点击 Key 列的值
2. 如果无法查看，点击 "Rotate" 生成新 Key（新 Key 会显示完整值）
3. **注意**：轮换后旧 Key 会失效

### Q2: API Key 和 Access Token 有什么区别？

**A**:
- **API Key**：用于只读操作，简单易用，适合数据采集
- **Access Token**：用于读写操作，需要 OAuth 流程，配额更高但更复杂

对于数据采集项目，**API Key 就足够了**。

### Q3: 配额用完了怎么办？

**A**:
1. 等待第二天 UTC 0 点配额重置
2. 使用不同的 IP 地址（如果有）
3. 减少采集数量，分多天完成

### Q4: 可以同时使用多个 API Key 吗？

**A**: 
- 可以创建多个 API Key
- 但所有 Key 共享同一个 IP 的每日配额（10,000 次）
- 创建多个 Key 不会增加配额

## 📝 完整示例

### Windows 示例

```cmd
# 设置 API Key
set SO_API_KEY=rl_iyRnJYLzYdHXWn2GhMRt9iSk6

# 运行采集工具
collect-data.bat 1000 Sample_SO_data2

# 或者直接传递 Key
collect-data.bat 1000 Sample_SO_data2 rl_iyRnJYLzYdHXWn2GhMRt9iSk6
```

### Linux/Mac 示例

```bash
# 设置 API Key
export SO_API_KEY=rl_iyRnJYLzYdHXWn2GhMRt9iSk6

# 运行采集工具
./collect-data.sh 1000 Sample_SO_data2

# 或者直接传递 Key
./collect-data.sh 1000 Sample_SO_data2 rl_iyRnJYLzYdHXWn2GhMRt9iSk6
```

## ✅ 快速检查清单

- [ ] 已获取完整的 API Key
- [ ] 已在项目中配置 API Key（环境变量或命令行参数）
- [ ] 已验证 API Key 是否生效（检查日志中的配额）
- [ ] 已确认配额足够（10,000 次请求足够采集 1000 个线程）

---

**提示**：根据你的截图，你的 API Key 是 `rl_iyRnJYLzYdHXWn2GhMRt9iSk6`（从你之前提供的 token 推测）。如果这是完整的 Key，可以直接使用。如果 Key 被部分隐藏，请按照上面的方法获取完整 Key。
















