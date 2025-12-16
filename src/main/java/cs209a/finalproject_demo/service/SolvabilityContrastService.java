package cs209a.finalproject_demo.service;

import cs209a.finalproject_demo.dto.SolvabilityContrastResponse;
import cs209a.finalproject_demo.dto.SolvabilityContrastResponse.BoxPlotData;
import cs209a.finalproject_demo.dto.SolvabilityContrastResponse.BoxPlotStats;
import cs209a.finalproject_demo.dto.SolvabilityContrastResponse.CommentFrequencyData;
import cs209a.finalproject_demo.dto.SolvabilityContrastResponse.DistributionData;
import cs209a.finalproject_demo.dto.SolvabilityContrastResponse.FeatureComparison;
import cs209a.finalproject_demo.dto.SolvabilityContrastResponse.TagFrequencyData;
import cs209a.finalproject_demo.entity.AnswerEntity;
import cs209a.finalproject_demo.entity.QuestionCommentEntity;
import cs209a.finalproject_demo.entity.QuestionEntity;
import cs209a.finalproject_demo.entity.TagEntity;
import cs209a.finalproject_demo.repository.QuestionCommentRepository;
import cs209a.finalproject_demo.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 可解性对比分析服务
 * 分析可解决问题 vs. 难解决问题在多个特征上的差异
 */
@Service
public class SolvabilityContrastService {

    private final QuestionRepository questionRepository;
    private final QuestionCommentRepository questionCommentRepository;
    
    // 高复杂性主题标签
    private static final Set<String> COMPLEX_TOPICS = Set.of(
            "multithreading", "concurrency", "reflection", "jni", 
            "native", "bytecode", "instrumentation", "classloader"
    );
    
    // 代码片段检测模式
    private static final Pattern CODE_SNIPPET_PATTERN = Pattern.compile(
            "<(?:pre|code)[^>]*>.*?</(?:pre|code)>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    public SolvabilityContrastService(QuestionRepository questionRepository,
                                      QuestionCommentRepository questionCommentRepository) {
        this.questionRepository = questionRepository;
        this.questionCommentRepository = questionCommentRepository;
    }

    /**
     * 分析可解性对比
     * @param from 开始日期（可选）
     * @param to 结束日期（可选）
     * @return 特征对比数据
     */
    public SolvabilityContrastResponse analyze(LocalDate from, LocalDate to) {
        // 获取可解决问题和难解决问题
        List<QuestionEntity> allSolvable = questionRepository.findSolvableQuestions();
        List<QuestionEntity> allHard = questionRepository.findHardToSolveQuestions();
        
        // 批量加载标签，避免N+1查询
        loadTagsForQuestions(allSolvable);
        loadTagsForQuestions(allHard);
        
        // 应用时间过滤（如果提供）
        List<QuestionEntity> solvableQuestions = filterByDateRange(allSolvable, from, to);
        List<QuestionEntity> hardQuestions = filterByDateRange(allHard, from, to);
        
        // 进一步筛选：应用标准三（时效性）到可解决问题
        solvableQuestions = refineSolvableQuestions(solvableQuestions);
        
        // 进一步筛选：应用标准三（时间因素）到难解决问题
        hardQuestions = refineHardQuestions(hardQuestions);
        
        // 计算特征对比
        List<FeatureComparison> features = new ArrayList<>();
        
        // 特征1：问题清晰度和细节（字符数）
        features.add(calculateQuestionLengthFeature(solvableQuestions, hardQuestions));
        
        // 特征2：代码片段存在性（百分比）
        features.add(calculateCodeSnippetFeature(solvableQuestions, hardQuestions));
        
        // 特征3：提问者声誉（平均分数）
        features.add(calculateReputationFeature(solvableQuestions, hardQuestions));
        
        // 特征4（可选）：主题复杂性（百分比）
        features.add(calculateTopicComplexityFeature(solvableQuestions, hardQuestions));
        
        // 批量加载问题评论，用于评论频率统计
        loadQuestionCommentsForQuestions(solvableQuestions);
        loadQuestionCommentsForQuestions(hardQuestions);
        
        // 计算标签频率数据（前10个标签，排除java）
        List<TagFrequencyData> tagFrequencyData = calculateTagFrequencyData(solvableQuestions, hardQuestions);
        
        // 计算评论频率数据
        CommentFrequencyData commentFrequencyData = calculateCommentFrequencyData(solvableQuestions, hardQuestions);
        
        // 计算5个分布数据
        DistributionData codeSnippetRatioDistribution = calculateCodeSnippetRatioDistribution(solvableQuestions, hardQuestions);
        DistributionData tagCountDistribution = calculateTagCountDistribution(solvableQuestions, hardQuestions);
        DistributionData questionLengthDistribution = calculateQuestionLengthDistribution(solvableQuestions, hardQuestions);
        DistributionData reputationDistribution = calculateReputationDistribution(solvableQuestions, hardQuestions);
        DistributionData commentCountDistribution = calculateCommentCountDistribution(solvableQuestions, hardQuestions);
        
        // 计算声誉箱线图数据
        BoxPlotData reputationBoxPlotData = calculateReputationBoxPlotData(solvableQuestions, hardQuestions);
        
        return new SolvabilityContrastResponse(
                features, 
                tagFrequencyData, 
                commentFrequencyData,
                codeSnippetRatioDistribution,
                tagCountDistribution,
                questionLengthDistribution,
                reputationDistribution,
                commentCountDistribution,
                reputationBoxPlotData
        );
    }
    
