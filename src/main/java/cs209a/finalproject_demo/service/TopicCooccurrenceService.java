package cs209a.finalproject_demo.service;

import cs209a.finalproject_demo.dataset.LocalDatasetRepository;
import cs209a.finalproject_demo.dto.TopicCooccurrenceResponse;
import cs209a.finalproject_demo.dto.TopicCooccurrenceResponse.TopicPair;
import cs209a.finalproject_demo.model.Question;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 话题共现分析服务
 * 分析标签对的共现频率，用于力导向图可视化
 */
@Service
public class TopicCooccurrenceService {

    private final LocalDatasetRepository repository;
    
    // 预定义的核心 Java 话题集合（可选筛选）
    private static final Set<String> CORE_JAVA_TOPICS = Set.of(
            "spring-boot", "spring", "security", "concurrency", "multithreading",
            "testing", "junit", "mockito", "lambda", "stream", "reflection",
            "socket", "netty", "jdbc", "hibernate", "jpa", "maven", "gradle",
            "collections", "generics", "io", "nio", "exception", "serialization"
    );

    public TopicCooccurrenceService(LocalDatasetRepository repository) {
        this.repository = repository;
    }

    /**
     * 获取 Top N 话题共现对
     * 
     * @param topN 返回的共现对数量
     * @param filterCoreTopics 是否只筛选核心 Java 话题（可选，默认 false）
     * @return 话题共现响应
     */
    public TopicCooccurrenceResponse topPairs(int topN, boolean filterCoreTopics) {
        List<Question> questions = repository.findAllQuestions();
        // 使用 Map<Pair<String, String>, Long> 的等价实现
        // 使用有序的字符串键来确保 (a,b) 和 (b,a) 被视为同一对
        Map<String, Long> pairCounters = new HashMap<>();

        // 遍历每个问题，生成所有无序标签对
        for (Question question : questions) {
            List<String> tags = question.tags().stream()
                    .map(tag -> tag.toLowerCase(Locale.ROOT))
                    .filter(tag -> !"java".equals(tag)) // 排除通用标签 "java"
                    .filter(tag -> !filterCoreTopics || CORE_JAVA_TOPICS.contains(tag)) // 可选：只筛选核心话题
                    .distinct()
                    .toList();

            // 为该问题生成所有无序标签对 (t_i, t_j)，其中 i != j
            // 使用双重循环，j > i 确保每个对只生成一次
            for (int i = 0; i < tags.size(); i++) {
                for (int j = i + 1; j < tags.size(); j++) {
                    String tagA = tags.get(i);
                    String tagB = tags.get(j);
                    // 创建规范化的键（按字典序排序，确保无序）
                    String pairKey = tagA.compareTo(tagB) < 0 
                            ? tagA + "|" + tagB 
                            : tagB + "|" + tagA;
                    // 使用 merge 方法累加频率
                    pairCounters.merge(pairKey, 1L, Long::sum);
                }
            }
        }

        // 对所有标签对按频率降序排序，提取 Top N
        List<TopicPair> topPairs = pairCounters.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(topN)
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|");
                    // 确保顺序一致（按字典序）
                    List<String> topicPair = Arrays.asList(parts[0], parts[1]);
                    return new TopicPair(topicPair, entry.getValue());
                })
                .toList();

        return new TopicCooccurrenceResponse(topPairs);
    }

    /**
     * 获取 Top N 话题共现对（默认不过滤核心话题）
     */
    public TopicCooccurrenceResponse topPairs(int topN) {
        return topPairs(topN, false);
    }
}
