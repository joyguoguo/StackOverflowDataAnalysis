# 快速开始 - 数据采集

## 🚀 最简单的运行方式

### 方式一：使用脚本（推荐）

#### Windows 用户

```cmd
# 给脚本添加执行权限（首次运行）
# 直接运行
collect-data.bat 1000 Sample_SO_data

# 或使用访问令牌
collect-data.bat 1000 Sample_SO_data your_access_token
```

#### Linux/Mac 用户

```bash
# 给脚本添加执行权限（首次运行）
chmod +x collect-data.sh

# 运行
./collect-data.sh 1000 Sample_SO_data

# 或使用访问令牌
./collect-data.sh 1000 Sample_SO_data your_access_token
```

### 方式二：使用 Maven 直接运行（无需构建 jar）

#### 步骤 1：先编译项目

```bash
# Windows
mvnw.cmd clean compile

# Linux/Mac
./mvnw clean compile
```

#### 步骤 2：运行采集工具

```bash
# Windows
java -cp "target/classes;%USERPROFILE%\.m2\repository\com\fasterxml\jackson\core\jackson-databind\*\jackson-databind-*.jar;%USERPROFILE%\.m2\repository\com\fasterxml\jackson\core\jackson-core\*\jackson-core-*.jar;%USERPROFILE%\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\*\jackson-annotations-*.jar;%USERPROFILE%\.m2\repository\org\slf4j\slf4j-api\*\slf4j-api-*.jar" cs209a.finalproject_demo.collector.SimpleDataCollector 1000 Sample_SO_data

# 或使用 Maven 构建类路径（更简单）
mvnw.cmd exec:java -Dexec.mainClass="cs209a.finalproject_demo.collector.SimpleDataCollector" -Dexec.args="1000 Sample_SO_data"
```

实际上，更简单的方式是：

### 方式三：构建 jar 后运行（适合生产环境）

#### 步骤 1：构建项目

```bash
# Windows
mvnw.cmd clean package

# Linux/Mac  
./mvnw clean package
```

#### 步骤 2：运行

由于 Spring Boot 打包的 jar 是 fat jar，不能直接使用 `-cp` 运行。需要创建一个新的启动类或使用 Spring Boot 的方式。

**最简单的方法**：使用 Maven 运行主应用，然后调用采集服务。或者使用下面的方式。

## ⚡ 最推荐的运行方式

由于项目依赖 Spring Boot，最简单的方式是：

### 1. 编译项目

```bash
mvnw.cmd clean compile
# 或
./mvnw clean compile
```

### 2. 使用 Maven Exec 插件运行

首先在 `pom.xml` 中添加 exec 插件（见下方），然后运行：

```bash
mvnw.cmd exec:java -Dexec.mainClass="cs209a.finalproject_demo.collector.SimpleDataCollector" -Dexec.args="1000 Sample_SO_data"
```

或者，**更简单的方式**：我已经创建了启动脚本，直接运行：

```bash
# Windows
collect-data.bat 1000 Sample_SO_data

# Linux/Mac
chmod +x collect-data.sh
./collect-data.sh 1000 Sample_SO_data
```

## 🔧 如果遇到问题

### 问题：找不到类

**解决方案**：
1. 确保已经编译：`mvnw clean compile`
2. 检查 `target/classes` 目录下是否有编译后的 `.class` 文件
3. 使用提供的脚本 `collect-data.bat` 或 `collect-data.sh`

### 问题：缺少依赖

**解决方案**：
1. 运行 `mvnw clean install` 下载所有依赖
2. 使用脚本运行，脚本会自动处理依赖路径

### 问题：权限错误（Linux/Mac）

**解决方案**：
```bash
chmod +x collect-data.sh
chmod +x mvnw
```

## 📝 完整示例

### Windows PowerShell

```powershell
# 进入项目目录
cd "D:\南方科技大学\大三上\cs209A\project\CS209A_FinalProject_demo"

# 运行采集脚本
.\collect-data.bat 1000 Sample_SO_data
```

### Linux/Mac Terminal

```bash
# 进入项目目录
cd /path/to/CS209A_FinalProject_demo

# 给脚本执行权限（首次）
chmod +x collect-data.sh

# 运行采集脚本
./collect-data.sh 1000 Sample_SO_data
```

---

**提示**：如果仍然遇到问题，请查看 `docs/DataCollection.md` 获取更详细的说明。
































