<!-- d48a2a23-b5aa-4dd3-8eed-a752761ac349 c0b51a06-00fe-4b5a-9e03-61b5627251a8 -->
# 添加提问者声誉分布箱线图

## 概述

在现有的声誉分布分组柱状图基础上，添加一个箱线图来更直观地展示易解决问题和难解决问题的提问者声誉分布，包括五数概括和异常值检测。

## 阶段一：扩展 DTO 结构

### 1.1 更新 SolvabilityContrastResponse

添加箱线图数据字段：

- `reputation_boxplot_data`: 包含两组问题的箱线图统计数据

**文件**: `src/main/java/cs209a/finalproject_demo/dto/SolvabilityContrastResponse.java`

**新结构**:

```java
public record SolvabilityContrastResponse(
    List<FeatureComparison> comparison_data,
    List<TagFrequencyData> tag_frequency_data,
    CommentFrequencyData comment_frequency_data,
    DistributionData code_snippet_ratio_distribution,
    DistributionData tag_count_distribution,
    DistributionData question_length_distribution,
    DistributionData reputation_distribution,
    DistributionData comment_count_distribution,
    BoxPlotData reputation_boxplot_data  // 新增
) {
    public record BoxPlotData(
        BoxPlotStats solvable,
        BoxPlotStats hard
    ) {}
    
    public record BoxPlotStats(
        double min,
        double q1,      // 第一四分位数
        double median,  // 中位数
        double q3,      // 第三四分位数
        double max,
        List<Double> outliers  // 异常值列表
    ) {}
}
```

## 阶段二：实现服务方法

### 2.1 计算箱线图统计数据

实现方法计算声誉的五数概括和异常值：

- 收集所有声誉值
- 计算最小值、Q1、中位数、Q3、最大值
- 使用IQR方法检测异常值（Q1 - 1.5*IQR 和 Q3 + 1.5*IQR 之外的值）

**文件**: `src/main/java/cs209a/finalproject_demo/service/SolvabilityContrastService.java`

**方法**: `calculateReputationBoxPlotData(List<QuestionEntity> solvable, List<QuestionEntity> hard)`

### 2.2 辅助方法

- `calculateBoxPlotStats(List<Integer> values)`: 计算单个数据集的箱线图统计
- `calculatePercentile(List<Integer> sortedValues, double percentile)`: 计算百分位数
- `detectOutliers(List<Integer> values, double q1, double q3)`: 检测异常值

### 2.3 更新 analyze() 方法

在计算声誉分布后，调用箱线图计算方法并包含在响应中。

## 阶段三：前端实现

### 3.1 添加 HTML 结构

在声誉分布图附近添加箱线图容器。

**文件**: `src/main/resources/templates/index.html`

### 3.2 实现箱线图

由于项目已使用D3.js，使用D3.js绘制箱线图：

- 创建SVG容器
- 绘制箱体（Q1到Q3的矩形）
- 绘制中位数线
- 绘制须线（从箱体到min/max的线）
- 绘制异常值点
- 添加标签和工具提示

**函数**: `initReputationBoxPlot()`, `renderReputationBoxPlot()`

### 3.3 更新 loadSolvability() 函数

从API响应中获取箱线图数据并渲染。

## 实现细节

### 箱线图统计计算

1. 收集所有声誉值并排序
2. 计算百分位数：

   - Q1: 25%分位数
   - Median: 50%分位数
   - Q3: 75%分位数

3. 计算IQR = Q3 - Q1
4. 异常值检测：

   - 下界 = Q1 - 1.5 * IQR
   - 上界 = Q3 + 1.5 * IQR
   - 超出上下界的值为异常值

### D3.js箱线图绘制

- 使用D3的scale来映射数据到像素位置
- 绘制两个并排的箱线图（易解决和难解决）
- 添加坐标轴和标签
- 实现交互式工具提示显示具体数值

### To-dos

- [ ] 调整 QuestionRepository 中 findSolvableQuestions / findHardToSolveQuestions 的 JPQL 条件，加入 closedDate 过滤并简化为粗筛逻辑
- [ ] 按新规则重写 SolvabilityContrastService.refineSolvableQuestions，实现“有采纳答案且 2 小时内采纳，且未关闭”的过滤
- [ ] 按新规则重写 SolvabilityContrastService.refineHardQuestions，实现“无采纳但有高分答案”与“长期无人回答且未关闭”的过滤
- [ ] （可选）在前端页面或文档中更新易解决 / 难解决问题定义的中文描述