package cs209a.finalproject_demo.service;

import cs209a.finalproject_demo.dto.SolvabilityContrastResponse;
import cs209a.finalproject_demo.dto.SolvabilityContrastResponse.CommentFrequencyData;
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
import java.time.Instant;
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
        
        return new SolvabilityContrastResponse(features, tagFrequencyData, commentFrequencyData);
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
     * 精炼可解决问题：应用标准三（时效性）
     * 保留有被接受答案的，或有高赞答案的，或首次回答时间 < 1小时的问题
     */
    private List<QuestionEntity> refineSolvableQuestions(List<QuestionEntity> questions) {
        return questions.stream()
                .filter(q -> {
                    // 标准一：有被接受的答案（已在查询中）
                    if (q.getAcceptedAnswerId() != null) {
                        return true;
                    }
                    
                    // 标准二：有高赞答案（score >= 5，已在查询中）
                    boolean hasHighScoreAnswer = q.getAnswers().stream()
                            .anyMatch(a -> a.getScore() != null && a.getScore() >= 5);
                    if (hasHighScoreAnswer) {
                        return true;
                    }
                    
                    // 标准三：首次回答时间 < 1小时
                    if (q.getCreationDate() != null && !q.getAnswers().isEmpty()) {
                        Instant firstAnswerTime = q.getAnswers().stream()
                                .map(AnswerEntity::getCreationDate)
                                .filter(date -> date != null)
                                .min(Instant::compareTo)
                                .orElse(null);
                        
                        if (firstAnswerTime != null) {
                            Duration timeToFirstAnswer = Duration.between(q.getCreationDate(), firstAnswerTime);
                            if (timeToFirstAnswer.toHours() < 1) {
                                return true;
                            }
                        }
                    }
                    
                    return false;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 精炼难解决问题：应用标准三（时间因素）
     * 保留无被接受答案且（答案数 <= 1 或（超过30天且最高答案分数 < 5））的问题
     */
    private List<QuestionEntity> refineHardQuestions(List<QuestionEntity> questions) {
        Instant now = Instant.now();
        return questions.stream()
                .filter(q -> {
                    // 标准一：无被接受的答案（已在查询中）
                    if (q.getAcceptedAnswerId() != null) {
                        return false;
                    }
                    
                    // 标准二：答案数量很少（已在查询中）
                    if (q.getAnswerCount() == null || q.getAnswerCount() <= 1) {
                        return true;
                    }
                    
                    // 标准三：超过30天且最高答案分数 < 5
                    if (q.getCreationDate() != null) {
                        Duration age = Duration.between(q.getCreationDate(), now);
                        if (age.toDays() > 30) {
                            int maxAnswerScore = q.getAnswers().stream()
                                    .map(AnswerEntity::getScore)
                                    .filter(score -> score != null)
                                    .mapToInt(Integer::intValue)
                                    .max()
                                    .orElse(0);
                            
                            if (maxAnswerScore < 5) {
                                return true;
                            }
                        }
                    }
                    
                    return false;
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
     * 计算提问者声誉特征（平均分数）
     */
    private FeatureComparison calculateReputationFeature(
            List<QuestionEntity> solvable, List<QuestionEntity> hard) {
        
        double solvableAvg = solvable.stream()
                .mapToInt(this::getOwnerReputation)
                .average()
                .orElse(0.0);
        
        double hardAvg = hard.stream()
                .mapToInt(this::getOwnerReputation)
                .average()
                .orElse(0.0);
        
        return new FeatureComparison(
                "Avg Asker Reputation",
                solvableAvg,
                hardAvg,
                "Points"
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
}
