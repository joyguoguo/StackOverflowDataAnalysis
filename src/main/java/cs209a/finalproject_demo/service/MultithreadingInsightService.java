package cs209a.finalproject_demo.service;

import cs209a.finalproject_demo.dataset.LocalDatasetRepository;
import cs209a.finalproject_demo.dto.MultithreadingPitfallResponse;
import cs209a.finalproject_demo.dto.MultithreadingPitfallResponse.PitfallInsight;
import cs209a.finalproject_demo.model.Question;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 多线程问题分析服务
 * 基于标签、标题、正文和代码片段识别多线程相关问题
 */
@Service
public class MultithreadingInsightService {

    private final LocalDatasetRepository repository;
    private final List<PitfallRule> rules = buildRules();
    private final Set<String> multithreadingTags = Set.of(
            "multithreading", "thread", "concurrency", "synchronization", 
            "executor", "locks", "parallel", "async"
    );

    public MultithreadingInsightService(LocalDatasetRepository repository) {
        this.repository = repository;
    }

    public MultithreadingPitfallResponse analyze(int topN) {
        Map<String, PitfallAggregate> aggregates = new LinkedHashMap<>();

        for (Question question : repository.findAllQuestions()) {
            if (!isMultithreadingQuestion(question)) {
                continue;
            }

            // 分析标题、正文和代码片段
            String fullText = question.fullText();
            String codeSnippet = extractCodeSnippet(question.body());

            // 匹配问题类型
            PitfallRule matchedRule = rules.stream()
                    .filter(rule -> rule.matcher().test(fullText) || 
                                   (codeSnippet != null && rule.matcher().test(codeSnippet)))
                    .findFirst()
                    .orElse(PitfallRule.defaultRule());

            aggregates.computeIfAbsent(matchedRule.label(), 
                    label -> new PitfallAggregate(label, new ArrayList<>()))
                    .addExample(question.id());
        }

        List<PitfallInsight> insights = aggregates.values().stream()
                .sorted(Comparator.comparingInt(PitfallAggregate::occurrence).reversed())
                .limit(topN)
                .map(aggregate -> new PitfallInsight(
                        aggregate.label(),
                        aggregate.occurrence(),
                        aggregate.examples().stream().limit(3).collect(Collectors.toList())))
                .toList();

        return new MultithreadingPitfallResponse(insights);
    }

    /**
     * 判断是否为多线程相关问题
     */
    private boolean isMultithreadingQuestion(Question question) {
        // 1. 检查标签
        boolean tagMatch = question.tags().stream()
                .anyMatch(tag -> multithreadingTags.contains(tag.toLowerCase()));

        // 2. 检查标题和正文中的关键词
        String text = question.fullText().toLowerCase();
        boolean keywordMatch = text.contains("thread") || 
                              text.contains("parallel") || 
                              text.contains("executor") ||
                              text.contains("deadlock") || 
                              text.contains("synchronized") || 
                              text.contains("lock") || 
                              text.contains("concurrent") ||
                              text.contains("race condition") ||
                              text.contains("volatile") ||
                              text.contains("atomic");

        // 3. 检查代码片段中的多线程 API
        boolean apiMatch = false;
        if (question.body() != null) {
            String code = extractCodeSnippet(question.body());
            if (code != null) {
                apiMatch = code.contains("Thread") ||
                          code.contains("Runnable") ||
                          code.contains("Executor") ||
                          code.contains("synchronized") ||
                          code.contains("Lock") ||
                          code.contains("ConcurrentHashMap") ||
                          code.contains("CompletableFuture");
            }
        }

        return tagMatch || keywordMatch || apiMatch;
    }

    /**
     * 从正文中提取代码片段
     */
    private String extractCodeSnippet(String body) {
        if (body == null || body.isEmpty()) {
            return null;
        }

        // 提取 <code> 或 <pre> 标签中的内容
        Pattern codePattern = Pattern.compile(
                "<(?:code|pre)[^>]*>(.*?)</(?:code|pre)>", 
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        java.util.regex.Matcher matcher = codePattern.matcher(body);
        StringBuilder codeBuilder = new StringBuilder();
        
        while (matcher.find()) {
            String code = matcher.group(1);
            // 移除 HTML 实体编码
            code = code.replace("&lt;", "<")
                      .replace("&gt;", ">")
                      .replace("&amp;", "&")
                      .replace("&quot;", "\"");
            codeBuilder.append(code).append("\n");
        }
        
        return codeBuilder.length() > 0 ? codeBuilder.toString() : null;
    }

    /**
     * 构建问题识别规则
     */
    private List<PitfallRule> buildRules() {
        List<PitfallRule> ruleList = new ArrayList<>();
        
        // 死锁相关
        ruleList.add(new PitfallRule("死锁", createPatternMatcher(
                "(deadlock|circular\\s*wait|nested\\s*lock|Thread\\.State\\s*:\\s*BLOCKED|lock\\s*ordering)")));
        
        // 竞态条件
        ruleList.add(new PitfallRule("竞态条件", createPatternMatcher(
                "(race\\s*condition|thread\\s*safe|atomic|inconsistent\\s*result|non-atomic)")));
        
        // 内存可见性
        ruleList.add(new PitfallRule("内存可见性", createPatternMatcher(
                "(volatile|happens-before|memory\\s*visibility|visibility\\s*issue)")));
        
        // 线程池问题
        ruleList.add(new PitfallRule("线程池问题", createPatternMatcher(
                "(thread\\s*pool|executorservice|pool\\s*exhaustion|RejectedExecution|ThreadPoolExecutor)")));
        
        // 锁/监视器异常
        ruleList.add(new PitfallRule("锁/监视器异常", createPatternMatcher(
                "(IllegalMonitorStateException|monitor|synchronized\\s*block|lock\\s*exception)")));
        
        // 性能与资源问题
        ruleList.add(new PitfallRule("性能与资源", createPatternMatcher(
                "(performance|resource|context\\s*switch|scalability|bottleneck|throughput)")));
        
        // 线程安全问题（集合类）
        ruleList.add(new PitfallRule("线程安全集合", createPatternMatcher(
                "((ArrayList|HashMap|HashSet).*thread|thread.*(ArrayList|HashMap|HashSet)|ConcurrentModificationException)")));
        
        // 默认规则（其他问题）
        ruleList.add(PitfallRule.defaultRule());
        
        return ruleList;
    }

    private java.util.function.Predicate<String> createPatternMatcher(String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return text -> pattern.matcher(text).find();
    }

    private record PitfallRule(String label, java.util.function.Predicate<String> matcher) {
        static PitfallRule defaultRule() {
            return new PitfallRule("其他多线程问题", text -> true);
        }
    }

    private static class PitfallAggregate {
        private final String label;
        private final List<Long> examples;

        PitfallAggregate(String label, List<Long> examples) {
            this.label = label;
            this.examples = examples;
        }

        void addExample(long questionId) {
            examples.add(questionId);
        }

        int occurrence() {
            return examples.size();
        }

        String label() {
            return label;
        }

        List<Long> examples() {
            return examples;
        }
    }
}
