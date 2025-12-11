package cs209a.finalproject_demo.service;

import cs209a.finalproject_demo.dataset.LocalDatasetRepository;
import cs209a.finalproject_demo.dto.TopicCooccurrenceResponse;
import cs209a.finalproject_demo.dto.TopicCooccurrenceResponse.TopicPair;
import cs209a.finalproject_demo.dto.TopicCooccurrenceResponse.VennDiagramData;
import cs209a.finalproject_demo.dto.TopicCooccurrenceResponse.VennSet;
import cs209a.finalproject_demo.dto.TopicCooccurrenceResponse.VennIntersection;
import cs209a.finalproject_demo.model.Question;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
public class TopicCooccurrenceService {

    private final LocalDatasetRepository repository;

    public TopicCooccurrenceService(LocalDatasetRepository repository) {
        this.repository = repository;
    }

    public TopicCooccurrenceResponse topPairs(int topN) {
        List<Question> questions = repository.findAllQuestions();
        Map<String, Long> pairCounters = new HashMap<>();
        Map<String, Long> tagCounters = new HashMap<>();

        // 统计共现对和单个标签出现次数
        for (Question question : questions) {
            List<String> tags = question.tags().stream()
                    .map(tag -> tag.toLowerCase(Locale.ROOT))
                    .filter(tag -> !"java".equals(tag))
                    .distinct()
                    .toList();

            // 统计单个标签
            for (String tag : tags) {
                tagCounters.merge(tag, 1L, Long::sum);
            }

            // 统计共现对
            Set<String> seenPairs = new TreeSet<>();
            for (int i = 0; i < tags.size(); i++) {
                for (int j = i + 1; j < tags.size(); j++) {
                    String a = tags.get(i);
                    String b = tags.get(j);
                    String key = a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
                    if (seenPairs.add(key)) {
                        pairCounters.merge(key, 1L, Long::sum);
                    }
                }
            }
        }

        // 获取 Top N 共现对
        List<TopicPair> topPairs = pairCounters.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(topN)
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|");
                    return new TopicPair(List.of(parts[0], parts[1]), entry.getValue());
                })
                .toList();

        // 生成韦恩图数据（使用前 3 个最强共现对）
        VennDiagramData vennData = generateVennDiagramData(topPairs, tagCounters, Math.min(3, topN));

        return new TopicCooccurrenceResponse(topPairs, vennData);
    }

    /**
     * 生成韦恩图数据
     * 选择前 N 个最强共现对，构建韦恩图
     */
    private VennDiagramData generateVennDiagramData(List<TopicPair> topPairs, 
                                                     Map<String, Long> tagCounters, 
                                                     int vennTopN) {
        if (topPairs.isEmpty() || vennTopN <= 0) {
            return null;
        }

        // 选择前 N 个共现对
        List<TopicPair> selectedPairs = topPairs.stream()
                .limit(vennTopN)
                .toList();

        // 收集所有相关标签
        Set<String> allTags = new TreeSet<>();
        for (TopicPair pair : selectedPairs) {
            allTags.addAll(pair.topics());
        }

        // 构建集合数据（单个标签的出现次数）
        List<VennSet> sets = allTags.stream()
                .map(tag -> new VennSet(tag, tagCounters.getOrDefault(tag, 0L)))
                .sorted(Comparator.comparing(VennSet::size).reversed())
                .toList();

        // 构建交集数据（共现对的次数）
        List<VennIntersection> intersections = selectedPairs.stream()
                .map(pair -> new VennIntersection(pair.topics(), pair.count()))
                .sorted(Comparator.comparing(VennIntersection::size).reversed())
                .toList();

        return new VennDiagramData(sets, intersections);
    }
}
