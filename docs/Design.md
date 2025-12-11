# Stack Overflow Java Q&A 分析系统设计文档

> 版本：v1.0  
> 项目：`CS209A_FinalProject_demo`  
> 最近更新：2025-11-25

## 1. 背景与目标
- **背景**：当前仓库基于 Spring Boot 3.5.7，提供了一个包含 `HomeController` 与静态 `index.html` 的最小可运行示例，缺少数据接入与分析能力。
- **目标**：在此基础上实现一个离线分析 Stack Overflow Java 线程的全栈系统，覆盖数据采集、存储、分析、可视化及 REST API，满足课程四大分析需求。
- **范围**：本设计文档定义系统的整体架构、模块职责、数据库结构、核心算法、接口规范、前端交互、非功能指标及测试/部署计划。

## 2. 总体架构
- **分层结构**：`数据采集层 → 数据存储层 → 后端服务层 → REST API → 前端可视化层`。
- **技术栈**
  - 后端：Spring Boot、Spring Web、Spring Data JPA（或 MyBatis）、Jackson、Lombok。
  - 数据库：PostgreSQL（推荐）或 MySQL。
  - 前端：Thymeleaf + 原生 JS/TypeScript，结合 ECharts/Chart.js；后续可替换为 React/Vue 独立前端。
  - 辅助：MapStruct 做 DTO 转换，Flyway 维护 schema，Scheduler 或独立 CLI 完成数据抓取。
- **部署**：单体 Jar，默认运行在 `http://localhost:8080`，通过 `application-{profile}.properties` 管理环境配置。

## 3. 数据采集与持久化
### 3.1 数据来源
- 使用 Stack Exchange API (`/questions`, `/answers`, `/comments`, `/tags`) 结合 `tagged=java`。
- 分页抓取，控制速率与重试；记录 `has_more`、`quota_remaining`。

### 3.2 流程
1. **采集调度**：CLI `DataCollectorApplication` 或 Spring `CommandLineRunner`，读取配置决定时间窗口、数量、并发度。
2. **请求封装**：`collector.client.StackOverflowClient` 负责签名参数、速率限制、失败重试。
3. **数据标准化**：使用 `collector.mapper` 将 JSON 映射为内部 DTO（QuestionDTO、AnswerDTO 等）。
4. **持久化**：通过 `collector.service.IngestionService` 写入数据库（或生成 CSV/JSON 后批量导入）。
5. **日志与审计**：记录批次 id、拉取时间、成功/失败计数、异常明细。

### 3.3 数据模型
| 表/文件 | 关键字段 | 说明 |
|---------|----------|------|
| `questions` | `id`, `title`, `body`, `tags[]`, `owner_id`, `creation_time`, `score`, `answer_count`, `accepted_answer_id`, `has_code_snippet`, `word_count` | 主体表 |
| `answers` | `id`, `question_id`, `body`, `score`, `is_accepted`, `owner_id`, `creation_time` | |
| `comments` | `id`, `post_id`, `post_type`, `body`, `score`, `owner_id`, `creation_time` | |
| `users` | `id`, `display_name`, `reputation`, `user_type` | 仅存相关用户 |
| `tags` | `id`, `name` | 可选，用于扩展 |
| `thread_metrics` | `question_id`, `view_count`, `favorite_count`, `edit_count`, `last_activity_time` | 便于查询 |

说明：若初期采用文件格式，需定义 Schema 相同的 JSON/Parquet，并提供 DAO 兼容层。

## 4. 后端服务层设计
### 4.1 包结构（建议）
```
cs209a.finalproject_demo
 ├─ collector      # 离线采集
 ├─ config         # 全局配置、常量
 ├─ controller     # REST & 页面控制器（现有 HomeController 将迁移至该包并补注解）
 ├─ dto            # API 返回对象
 ├─ entity         # JPA 实体
 ├─ repository     # 数据访问接口
 ├─ service
 │   ├─ analytics  # 四大分析
 │   ├─ query      # 通用查询
 │   └─ pipeline   # 数据清洗、特征工程
 ├─ util           # 工具
 └─ viewmodel      # 前端渲染模型（若使用 Thymeleaf）
```

