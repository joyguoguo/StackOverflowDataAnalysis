# CS209A Final Project Demo

This is a simple Spring Boot project template designed to help you kickstart your CS209A final project — a web application for analyzing Stack Overflow Java Q&A data.

The demo includes:
- A basic homepage with a search bar and a pie chart.
- All code is written in Java using Spring Boot 3.5.7 and JDK 22.

---

## 🛠 Project Setup & Configuration

### Prerequisites
- **Java Development Kit (JDK) 22** (or higher)
- **IntelliJ IDEA** (Community or Ultimate Edition)

### Creating the Project from Scratch (Recommended)

If you prefer to create the project yourself (highly recommended for learning), follow these steps:

1. Open IntelliJ IDEA → **New Project** → Select **Spring Initializr**.
2. Configure the project as shown in the image below:

   ![Project Creation Settings](/imgs/proj_setting_0.png)

    - **Name**: `FinalProject_demo`
    - **Group**: `cs209a`
    - **Artifact**: `finalproject_demo`
    - **Package name**: `cs209a.finalproject_demo`
    - **JDK**: `openjdk-22 Oracle OpenJDK 22.0.1`
    - **Packaging**: `Jar`

3. Add the following dependencies:

   ![Dependencies Selection](/imgs/proj_setting_1.png)

    - **Spring Web**
    - **Thymeleaf**
    - **Spring Boot DevTools**

4. Click **Create** to generate the project.

---

## ▶️ How to Run the Project

1. Clone this repository (or create your own project based on the instructions above).
2. Open the project folder in IntelliJ IDEA.
3. Navigate to the main class: `src/main/java/cs209a/finalproject_demo/FinalProjectDemoApplication.java`.
4. Click the **Run** button (green triangle) next to the `main` method.

You will see logs similar to this in the console:

![Console Output](/imgs/cmd_output.png)

> ✅ Look for the line: `Tomcat started on port 8080 (http)` — this means your server is running!

---

## 🌐 Accessing the Frontend

Once the server is running, open your browser and visit:

```
http://localhost:8080
```

You should see the following homepage:

![Homepage Screenshot](/imgs/web_output.png)

This page includes:
- A **search bar** (placeholder functionality only).
- A **pie chart** showing "Thread Distribution by Type" (Type 1, Type 2, Type 3).

---

## 🚀 当前功能

- 读取 `Sample_SO_data/` 下的离线 Stack Overflow Java 线程样本并映射为本地内存数据集。
- 提供 REST API：
  - `GET /api/topic-trends`
  - `GET /api/cooccurrence`
  - `GET /api/multithreading/pitfalls`
  - `GET /api/solvability/contrast`
  - `GET /api/metadata/status`
- 前端仪表盘展示：
  - Topic Trends 折线图（可切换指标）
  - 标签共现 Top N 柱状图
  - 多线程常见问题条形图
  - 易解/难解问题雷达图
  - 数据概览卡片

## 📥 数据采集

本项目已实现完整的数据采集功能，可以从 Stack Overflow API 采集 Java 相关的问答数据。

### 快速开始

1. **使用独立采集工具（推荐）**

   ```bash
   # 编译项目
   mvn clean package
   
   # 采集 1000 个线程（使用环境变量）
   export COLLECT_COUNT=1000
   export COLLECT_OUTPUT=Sample_SO_data
   java -cp target/FinalProject_demo-0.0.1-SNAPSHOT.jar \
       cs209a.finalproject_demo.collector.SimpleDataCollector \
       1000 Sample_SO_data
   
   # 或使用访问令牌（可选，提升配额）
   export SO_ACCESS_TOKEN=your_access_token
   java -cp target/FinalProject_demo-0.0.1-SNAPSHOT.jar \
       cs209a.finalproject_demo.collector.SimpleDataCollector \
       1000 Sample_SO_data your_access_token
   ```

2. **在 Spring Boot 应用中集成**

   数据采集服务已集成到 Spring Boot 应用中，可以通过配置调用：

   ```java
   @Autowired
   private DataCollectorService collectorService;
   
   // 采集 1000 个线程
   CollectionResult result = collectorService.collectThreads(
       1000, "Sample_SO_data", null, null);
   ```

### 详细文档

更多使用说明、配置选项和故障排查，请参考 [数据采集指南](docs/DataCollection.md)。

**注意**：
- 需要能够访问 Stack Exchange API
- 建议创建 Stack Overflow 账户并使用访问令牌以提升配额
- API 有速率限制，采集大量数据需要时间

---

## 📈 下一步建议

1. ✅ **数据采集已完成**：可以从 Stack Overflow API 采集数据
2. 将当前内存分析逻辑迁移至数据库层（JPA/SQL），支持更大规模数据
3. 引入更多可配置筛选项与 Drill-down 交互
4. 为关键分析编写单元/集成测试，并优化性能与缓存策略

---
### RESTFUL
是的，项目已满足该要求，且有 2 个以上可演示的 RESTful API：
GET /api/cooccurrence?topN=10&filterCoreTopics=false：返回主题共现 Top N 对及频次，JSON。
GET /api/topic-trends?topics=java,spring&metric=QUESTIONS&from=2020-01-01&to=2020-12-31&topN=8：返回主题趋势分析，JSON。
GET /api/multithreading/pitfalls?topN=5：返回多线程坑点 Top N，JSON。
GET /api/solvability/contrast?from=2024-01-01&to=2024-12-31：返回易/难问题对比的全部特征、分布、箱线图数据，JSON。
GET /api/metadata/status：返回元数据快照（数据量/状态），JSON。
这些端点都在 AnalysisController 和 MetadataController 中定义，前端通过这些 REST API 获取数据进行可视化，符合“至少 2 个 REST 端点、可在浏览器演示”的要求。

Happy coding! 🚀