package cs209a.finalproject_demo.service;

import cs209a.finalproject_demo.dto.MultithreadingPitfallResponse;
import cs209a.finalproject_demo.dto.MultithreadingPitfallResponse.CategoryGroup;
import cs209a.finalproject_demo.dto.MultithreadingPitfallResponse.PitfallDetail;
import cs209a.finalproject_demo.entity.AnswerEntity;
import cs209a.finalproject_demo.entity.QuestionEntity;
import cs209a.finalproject_demo.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 多线程问题分析服务
 * 基于文本分析从数据中发现真实的多线程陷阱
 * 完全废除预定义匹配模式，改为动态发现
 * 所有文本匹配使用正则表达式
 */
@Service
public class MultithreadingInsightService {

    private final QuestionRepository questionRepository;
    
    // Java异常类名模式（用于识别异常类型）
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile(
            "\\b(java\\.(lang|util|io|nio|concurrent)\\.)?[A-Z]\\w*Exception\\b"
    );
    
    // 多线程相关关键词模式（使用正则表达式）
    private static final Map<String, Pattern> MULTITHREADING_KEYWORD_PATTERNS = new LinkedHashMap<>();
    static {
        MULTITHREADING_KEYWORD_PATTERNS.put("deadlock", Pattern.compile("\\bdeadlock\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("race condition", Pattern.compile("\\brace\\s+condition\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("data race", Pattern.compile("\\bdata\\s+race\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("thread safe", Pattern.compile("\\bthread\\s+safe\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("thread safety", Pattern.compile("\\bthread\\s+safety\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("synchronized", Pattern.compile("\\bsynchronized\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("volatile", Pattern.compile("\\bvolatile\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("atomic", Pattern.compile("\\batomic\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("lock", Pattern.compile("\\block\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("monitor", Pattern.compile("\\bmonitor\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("mutex", Pattern.compile("\\bmutex\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("concurrent", Pattern.compile("\\bconcurrent\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("parallel", Pattern.compile("\\bparallel\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("thread pool", Pattern.compile("\\bthread\\s+pool\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("executor", Pattern.compile("\\bexecutor\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("executorservice", Pattern.compile("\\bexecutorservice\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("blocking", Pattern.compile("\\bblocking\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("blocked", Pattern.compile("\\bblocked\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("hanging", Pattern.compile("\\bhanging\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("freezing", Pattern.compile("\\bfreezing\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("stuck", Pattern.compile("\\bstuck\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("waiting", Pattern.compile("\\bwaiting\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("visibility", Pattern.compile("\\bvisibility\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("happens-before", Pattern.compile("\\bhappens-before\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("memory barrier", Pattern.compile("\\bmemory\\s+barrier\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("stale data", Pattern.compile("\\bstale\\s+data\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("concurrent modification", Pattern.compile("\\bconcurrent\\s+modification\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("thread contention", Pattern.compile("\\bthread\\s+contention\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("context switch", Pattern.compile("\\bcontext\\s+switch\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("livelock", Pattern.compile("\\blivelock\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("starvation", Pattern.compile("\\bstarvation\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("reentrant", Pattern.compile("\\breentrant\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("semaphore", Pattern.compile("\\bsemaphore\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("countdownlatch", Pattern.compile("\\bcountdownlatch\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("cyclicbarrier", Pattern.compile("\\bcyclicbarrier\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("phaser", Pattern.compile("\\bphaser\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("completablefuture", Pattern.compile("\\bcompletablefuture\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("future", Pattern.compile("\\bfuture\\b", Pattern.CASE_INSENSITIVE));
    }
    
    // 错误描述模式（使用正则表达式）
    private static final Map<String, Pattern> ERROR_PATTERN_PATTERNS = new LinkedHashMap<>();
    static {
        ERROR_PATTERN_PATTERNS.put("not working", Pattern.compile("\\bnot\\s+working\\b", Pattern.CASE_INSENSITIVE));
        ERROR_PATTERN_PATTERNS.put("doesn't work", Pattern.compile("\\bdoesn'?t\\s+work\\b", Pattern.CASE_INSENSITIVE));
        ERROR_PATTERN_PATTERNS.put("not thread safe", Pattern.compile("\\bnot\\s+thread\\s+safe\\b", Pattern.CASE_INSENSITIVE));
        ERROR_PATTERN_PATTERNS.put("thread interference", Pattern.compile("\\bthread\\s+interference\\b", Pattern.CASE_INSENSITIVE));
        ERROR_PATTERN_PATTERNS.put("inconsistent", Pattern.compile("\\binconsistent\\b", Pattern.CASE_INSENSITIVE));
        ERROR_PATTERN_PATTERNS.put("unexpected behavior", Pattern.compile("\\bunexpected\\s+behavior\\b", Pattern.CASE_INSENSITIVE));
        ERROR_PATTERN_PATTERNS.put("random", Pattern.compile("\\brandom\\b", Pattern.CASE_INSENSITIVE));
        ERROR_PATTERN_PATTERNS.put("sometimes", Pattern.compile("\\bsometimes\\b", Pattern.CASE_INSENSITIVE));
        ERROR_PATTERN_PATTERNS.put("intermittent", Pattern.compile("\\bintermittent\\b", Pattern.CASE_INSENSITIVE));
        ERROR_PATTERN_PATTERNS.put("performance issue", Pattern.compile("\\bperformance\\s+issue\\b", Pattern.CASE_INSENSITIVE));
        ERROR_PATTERN_PATTERNS.put("slow", Pattern.compile("\\bslow\\b", Pattern.CASE_INSENSITIVE));
        ERROR_PATTERN_PATTERNS.put("bottleneck", Pattern.compile("\\bbottleneck\\b", Pattern.CASE_INSENSITIVE));
        ERROR_PATTERN_PATTERNS.put("resource leak", Pattern.compile("\\bresource\\s+leak\\b", Pattern.CASE_INSENSITIVE));
        ERROR_PATTERN_PATTERNS.put("memory leak", Pattern.compile("\\bmemory\\s+leak\\b", Pattern.CASE_INSENSITIVE));
    }

    public MultithreadingInsightService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    /**
     * 分析多线程常见陷阱
     * 完全基于文本分析，动态发现陷阱类型
     * topN 参数控制返回的大类数量
     */
    public MultithreadingPitfallResponse analyze(int topN) {
        // 数据库初筛：获取潜在的多线程相关问题
        List<QuestionEntity> candidateQuestions = questionRepository.findPotentialMultithreadingQuestions();
        
        // 收集所有文本内容
        List<QuestionTextContent> textContents = new ArrayList<>();
        for (QuestionEntity question : candidateQuestions) {
            String questionText = filterCodeSnippets(
                    (question.getTitle() != null ? question.getTitle() : "") + " " +
                    (question.getBody() != null ? question.getBody() : "")
            );
            
            String answerText = "";
            Optional<AnswerEntity> acceptedAnswer = findAcceptedAnswer(question);
            if (acceptedAnswer.isPresent()) {
                answerText = filterCodeSnippets(
                        acceptedAnswer.get().getBody() != null ? acceptedAnswer.get().getBody() : ""
                );
            }
            
            textContents.add(new QuestionTextContent(
                    question.getQuestionId(),
                    questionText.toLowerCase(),
                    answerText.toLowerCase()
            ));
        }
        
        // 文本分析：发现陷阱
        Map<String, PitfallInfo> discoveredPitfalls = discoverPitfalls(textContents);
        
        // 按大类聚合
        Map<String, List<PitfallDetail>> categoryMap = new LinkedHashMap<>();
        
        for (Map.Entry<String, PitfallInfo> entry : discoveredPitfalls.entrySet()) {
            String pitfallLabel = entry.getKey();
            PitfallInfo info = entry.getValue();
            String category = categorizePitfall(pitfallLabel, info);
            
            PitfallDetail detail = new PitfallDetail(
                    pitfallLabel,
                    info.count,
                    info.examples.stream().limit(3).collect(Collectors.toList())
            );
            
            categoryMap.computeIfAbsent(category, k -> new ArrayList<>()).add(detail);
        }
        
        // 构建响应，按大类总数量排序，限制topN个大类
        List<CategoryGroup> categories = categoryMap.entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    List<PitfallDetail> pitfalls = entry.getValue();
                    long totalCount = pitfalls.stream().mapToLong(PitfallDetail::count).sum();
                    
                    // 对细项按数量排序
                    List<PitfallDetail> sortedPitfalls = pitfalls.stream()
                            .sorted(Comparator.comparingLong(PitfallDetail::count).reversed())
                            .collect(Collectors.toList());
                    
                    return new CategoryGroup(category, totalCount, sortedPitfalls);
                })
                .sorted(Comparator.comparingLong(CategoryGroup::totalCount).reversed())
                .limit(topN)
                .collect(Collectors.toList());

        return new MultithreadingPitfallResponse(categories);
    }

    /**
     * 基于文本分析发现陷阱
     */
    private Map<String, PitfallInfo> discoverPitfalls(List<QuestionTextContent> textContents) {
        Map<String, PitfallInfo> pitfalls = new LinkedHashMap<>();
        
        // 1. 提取异常类型
        Map<String, List<Long>> exceptionMap = extractExceptionTypes(textContents);
        for (Map.Entry<String, List<Long>> entry : exceptionMap.entrySet()) {
            if (entry.getValue().size() >= 2) { // 至少出现2次
                pitfalls.put(entry.getKey(), new PitfallInfo(entry.getValue().size(), entry.getValue()));
            }
        }
        
        // 2. 提取关键词
        Map<String, List<Long>> keywordMap = extractKeywords(textContents);
        for (Map.Entry<String, List<Long>> entry : keywordMap.entrySet()) {
            if (entry.getValue().size() >= 3) { // 至少出现3次
                String normalizedKey = normalizeKeyword(entry.getKey());
                if (!pitfalls.containsKey(normalizedKey)) {
                    pitfalls.put(normalizedKey, new PitfallInfo(entry.getValue().size(), entry.getValue()));
                }
            }
        }
        
        // 3. 识别错误模式
        Map<String, List<Long>> patternMap = identifyErrorPatterns(textContents);
        for (Map.Entry<String, List<Long>> entry : patternMap.entrySet()) {
            if (entry.getValue().size() >= 2) { // 至少出现2次
                String normalizedKey = normalizeKeyword(entry.getKey());
                if (!pitfalls.containsKey(normalizedKey)) {
                    pitfalls.put(normalizedKey, new PitfallInfo(entry.getValue().size(), entry.getValue()));
                }
            }
        }
        
        return pitfalls;
    }

    /**
     * 提取异常类型及其频率
     */
    private Map<String, List<Long>> extractExceptionTypes(List<QuestionTextContent> textContents) {
        Map<String, List<Long>> exceptionMap = new LinkedHashMap<>();
        
        for (QuestionTextContent content : textContents) {
            String combinedText = content.questionText + " " + content.answerText;
            
            java.util.regex.Matcher matcher = EXCEPTION_PATTERN.matcher(combinedText);
            Set<String> foundExceptions = new HashSet<>();
            
            while (matcher.find()) {
                String exception = matcher.group();
                // 简化异常名称（只保留类名，去掉包名）
                String simpleName = exception.contains(".") 
                        ? exception.substring(exception.lastIndexOf('.') + 1)
                        : exception;
                foundExceptions.add(simpleName);
            }
            
            for (String exception : foundExceptions) {
                exceptionMap.computeIfAbsent(exception, k -> new ArrayList<>())
                        .add(content.questionId);
            }
        }
        
        return exceptionMap;
    }

    /**
     * 提取关键词及其频率（使用正则表达式）
     */
    private Map<String, List<Long>> extractKeywords(List<QuestionTextContent> textContents) {
        Map<String, List<Long>> keywordMap = new LinkedHashMap<>();
        
        for (QuestionTextContent content : textContents) {
            String combinedText = content.questionText + " " + content.answerText;
            
            for (Map.Entry<String, Pattern> entry : MULTITHREADING_KEYWORD_PATTERNS.entrySet()) {
                String keyword = entry.getKey();
                Pattern pattern = entry.getValue();
                if (pattern.matcher(combinedText).find()) {
                    keywordMap.computeIfAbsent(keyword, k -> new ArrayList<>())
                            .add(content.questionId);
                }
            }
        }
        
        return keywordMap;
    }

    /**
     * 识别错误描述模式（使用正则表达式）
     */
    private Map<String, List<Long>> identifyErrorPatterns(List<QuestionTextContent> textContents) {
        Map<String, List<Long>> patternMap = new LinkedHashMap<>();
        
        for (QuestionTextContent content : textContents) {
            String combinedText = content.questionText + " " + content.answerText;
            
            for (Map.Entry<String, Pattern> entry : ERROR_PATTERN_PATTERNS.entrySet()) {
                String pattern = entry.getKey();
                Pattern regex = entry.getValue();
                if (regex.matcher(combinedText).find()) {
                    patternMap.computeIfAbsent(pattern, k -> new ArrayList<>())
                            .add(content.questionId);
                }
            }
        }
        
        return patternMap;
    }

    /**
     * 规范化关键词（首字母大写，处理复合词）
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return keyword;
        }
        
        // 处理复合词（如 "race condition" -> "Race Condition"）
        String[] words = keyword.split("\\s+");
        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) normalized.append(" ");
            if (!words[i].isEmpty()) {
                normalized.append(Character.toUpperCase(words[i].charAt(0)))
                          .append(words[i].substring(1).toLowerCase());
            }
        }
        
        return normalized.toString();
    }

    /**
     * 将发现的陷阱归类到大类（使用正则表达式匹配）
     */
    private String categorizePitfall(String pitfallLabel, @SuppressWarnings("unused") PitfallInfo info) {
        String lowerLabel = pitfallLabel.toLowerCase();
        
        // Synchronization Issues - 使用正则表达式
        Pattern syncPattern = Pattern.compile("\\b(deadlock|lock|monitor|synchronized|mutex|illegalmonitor|illegalthreadstate)\\b", Pattern.CASE_INSENSITIVE);
        if (syncPattern.matcher(lowerLabel).find()) {
            return "Synchronization Issues";
        }
        
        // Memory Consistency - 使用正则表达式
        Pattern memoryPattern = Pattern.compile("\\b(race\\s+condition|data\\s+race|visibility|volatile|happens-before|memory\\s+barrier|stale)\\b", Pattern.CASE_INSENSITIVE);
        if (memoryPattern.matcher(lowerLabel).find()) {
            return "Memory Consistency";
        }
        
        // Concurrent Data Structures - 使用正则表达式
        Pattern collectionPattern = Pattern.compile("\\b(concurrentmodification|concurrent\\s+modification|collection|arraylist|hashmap|hashset)\\b", Pattern.CASE_INSENSITIVE);
        if (collectionPattern.matcher(lowerLabel).find()) {
            return "Concurrent Data Structures";
        }
        
        // Thread Pool & Executors - 使用正则表达式
        Pattern executorPattern = Pattern.compile("\\b(thread\\s+pool|executor|rejectedexecution|threadpoolexecutor)\\b", Pattern.CASE_INSENSITIVE);
        if (executorPattern.matcher(lowerLabel).find()) {
            return "Thread Pool & Executors";
        }
        
        // Performance Issues - 使用正则表达式
        Pattern performancePattern = Pattern.compile("\\b(performance|resource|bottleneck|slow|contention|context\\s+switch)\\b", Pattern.CASE_INSENSITIVE);
        if (performancePattern.matcher(lowerLabel).find()) {
            return "Performance Issues";
        }
        
        // Exceptions (其他异常类型)
        if (pitfallLabel.endsWith("Exception") || pitfallLabel.endsWith("Error")) {
            return "Exceptions";
        }
        
        // Others
        return "Others";
    }

    /**
     * 过滤代码片段，移除 <pre><code>...</code></pre> 块中的内容
     */
    private String filterCodeSnippets(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // 移除 <code> 和 <pre> 标签及其内容
        Pattern codePattern = Pattern.compile(
                "<(?:code|pre)[^>]*>.*?</(?:code|pre)>", 
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        String filtered = codePattern.matcher(text).replaceAll(" ");
        
        // 移除其他HTML标签，只保留文本内容
        filtered = filtered.replaceAll("<[^>]+>", " ");
        
        // 移除HTML实体编码
        filtered = filtered.replace("&lt;", " ")
                           .replace("&gt;", " ")
                           .replace("&amp;", " ")
                           .replace("&quot;", " ")
                           .replace("&nbsp;", " ");
        
        return filtered;
    }

    /**
     * 从问题中查找被接受的答案
     */
    private Optional<AnswerEntity> findAcceptedAnswer(QuestionEntity question) {
        if (question.getAcceptedAnswerId() == null) {
            return Optional.empty();
        }
        
        return question.getAnswers().stream()
                .filter(answer -> answer.getAnswerId().equals(question.getAcceptedAnswerId()))
                .findFirst();
    }

    /**
     * 问题文本内容记录
     */
    private static class QuestionTextContent {
        final Long questionId;
        final String questionText;
        final String answerText;
        
        QuestionTextContent(Long questionId, String questionText, String answerText) {
            this.questionId = questionId;
            this.questionText = questionText;
            this.answerText = answerText;
        }
    }

    /**
     * 陷阱信息
     */
    private static class PitfallInfo {
        final int count;
        final List<Long> examples;
        
        PitfallInfo(int count, List<Long> examples) {
            this.count = count;
            this.examples = new ArrayList<>(examples);
        }
    }
}