### 4.2 关键服务
- `TopicTrendService`：基于时间桶（按月/周）统计各话题的活动指标。
- `TopicCooccurrenceService`：生成标签组合频率矩阵，支持 Top N 与最小出现阈值。
- `MultithreadingInsightService`：对正文进行文本解析（正则 + 简易 NLP），归类多线程常见问题。
- `SolvabilityContrastService`：计算问题可解性标签，统计多维特征差异。
- `VisualizationFacade`：聚合 Service 输出，为 Controller 提供统一接口，便于缓存/鉴权。

### 4.3 公共组件
- `TimeBucketUtil`：生成时间区间、对齐时间戳。
- `TextAnalyzer`：代码片段检测、关键词抽取、异常信息解析。
- `FeatureExtractor`：为每个问题计算特征（长度、标签数量、提问者声望等）。
- `CacheConfig`：使用 Caffeine/Redis 缓存热点请求（缓存时间 < 10 min，确保“动态生成”）。

## 5. 核心分析算法
### 5.1 Topic Trends
- **输入**：`topics`, `metric`, `from`, `to`, `bucketSize`.
- **实现**：
  1. 基于标签+正文关键词映射话题（`topic -> {tags, keywords}`）。
  2. 按时间桶统计问题数量、回答数量或互动指标。
  3. 产出序列数据（`[{topic, bucketStart, metricValue}]`）。
- **可视化**：多折线图/堆叠面积图。

### 5.2 Topic Co-occurrence
- **输入**：`topN`, `minScore`, `timeFilter`.
- **实现**：
  1. 对每条问题的标签做组合 `C(n,2)`。
  2. 使用 `Map<Pair<String,String>, Long>` 或 SQL `GROUP BY`.
  3. 排序、支持前端归一化显示。
- **可视化**：和弦图、热力图或条形图。

### 5.3 Multithreading Pitfalls
- **输入**：`topN`, `includeCode`, `minScore`.
- **实现**：
  1. 过滤 `tag` 包含 `multithreading/concurrency/thread`。
  2. `TextAnalyzer` 提取异常/关键词（`Pattern` 匹配：`IllegalMonitorState`, `deadlock`, `wait()` 等）。
  3. 统计频率，附示例问题 id、典型代码片段。
  4. 可结合 TF-IDF/简易 LDA 进一步聚类。
- **可视化**：词云、排名条形图，支持点击展示样例。

### 5.4 Solvable vs Hard-to-Solve
- **可解性定义**：
  - Solvable：`accepted_answer` 存在且回答时间 < 48h 且得分 ≥ 1。
  - Hard：无采纳且在 7 天内无回答或得分 < 0。
- **特征**（至少 3 个）：问题长度、代码片段存在与否、标签数量、提问者声望、提问时间段等。
- **实现**：
  1. `FeatureExtractor` 为每个问题生成特征向量。
  2. 分别计算均值/中位数/分布。
  3. 输出对比结构，供雷达图/箱线图展示。

## 6. REST API 设计
| 方法 | 路径 | 描述 | 请求参数 | 响应示例 |
|------|------|------|----------|----------|
| GET | `/api/topic-trends` | 获取话题趋势数据 | `topics=a,b`, `metric=questions`, `from=2022-01`, `to=2024-12`, `bucket=MONTH` | `{ "series":[{ "topic":"lambda", "points":[{"bucket":"2023-01","value":12}, ...]}], "meta":{} }` |
| GET | `/api/cooccurrence` | 获取话题共现 Top N | `topN=10`, `minScore=0` | `{ "pairs":[{"topics":["spring","security"],"count":56}], ... }` |
| GET | `/api/multithreading/pitfalls` | 多线程常见问题 | `topN=5`, `includeExamples=true` | `{ "pitfalls":[{"label":"死锁", "count":42, "examples":[12345,23456]}] }` |
| GET | `/api/solvability/contrast` | 可解性对比 | `from`, `to`, `factors=length,codeSnippet` | `{ "features":[{"name":"question_length","solvable":{"avg":120}, "hard":{"avg":240}}] }` |
| GET | `/api/metadata/status` | 数据概况/健康检查 | 无 | `{ "threads":1500,"lastUpdated":"2025-11-20T10:20:00Z" }` |