    /**
     * 按日期范围过滤问题
     */
    private List<QuestionEntity> filterByDateRange(List<QuestionEntity> questions, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return questions;
        }
        
        ZoneId zoneId = ZoneId.systemDefault();
        return questions.stream()
                .filter(q -> {
                    if (q.getCreationDate() == null) {
                        return false;
                    }
                    LocalDate questionDate = q.getCreationDate().atZone(zoneId).toLocalDate();
                    if (from != null && questionDate.isBefore(from)) {
                        return false;
                    }
                    if (to != null && questionDate.isAfter(to)) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 精炼可解决问题
     * 新规则：问题未关闭，且有被接受答案，并且被接受答案的创建时间距离问题创建时间小于2小时
     */
    private List<QuestionEntity> refineSolvableQuestions(List<QuestionEntity> questions) {
        return questions.stream()
                .filter(q -> {
                    // 必须未关闭
                    if (q.getClosedDate() != null) {
                        return false;
                    }

                    // 必须有被接受答案
                    Long acceptedId = q.getAcceptedAnswerId();
                    if (acceptedId == null) {
                        return false;
                    }

                    if (q.getCreationDate() == null || q.getAnswers() == null || q.getAnswers().isEmpty()) {
                        return false;
                    }

                    // 在 answers 中找到被接受的答案
                    AnswerEntity accepted = q.getAnswers().stream()
                            .filter(a -> a.getAnswerId() != null && a.getAnswerId().equals(acceptedId))
                            .findFirst()
                            .orElse(null);
                    if (accepted == null || accepted.getCreationDate() == null) {
                        return false;
                    }

                    // 计算时间差 < 2 小时
                    Duration delta = Duration.between(q.getCreationDate(), accepted.getCreationDate());
                    return delta.toHours() < 2;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 精炼难解决问题
     * 新规则：问题未关闭，且满足：
     *  条件A：无被接受答案，但存在得分 > 6 的答案；
     *  或 条件B：无被接受答案且没有任何答案，并且距 2025-12-11 超过 240 天。
     */
    private List<QuestionEntity> refineHardQuestions(List<QuestionEntity> questions) {
        // 固定比较日期 2025-12-11，用于长期无人回答判断
        LocalDate referenceDate = LocalDate.of(2025, 12, 11);
        ZoneId zoneId = ZoneId.systemDefault();

        return questions.stream()
                .filter(q -> {
                    // 排除已关闭问题
                    if (q.getClosedDate() != null) {
                        return false;
                    }

                    // 必须没有被接受答案
                    if (q.getAcceptedAnswerId() != null) {
                        return false;
                    }

                    List<AnswerEntity> answers = q.getAnswers() != null ? q.getAnswers() : List.of();

                    // 条件A：有高分答案（score > 6）
                    boolean hasHighScoreAnswer = answers.stream()
                            .map(AnswerEntity::getScore)
                            .filter(score -> score != null)
                            .anyMatch(score -> score > 6);

                    if (hasHighScoreAnswer) {
                        return true;
                    }

                    // 条件B：长期无人回答（无任何答案，且创建时间距参考日期 > 240 天）
                    if (!answers.isEmpty()) {
                        return false;
                    }
                    if (q.getCreationDate() == null) {
                        return false;
                    }

                    LocalDate createdDate = q.getCreationDate().atZone(zoneId).toLocalDate();
                    // createdDate + 240 天 早于 referenceDate，说明间隔 > 240 天
                    return createdDate.plusDays(240).isBefore(referenceDate);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 计算问题长度特征（字符数）
     */
    private FeatureComparison calculateQuestionLengthFeature(
            List<QuestionEntity> solvable, List<QuestionEntity> hard) {
        
        double solvableAvg = solvable.stream()
                .mapToInt(this::calculateQuestionLength)
                .average()
                .orElse(0.0);
        
        double hardAvg = hard.stream()
                .mapToInt(this::calculateQuestionLength)
                .average()
                .orElse(0.0);
        
        return new FeatureComparison(
                "Avg Question Length",
                solvableAvg,
                hardAvg,
                "Characters"
        );
    }
    
    /**
     * 计算代码片段存在性特征（百分比）
     */
    private FeatureComparison calculateCodeSnippetFeature(
            List<QuestionEntity> solvable, List<QuestionEntity> hard) {
        
        long solvableWithCode = solvable.stream()
                .filter(this::hasCodeSnippet)
                .count();
        double solvablePercentage = solvable.isEmpty() ? 0.0 
                : (double) solvableWithCode / solvable.size();
        
        long hardWithCode = hard.stream()
                .filter(this::hasCodeSnippet)
                .count();
        double hardPercentage = hard.isEmpty() ? 0.0 
                : (double) hardWithCode / hard.size();
        
        return new FeatureComparison(
                "Code Snippet Presence",
                solvablePercentage,
                hardPercentage,
                "Percentage"
        );
    }
    
    /**
     * 计算提问者声誉特征（平均分数，log10变换后）
     */
    private FeatureComparison calculateReputationFeature(
            List<QuestionEntity> solvable, List<QuestionEntity> hard) {
        
        double solvableAvg = solvable.stream()
                .mapToInt(this::getOwnerReputation)
                .mapToDouble(this::log10Reputation)
                .average()
                .orElse(0.0);
        
        double hardAvg = hard.stream()
                .mapToInt(this::getOwnerReputation)
                .mapToDouble(this::log10Reputation)
                .average()
                .orElse(0.0);
        
        return new FeatureComparison(
                "Avg Asker Reputation (log10)",
                solvableAvg,
                hardAvg,
                "log10(Points)"
        );
    }
    
    /**
     * 计算主题复杂性特征（百分比）
     */
    private FeatureComparison calculateTopicComplexityFeature(
            List<QuestionEntity> solvable, List<QuestionEntity> hard) {
        
        long solvableComplex = solvable.stream()
                .filter(this::isComplexTopic)
                .count();
        double solvablePercentage = solvable.isEmpty() ? 0.0 
                : (double) solvableComplex / solvable.size();
        
        long hardComplex = hard.stream()
                .filter(this::isComplexTopic)
                .count();
        double hardPercentage = hard.isEmpty() ? 0.0 
                : (double) hardComplex / hard.size();
        
        return new FeatureComparison(
                "High Complexity Topic",
                solvablePercentage,
                hardPercentage,
                "Percentage"
        );
    }
    
    /**
     * 计算问题长度（字符数）
     */
    private int calculateQuestionLength(QuestionEntity question) {
        String body = question.getBody();
        if (body == null) {
            body = "";
        }
        // 移除HTML标签来计算纯文本长度
        String textOnly = body.replaceAll("<[^>]+>", " ");
        return textOnly.length();
    }
    
    /**
     * 检查问题是否包含代码片段
     */
    private boolean hasCodeSnippet(QuestionEntity question) {
        String body = question.getBody();
        if (body == null) {
            return false;
        }
        return CODE_SNIPPET_PATTERN.matcher(body).find();
    }
    
    /**
     * 获取提问者声誉
     */
    private int getOwnerReputation(QuestionEntity question) {
        if (question.getOwner() == null) {
            return 0;
        }
        Integer reputation = question.getOwner().getReputation();
        return reputation != null ? reputation : 0;
    }
    
    /**
     * 对声誉值进行log10变换
     * 处理边界情况：如果reputation <= 0，返回log10(1) = 0
     */
    private double log10Reputation(int reputation) {
        if (reputation <= 0) {
            return 0.0; // log10(1) = 0
        }
        return Math.log10(reputation);
    }
    
    /**
     * 检查是否为复杂主题
     */
    private boolean isComplexTopic(QuestionEntity question) {
        if (question.getTags() == null || question.getTags().isEmpty()) {
            return false;
        }
        return question.getTags().stream()
                .map(TagEntity::getName)
                .anyMatch(tag -> COMPLEX_TOPICS.contains(tag.toLowerCase()));
    }
    
    /**
     * 批量加载问题的标签，避免N+1查询
     */
    private void loadTagsForQuestions(List<QuestionEntity> questions) {
        if (questions.isEmpty()) {
            return;
        }
        
        List<Long> questionIds = questions.stream()
                .map(QuestionEntity::getQuestionId)
                .collect(Collectors.toList());
        
        // 批量查询标签
        List<Object[]> questionTags = questionRepository.findQuestionTagsByQuestionIds(questionIds);
        
        // 构建标签映射
        java.util.Map<Long, List<TagEntity>> tagsMap = new java.util.HashMap<>();
        for (Object[] row : questionTags) {
            Long qId = (Long) row[0];
            TagEntity tag = (TagEntity) row[1];
            tagsMap.computeIfAbsent(qId, k -> new ArrayList<>()).add(tag);
        }
        
        // 关联标签到问题
        for (QuestionEntity question : questions) {
            question.getTags().clear();
            question.getTags().addAll(tagsMap.getOrDefault(question.getQuestionId(), List.of()));
        }
    }
    
    /**
     * 批量加载问题的评论，避免N+1查询
     */
    private void loadQuestionCommentsForQuestions(List<QuestionEntity> questions) {
        if (questions.isEmpty()) {
            return;
        }
        
        List<Long> questionIds = questions.stream()
                .map(QuestionEntity::getQuestionId)
                .collect(Collectors.toList());
        
        // 批量查询评论
        List<QuestionCommentEntity> allComments = questionCommentRepository.findByQuestionQuestionIdIn(questionIds);
        
        // 构建评论映射
        java.util.Map<Long, List<QuestionCommentEntity>> commentsMap = allComments.stream()
                .collect(Collectors.groupingBy(c -> c.getQuestion().getQuestionId()));
        
        // 关联评论到问题
        for (QuestionEntity question : questions) {
            question.getQuestionComments().clear();
            question.getQuestionComments().addAll(
                    commentsMap.getOrDefault(question.getQuestionId(), List.of())
            );
        }
    }
    
    /**
     * 计算标签频率数据（前10个标签，排除java）
     */
    private List<TagFrequencyData> calculateTagFrequencyData(
            List<QuestionEntity> solvable, List<QuestionEntity> hard) {
        
        // 收集所有标签（排除java）
        java.util.Map<String, Integer> solvableTagCount = new java.util.HashMap<>();
        java.util.Map<String, Integer> hardTagCount = new java.util.HashMap<>();
        
        // 统计易解决问题中的标签
        for (QuestionEntity question : solvable) {
            if (question.getTags() != null) {
                for (TagEntity tag : question.getTags()) {
                    String tagName = tag.getName().toLowerCase();
                    if (!tagName.equals("java")) {
                        solvableTagCount.put(tagName, solvableTagCount.getOrDefault(tagName, 0) + 1);
                    }
                }
            }
        }
        
        // 统计难解决问题中的标签
        for (QuestionEntity question : hard) {
            if (question.getTags() != null) {
                for (TagEntity tag : question.getTags()) {
                    String tagName = tag.getName().toLowerCase();
                    if (!tagName.equals("java")) {
                        hardTagCount.put(tagName, hardTagCount.getOrDefault(tagName, 0) + 1);
                    }
                }
            }
        }
        
        // 获取所有唯一标签
        java.util.Set<String> allTags = new java.util.HashSet<>();
        allTags.addAll(solvableTagCount.keySet());
        allTags.addAll(hardTagCount.keySet());
        
        // 按总频率排序，选择前10个
        List<TagFrequencyData> tagData = allTags.stream()
                .map(tagName -> new TagFrequencyData(
                        tagName,
                        solvableTagCount.getOrDefault(tagName, 0),
                        hardTagCount.getOrDefault(tagName, 0)
                ))
                .sorted((a, b) -> Integer.compare(
                        (b.solvable_count() + b.hard_count()),
                        (a.solvable_count() + a.hard_count())
                ))
                .limit(10)
                .collect(Collectors.toList());
        
        return tagData;
    }
    
    /**
     * 计算评论频率数据
     */
    private CommentFrequencyData calculateCommentFrequencyData(
            List<QuestionEntity> solvable, List<QuestionEntity> hard) {
        
        // 统计易解决问题中有评论的数量
        long solvableWithComments = solvable.stream()
                .filter(q -> q.getQuestionComments() != null && !q.getQuestionComments().isEmpty())
                .count();
        
        // 统计难解决问题中有评论的数量
        long hardWithComments = hard.stream()
                .filter(q -> q.getQuestionComments() != null && !q.getQuestionComments().isEmpty())
                .count();
        
        return new CommentFrequencyData(
                (int) solvableWithComments,
                solvable.size(),
                (int) hardWithComments,
                hard.size()
        );
    }
    
    /**
     * 计算代码片段比率分布
     */
    private DistributionData calculateCodeSnippetRatioDistribution(
            List<QuestionEntity> solvable, List<QuestionEntity> hard) {
        
        // 定义10个区间：0-0.1, 0.1-0.2, ..., 0.9-1.0
        int numBins = 10;
        List<String> bins = new ArrayList<>();
        for (int i = 0; i < numBins; i++) {
            double start = i * 0.1;
            double end = (i + 1) * 0.1;
            bins.add(String.format("%.1f-%.1f", start, end));
        }
        
        // 初始化计数数组
        int[] solvableCounts = new int[numBins];
        int[] hardCounts = new int[numBins];
        
        // 统计易解决问题
        for (QuestionEntity question : solvable) {
            double ratio = calculateCodeSnippetRatio(question);
            int binIndex = Math.min((int) (ratio * numBins), numBins - 1);
            solvableCounts[binIndex]++;
        }
        
        // 统计难解决问题
        for (QuestionEntity question : hard) {
            double ratio = calculateCodeSnippetRatio(question);
            int binIndex = Math.min((int) (ratio * numBins), numBins - 1);
            hardCounts[binIndex]++;
        }
        
        // 转换为频率百分比
        List<Double> solvableFreq = new ArrayList<>();
        List<Double> hardFreq = new ArrayList<>();
        double solvableTotal = solvable.isEmpty() ? 1 : solvable.size();
        double hardTotal = hard.isEmpty() ? 1 : hard.size();
        
        for (int i = 0; i < numBins; i++) {
            solvableFreq.add((solvableCounts[i] / solvableTotal) * 100.0);
            hardFreq.add((hardCounts[i] / hardTotal) * 100.0);
        }
        
        return new DistributionData(bins, solvableFreq, hardFreq);
    }
    
    /**
     * 计算代码片段比率
     */
    private double calculateCodeSnippetRatio(QuestionEntity question) {
        String body = question.getBody();
        if (body == null || body.isEmpty()) {
            return 0.0;
        }
        
        // 提取代码片段
        java.util.regex.Matcher matcher = CODE_SNIPPET_PATTERN.matcher(body);
        int codeSnippetLength = 0;
        while (matcher.find()) {
            codeSnippetLength += matcher.group().length();
        }
        
        // 计算总字符数（去除HTML标签）
        String textOnly = body.replaceAll("<[^>]+>", " ");
        int totalLength = textOnly.length();
        
        if (totalLength == 0) {
            return 0.0;
        }
        
        return Math.min(1.0, (double) codeSnippetLength / totalLength);
    }
    
    /**
     * 计算标签数分布
     */
    private DistributionData calculateTagCountDistribution(
            List<QuestionEntity> solvable, List<QuestionEntity> hard) {
        
        // 定义标签数区间：0, 1, 2, 3-5, 6-10, 10+
        List<String> bins = List.of("0", "1", "2", "3-5", "6-10", "10+");
        int[] solvableCounts = new int[6];
        int[] hardCounts = new int[6];
        
        // 统计易解决问题
        for (QuestionEntity question : solvable) {
            int tagCount = question.getTags() != null ? question.getTags().size() : 0;
            int binIndex = getTagCountBinIndex(tagCount);
            solvableCounts[binIndex]++;
        }
        
        // 统计难解决问题
        for (QuestionEntity question : hard) {
            int tagCount = question.getTags() != null ? question.getTags().size() : 0;
            int binIndex = getTagCountBinIndex(tagCount);
            hardCounts[binIndex]++;
        }
        
        // 转换为频率百分比
        List<Double> solvableFreq = new ArrayList<>();
        List<Double> hardFreq = new ArrayList<>();
        double solvableTotal = solvable.isEmpty() ? 1 : solvable.size();
        double hardTotal = hard.isEmpty() ? 1 : hard.size();
        
        for (int i = 0; i < bins.size(); i++) {
            solvableFreq.add((solvableCounts[i] / solvableTotal) * 100.0);
            hardFreq.add((hardCounts[i] / hardTotal) * 100.0);
        }
        
        return new DistributionData(bins, solvableFreq, hardFreq);
    }
    
    /**
     * 获取标签数的区间索引
     */
    private int getTagCountBinIndex(int tagCount) {
        if (tagCount == 0) return 0;
        if (tagCount == 1) return 1;
        if (tagCount == 2) return 2;
        if (tagCount >= 3 && tagCount <= 5) return 3;
        if (tagCount >= 6 && tagCount <= 10) return 4;
        return 5; // 10+
    }
    
    /**
     * 计算问题长度分布
     */
    private DistributionData calculateQuestionLengthDistribution(
            List<QuestionEntity> solvable, List<QuestionEntity> hard) {
        
        // 定义长度区间：0-500, 500-1000, 1000-2000, 2000-5000, 5000+
        List<String> bins = List.of("0-500", "500-1000", "1000-2000", "2000-5000", "5000+");
        int[] solvableCounts = new int[5];
        int[] hardCounts = new int[5];
        
        // 统计易解决问题
        for (QuestionEntity question : solvable) {
            int length = calculateQuestionLength(question);
            int binIndex = getLengthBinIndex(length);
            solvableCounts[binIndex]++;
        }
        
        // 统计难解决问题
        for (QuestionEntity question : hard) {
            int length = calculateQuestionLength(question);
            int binIndex = getLengthBinIndex(length);
            hardCounts[binIndex]++;
        }
        
        // 转换为频率百分比
        List<Double> solvableFreq = new ArrayList<>();
        List<Double> hardFreq = new ArrayList<>();
        double solvableTotal = solvable.isEmpty() ? 1 : solvable.size();
        double hardTotal = hard.isEmpty() ? 1 : hard.size();
        
        for (int i = 0; i < bins.size(); i++) {
            solvableFreq.add((solvableCounts[i] / solvableTotal) * 100.0);
            hardFreq.add((hardCounts[i] / hardTotal) * 100.0);
        }
        
        return new DistributionData(bins, solvableFreq, hardFreq);
    }
    
    /**
     * 获取长度的区间索引
     */
    private int getLengthBinIndex(int length) {
        if (length < 500) return 0;
        if (length < 1000) return 1;
        if (length < 2000) return 2;
        if (length < 5000) return 3;
        return 4; // 5000+
    }
    
    /**
     * 计算提问者声誉分布（log10变换后）
     */
    private DistributionData calculateReputationDistribution(
            List<QuestionEntity> solvable, List<QuestionEntity> hard) {
        
        // 定义log10变换后的声誉区间：0-1, 1-2, 2-3, 3-4, 4+
        // 对应原始值大致为：1-10, 10-100, 100-1000, 1000-10000, 10000+
        List<String> bins = List.of("0-1", "1-2", "2-3", "3-4", "4+");
        int[] solvableCounts = new int[5];
        int[] hardCounts = new int[5];
        
        // 统计易解决问题
        for (QuestionEntity question : solvable) {
            int reputation = getOwnerReputation(question);
            double logReputation = log10Reputation(reputation);
            int binIndex = getLogReputationBinIndex(logReputation);
            solvableCounts[binIndex]++;
        }
        
        // 统计难解决问题
        for (QuestionEntity question : hard) {
            int reputation = getOwnerReputation(question);
            double logReputation = log10Reputation(reputation);
            int binIndex = getLogReputationBinIndex(logReputation);
            hardCounts[binIndex]++;
        }
        
        // 转换为频率百分比
        List<Double> solvableFreq = new ArrayList<>();
        List<Double> hardFreq = new ArrayList<>();
        double solvableTotal = solvable.isEmpty() ? 1 : solvable.size();
        double hardTotal = hard.isEmpty() ? 1 : hard.size();
        
        for (int i = 0; i < bins.size(); i++) {
            solvableFreq.add((solvableCounts[i] / solvableTotal) * 100.0);
            hardFreq.add((hardCounts[i] / hardTotal) * 100.0);
        }
        
        return new DistributionData(bins, solvableFreq, hardFreq);
    }
    
    /**
     * 获取log10变换后声誉的区间索引
     */
    private int getLogReputationBinIndex(double logReputation) {
        if (logReputation < 1.0) return 0;
        if (logReputation < 2.0) return 1;
        if (logReputation < 3.0) return 2;
        if (logReputation < 4.0) return 3;
        return 4; // 4+
    }
    
    /**
     * 计算评论数量分布
     */
    private DistributionData calculateCommentCountDistribution(
            List<QuestionEntity> solvable, List<QuestionEntity> hard) {
        
        // 定义评论数区间：0, 1, 2-5, 6-10, 10+
        List<String> bins = List.of("0", "1", "2-5", "6-10", "10+");
        int[] solvableCounts = new int[5];
        int[] hardCounts = new int[5];
        
        // 统计易解决问题
        for (QuestionEntity question : solvable) {
            int commentCount = question.getQuestionComments() != null ? question.getQuestionComments().size() : 0;
            int binIndex = getCommentCountBinIndex(commentCount);
            solvableCounts[binIndex]++;
        }
        
        // 统计难解决问题
        for (QuestionEntity question : hard) {
            int commentCount = question.getQuestionComments() != null ? question.getQuestionComments().size() : 0;
            int binIndex = getCommentCountBinIndex(commentCount);
            hardCounts[binIndex]++;
        }
        
        // 转换为频率百分比
        List<Double> solvableFreq = new ArrayList<>();
        List<Double> hardFreq = new ArrayList<>();
        double solvableTotal = solvable.isEmpty() ? 1 : solvable.size();
        double hardTotal = hard.isEmpty() ? 1 : hard.size();
        
        for (int i = 0; i < bins.size(); i++) {
            solvableFreq.add((solvableCounts[i] / solvableTotal) * 100.0);
            hardFreq.add((hardCounts[i] / hardTotal) * 100.0);
        }
        
        return new DistributionData(bins, solvableFreq, hardFreq);
    }
    
    /**
     * 获取评论数的区间索引
     */
    private int getCommentCountBinIndex(int commentCount) {
        if (commentCount == 0) return 0;
        if (commentCount == 1) return 1;
        if (commentCount >= 2 && commentCount <= 5) return 2;
        if (commentCount >= 6 && commentCount <= 10) return 3;
        return 4; // 10+
    }
    
    /**
     * 计算声誉箱线图数据（log10变换后）
     */
    private BoxPlotData calculateReputationBoxPlotData(
            List<QuestionEntity> solvable, List<QuestionEntity> hard) {
        
        // 收集易解决组的声誉值（log10变换后）
        List<Double> solvableReputations = solvable.stream()
                .mapToInt(this::getOwnerReputation)
                .mapToDouble(this::log10Reputation)
                .boxed()
                .collect(Collectors.toList());
        
        // 收集难解决组的声誉值（log10变换后）
        List<Double> hardReputations = hard.stream()
                .mapToInt(this::getOwnerReputation)
                .mapToDouble(this::log10Reputation)
                .boxed()
                .collect(Collectors.toList());
        
        BoxPlotStats solvableStats = calculateBoxPlotStatsDouble(solvableReputations);
        BoxPlotStats hardStats = calculateBoxPlotStatsDouble(hardReputations);
        
        return new BoxPlotData(solvableStats, hardStats);
    }
    
    /**
     * 计算箱线图统计数据（Integer版本）
     */
    private BoxPlotStats calculateBoxPlotStats(List<Integer> values) {
        if (values.isEmpty()) {
            return new BoxPlotStats(0, 0, 0, 0, 0, List.of());
        }
        
        // 排序
        List<Integer> sorted = new ArrayList<>(values);
        sorted.sort(Integer::compareTo);
        
        // 计算百分位数
        double q1 = calculatePercentile(sorted, 0.25);
        double median = calculatePercentile(sorted, 0.50);
        double q3 = calculatePercentile(sorted, 0.75);
        
        double min = sorted.get(0);
        double max = sorted.get(sorted.size() - 1);
        
        // 检测异常值
        List<Double> outliers = detectOutliers(sorted, q1, q3);
        
        return new BoxPlotStats(min, q1, median, q3, max, outliers);
    }
    
    /**
     * 计算箱线图统计数据（Double版本，用于log10变换后的值）
     */
    private BoxPlotStats calculateBoxPlotStatsDouble(List<Double> values) {
        if (values.isEmpty()) {
            return new BoxPlotStats(0, 0, 0, 0, 0, List.of());
        }
        
        // 排序
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);
        
        // 计算百分位数
        double q1 = calculatePercentileDouble(sorted, 0.25);
        double median = calculatePercentileDouble(sorted, 0.50);
        double q3 = calculatePercentileDouble(sorted, 0.75);
        
        double min = sorted.get(0);
        double max = sorted.get(sorted.size() - 1);
        
        // 检测异常值
        List<Double> outliers = detectOutliersDouble(sorted, q1, q3);
        
        return new BoxPlotStats(min, q1, median, q3, max, outliers);
    }
    
    /**
     * 计算百分位数（Integer版本）
     */
    private double calculatePercentile(List<Integer> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) {
            return 0.0;
        }
        
        if (sortedValues.size() == 1) {
            return sortedValues.get(0);
        }
        
        double index = percentile * (sortedValues.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        
        if (lower == upper) {
            return sortedValues.get(lower);
        }
        
        double weight = index - lower;
        return sortedValues.get(lower) * (1 - weight) + sortedValues.get(upper) * weight;
    }
    
    /**
     * 计算百分位数（Double版本）
     */
    private double calculatePercentileDouble(List<Double> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) {
            return 0.0;
        }
        
        if (sortedValues.size() == 1) {
            return sortedValues.get(0);
        }
        
        double index = percentile * (sortedValues.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        
        if (lower == upper) {
            return sortedValues.get(lower);
        }
        
        double weight = index - lower;
        return sortedValues.get(lower) * (1 - weight) + sortedValues.get(upper) * weight;
    }
    
    /**
     * 检测异常值（使用IQR方法，Integer版本）
     */
    private List<Double> detectOutliers(List<Integer> values, double q1, double q3) {
        double iqr = q3 - q1;
        double lowerBound = q1 - 0.5 * iqr;
        double upperBound = q3 + 0.5 * iqr;
        
        return values.stream()
                .mapToDouble(Integer::doubleValue)
                .filter(v -> v < lowerBound || v > upperBound)
                .boxed()
                .collect(Collectors.toList());
    }
    
    /**
     * 检测异常值（使用IQR方法，Double版本）
     */
    private List<Double> detectOutliersDouble(List<Double> values, double q1, double q3) {
        double iqr = q3 - q1;
        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;
        
        return values.stream()
                .filter(v -> v < lowerBound || v > upperBound)
                .collect(Collectors.toList());
    }
}
