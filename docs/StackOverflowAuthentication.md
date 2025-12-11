# Stack Overflow API 认证指南

本文档详细说明如何成为 Stack Overflow 的认证用户，并获取 API 访问令牌以提升数据采集配额。

## 📋 为什么需要认证？

### 未认证用户 vs 已认证用户

| 项目 | 未认证用户 | 已认证用户 |
|------|-----------|-----------|
| **每分钟请求数** | 300 次 | 10,000 次 |
| **每日配额** | 较低 | 较高 |
| **访问令牌** | 不需要 | 需要 |

**结论**：如果计划采集大量数据（如 1000+ 线程），强烈建议使用认证用户和访问令牌。

---

## 🚀 步骤一：注册 Stack Overflow 账户

### 1.1 访问注册页面

打开浏览器，访问：
```
https://stackoverflow.com/users/signup
```

### 1.2 选择注册方式

Stack Overflow 支持多种注册方式：

- **使用 Google 账户**（推荐，最简单）
- **使用 GitHub 账户**
- **使用 Facebook 账户**
- **使用邮箱注册**

### 1.3 完成注册

1. 选择一种注册方式
2. 按照提示完成注册流程
3. 验证邮箱（如果使用邮箱注册）
4. 完成账户创建

**提示**：注册是免费的，不需要任何费用。

---

## 🔑 步骤二：创建 OAuth 应用并获取访问令牌

### 2.1 访问 OAuth 应用管理页面

登录 Stack Overflow 后，访问：
```
https://stackoverflow.com/oauth/apps
```

或者通过以下路径：
1. 点击右上角头像
2. 选择 "Settings"（设置）
3. 在左侧菜单中找到 "OAuth applications"（OAuth 应用）

### 2.2 创建新的 OAuth 应用

1. 点击 **"Register a new application"**（注册新应用）按钮

2. **填写应用信息**：
   - **Application name**（应用名称）：例如 `CS209A Data Collector`
   - **Description**（描述）：例如 `Data collection tool for CS209A final project`
   - **OAuth domain**（OAuth 域名）：可以填写 `localhost` 或你的域名
   - **Application website**（应用网站）：可以填写项目 GitHub 地址或 `http://localhost`

3. 点击 **"Register Application"**（注册应用）

### 2.3 获取访问令牌（Access Token）

创建应用后，你会看到：

- **Client ID**（客户端 ID）
- **Client Secret**（客户端密钥）
- **Key**（密钥）

**重要**：这些信息用于 OAuth 流程，但对于简单的 API 访问，我们还需要获取**个人访问令牌**。

### 2.4 获取个人访问令牌（Personal Access Token）

#### 方法一：通过 OAuth 流程（推荐用于生产环境）

如果你需要完整的 OAuth 流程，可以参考：
```
https://api.stackexchange.com/docs/authentication
```

#### 方法二：使用应用密钥（更简单，适合个人项目）

对于个人项目和数据采集，你可以：

1. **使用应用的 Key**：
   - 在 OAuth 应用页面找到你的应用的 **Key**
   - 这个 Key 可以直接在 API 请求中使用

2. **在 API 请求中添加参数**：
   ```
   ?key=YOUR_KEY&access_token=YOUR_ACCESS_TOKEN
   ```

**注意**：Stack Exchange API 的认证机制：
- **Key**：用于识别你的应用（可选，但推荐使用）
- **Access Token**：用于认证用户（可选，但使用后配额更高）

### 2.5 简化方式：仅使用 Key

对于数据采集项目，最简单的方式是：

1. 创建 OAuth 应用后，获取 **Key**
2. 在 API 请求中添加 `key` 参数
3. 虽然配额提升不如完整认证，但比未认证用户要好

---

## 📝 步骤三：在项目中使用访问令牌

### 3.1 方式一：通过环境变量（推荐）

#### Windows (PowerShell)
```powershell
$env:SO_ACCESS_TOKEN = "your_access_token_here"
$env:SO_API_KEY = "your_key_here"
```

#### Windows (CMD)
```cmd
set SO_ACCESS_TOKEN=your_access_token_here
set SO_API_KEY=your_key_here
```