规范：
- 统一返回 `ApiResponse<T>`：`{ "data": ..., "meta": {...}, "timestamp": ... }`
- 错误处理使用 `@ControllerAdvice`，提供友好错误消息。
- 对时间参数使用 ISO8601，验证参数范围。

## 7. 前端设计
### 7.1 页面结构
1. **Dashboard**：核心指标概览（数据量、更新时间、快捷入口）。
2. **Topic Trends**：筛选条（话题、时间范围、指标）+ 折线/面积图 + 洞察文本。
3. **Topic Co-occurrence**：Top N 列表、热力图、节点图。
4. **Multithreading Pitfalls**：条形图 + 词云 + 样例弹窗。
5. **Solvability**：雷达/箱线图 + 表格说明。
6. **数据管理**（可选）：展示采集批次、刷新按钮。

### 7.2 交互与状态
- 使用 `fetch`/`axios` 调用 REST API，Promise 结果更新 Chart。
- Loading / Empty / Error 三态提示。
- 主题色沿用 `index.html` 风格，可复用 Chart.js。

## 8. 安全与非功能
- **性能**：典型查询 < 3s；当数据量 > 10k 时需分页与索引（`tags`, `creation_time`）。
- **缓存**：分析结果缓存 5 分钟，键包含参数；手动刷新或数据更新时失效。
- **可靠性**：采集脚本支持断点续传；数据库定期备份。
- **安全**：对外 API 增加请求限流、输入校验；如部署公网需加鉴权（Basic/JWT）。
- **可观测性**：采用 `spring-boot-starter-actuator`，记录 API 耗时与异常数。

## 9. 测试计划
- **单元测试**：`TopicTrendServiceTest`, `TextAnalyzerTest` 等，使用 Mock 数据。
- **集成测试**：`@SpringBootTest` + Testcontainers 连接真实数据库。
- **端到端**：启动后端 + 前端，使用 Cypress/Playwright 验证交互流程。
- **性能测试**：JMeter/Locust 模拟多用户访问趋势接口。
- **数据验证**：采集脚本完成后运行校验任务，检查线程数量、空字段、重复记录。

## 10. 部署与运维
- **环境**：`dev`（内存数据库 + mock 数据）、`prod`（真实数据 + PostgreSQL）。
- **配置管理**：`application-dev.properties`、`application-prod.properties` 区分数据库、API Key。
- **启动流程**：
  1. `java -jar finalproject_demo.jar --spring.profiles.active=prod`
  2. 前端静态资源打包至 `src/main/resources/static` 或独立部署并通过 Nginx 反向代理。
- **监控**：利用 Actuator `/actuator/health`, `/metrics`；日志集中输出。

## 11. 里程碑 & 分工建议
| 阶段 | 时间 | 任务 |
|------|------|------|
| M1 | Week 1 | 完成数据模型、采集脚本 PoC，抓取 ≥200 线程 |
| M2 | Week 2 | 搭建数据库、实体、Repository，导入 ≥1000 线程 |
| M3 | Week 3 | 实现四个分析服务 + REST API，单元测试完成 |
| M4 | Week 4 | 前端可视化与交互完成，接入真实数据 |
| M5 | Week 5 | 性能优化、缓存、非功能完善，准备洞察内容 |
| M6 | Week 6 | 集成测试、演练答辩、准备展示材料 |

## 12. 与现有代码的衔接
- 现有 `HomeController` 缺少 `@Controller` 注解，应补充并扩展为路由入口；后续新增 `ApiController`、`DashboardController` 将前后端按职责划分。
- `index.html` 将升级为 Dashboard 的基础模板，抽离公共样式为 `static/css/main.css`，并引入 JS 模块化脚本。
- 后端 `FinalProjectDemoApplication` 保持入口不变，新增配置类与 Bean。

---
该设计文档覆盖系统蓝图与实施路径，可作为后续需求细化、开发排期及验收依据。若需求变更（如增加新分析问题或切换前端框架），可在对应章节扩展。 