#### Linux/Mac
```bash
export SO_ACCESS_TOKEN=your_access_token_here
export SO_API_KEY=your_key_here
```

### 3.2 方式二：通过命令行参数

```bash
# 使用脚本运行
collect-data.bat 1000 Sample_SO_data your_access_token

# 或使用 Maven
mvnw.cmd exec:java -Dexec.mainClass="cs209a.finalproject_demo.collector.SimpleDataCollector" \
    -Dexec.args="1000 Sample_SO_data your_access_token"
```

### 3.3 方式三：通过配置文件

编辑 `src/main/resources/application.properties`：

```properties
# Stack Overflow API 配置
so.api.access-token=your_access_token_here
so.api.key=your_key_here
```

---

## 🔧 更新代码以支持 Key

当前代码已经支持访问令牌。如果需要同时支持 Key，可以修改 `StackOverflowApiClient.java`：

```java
private final String apiKey;

public StackOverflowApiClient(String accessToken, String apiKey) {
    this.accessToken = accessToken;
    this.apiKey = apiKey;
    // ...
}

// 在构建 URL 时添加
if (apiKey != null && !apiKey.isEmpty()) {
    params.add("key=" + apiKey);
}
```

---

## ⚠️ 重要注意事项

### 1. 安全提示

- **不要**将访问令牌提交到 Git 仓库
- **不要**在公开场合分享你的访问令牌
- 如果令牌泄露，立即在 OAuth 应用页面撤销并重新生成

### 2. 配额限制

即使使用认证，仍然需要注意：
- **每分钟请求数**：最多 10,000 次
- **每日配额**：根据账户类型不同
- **速率限制**：如果触发限制，API 会返回 `backoff` 参数

### 3. 最佳实践

1. **使用访问令牌**：提升配额
2. **添加 Key**：帮助 Stack Exchange 识别你的应用
3. **遵守速率限制**：不要过度请求
4. **监控配额**：注意日志中的配额警告

---

## 📊 验证认证是否生效

运行数据采集工具后，检查日志输出：

```
INFO  - API request successful. Items: 100, Has more: true, Quota remaining: 9900
```

如果看到：
- **Quota remaining** 值很高（接近 10,000），说明认证成功
- **Quota remaining** 值很低（< 1000），可能是未认证或配额已用

---

## 🆘 常见问题

### Q1: 我没有看到 OAuth 应用选项？

**A**: 确保你已经登录 Stack Overflow 账户。如果仍然看不到，可能是账户权限问题，尝试：
1. 完善个人资料
2. 获得一些声望值（Reputation）
3. 等待一段时间后重试

### Q2: 访问令牌在哪里获取？

**A**: Stack Exchange API 的认证机制：
- **Key**：在 OAuth 应用页面直接可见
- **Access Token**：需要通过 OAuth 流程获取，或使用应用的 Key

对于数据采集项目，使用 **Key** 通常就足够了。

### Q3: 使用 Key 后配额没有提升？

**A**: 
- Key 主要用于识别应用，配额提升有限
- 完整的 OAuth 认证（Access Token）才能获得最大配额
- 检查是否正确在 API 请求中添加了 `key` 参数

### Q4: 如何撤销访问令牌？

**A**: 
1. 访问 https://stackoverflow.com/oauth/apps
2. 找到你的应用
3. 点击 "Revoke"（撤销）或删除应用

---

## 📚 相关资源

- [Stack Exchange API 文档](https://api.stackexchange.com/docs)
- [API 认证说明](https://api.stackexchange.com/docs/authentication)
- [OAuth 应用管理](https://stackoverflow.com/oauth/apps)
- [速率限制说明](https://api.stackexchange.com/docs/throttle)

---

## ✅ 快速检查清单

- [ ] 已注册 Stack Overflow 账户
- [ ] 已创建 OAuth 应用
- [ ] 已获取 Key（或 Access Token）
- [ ] 已在项目中配置访问令牌
- [ ] 已验证认证是否生效（检查配额）

完成以上步骤后，你就可以享受更高的 API 配额，更快地采集数据了！

---

**提示**：对于课程项目，如果只是采集 1000 个线程，未认证用户也可以完成，只是需要更多时间。认证主要用于提升效率和配额。








