package cs209a.finalproject_demo.service;

import cs209a.finalproject_demo.dataset.LocalDatasetRepository;
import cs209a.finalproject_demo.dto.TopicTrendResponse;
import cs209a.finalproject_demo.dto.TopicTrendResponse.DataPoint;
import cs209a.finalproject_demo.dto.TopicTrendResponse.TopicSeries;
import cs209a.finalproject_demo.model.Question;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TopicTrendService {

    private final LocalDatasetRepository repository;
    private final ZoneId zoneId = ZoneId.systemDefault();

    public TopicTrendService(LocalDatasetRepository repository) {
        this.repository = repository;
    }

    public TopicTrendResponse analyze(List<String> topics,
                                      Metric metric,
                                      LocalDate from,
                                      LocalDate to,
                                      int topN) {
        List<Question> questions = repository.findAllQuestions();
        if (questions.isEmpty()) {
            return TopicTrendResponse.empty(metric.name());
        }
        // 统一时间区间长度：默认最近 12 个月
        LocalDate maxDate = questions.stream()
                .map(q -> q.creationDate(zoneId))
                .max(LocalDate::compareTo)
                .orElse(LocalDate.now(zoneId));
        LocalDate minDate = questions.stream()
                .map(q -> q.creationDate(zoneId))
                .min(LocalDate::compareTo)
                .orElse(maxDate.minusMonths(12));

        YearMonth end = to == null ? YearMonth.from(maxDate) : YearMonth.from(to);
        // 从 end 往前推 11 个月，保证等长 12 个月区间
        YearMonth start = from == null
                ? end.minusMonths(11)
                : YearMonth.from(from);
        if (start.isBefore(YearMonth.from(minDate))) {
            start = YearMonth.from(minDate);
        }

        List<YearMonth> buckets = generateBuckets(start, end);

        List<String> requestedTopics;
        if (CollectionUtils.isEmpty(topics)) {
            requestedTopics = topNTags(questions, topN);
        } else {
            requestedTopics = topics.stream()
                    .map(t -> t.toLowerCase(Locale.ROOT))
                    .distinct()
                    .toList();
        }

        List<TopicSeries> series = requestedTopics.stream()
                .map(topic -> buildSeries(topic, metric, buckets, questions))
                .collect(Collectors.toList());

        return new TopicTrendResponse(series, metric.name(), "MONTH", start.atDay(1), end.atEndOfMonth());
    }

    private TopicSeries buildSeries(String topic,
                                    Metric metric,
                                    List<YearMonth> buckets,
                                    List<Question> questions) {
        Map<YearMonth, Double> aggregated = new LinkedHashMap<>();
        buckets.forEach(bucket -> aggregated.put(bucket, 0.0));

        questions.stream()
                .filter(q -> matchesTopic(q, topic))
                .forEach(question -> {
                    YearMonth bucket = YearMonth.from(question.creationDate(zoneId));
                    if (aggregated.containsKey(bucket)) {
                        aggregated.computeIfPresent(bucket, (b, value) -> value + metricValue(question, metric));
                    }
                });

        List<DataPoint> points = aggregated.entrySet().stream()
                .map(entry -> new DataPoint(entry.getKey().toString(), entry.getValue()))
                .toList();

        return new TopicSeries(topic, points);
    }

    private double metricValue(Question question, Metric metric) {
        return switch (metric) {
            case QUESTIONS -> 1.0;
            case ANSWERS -> Math.max(question.answerCount(), 0);
            case SCORE -> question.score();
            case ENGAGEMENT -> question.viewCount();
        };
    }

    private boolean matchesTopic(Question question, String tagName) {
        String normalized = tagName.toLowerCase(Locale.ROOT);
        return question.tags().stream().anyMatch(t -> t.equalsIgnoreCase(normalized));
    }

    private List<YearMonth> generateBuckets(YearMonth start, YearMonth end) {
        List<YearMonth> buckets = new ArrayList<>();
        YearMonth cursor = start;
        while (!cursor.isAfter(end)) {
            buckets.add(cursor);
            cursor = cursor.plusMonths(1);
        }
        return buckets;
    }

    private List<String> topNTags(List<Question> questions, int topN) {
        Map<String, Long> counts = new LinkedHashMap<>();
        questions.forEach(q -> q.tags().forEach(tag -> counts.merge(tag, 1L, Long::sum)));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .toList();
    }

    public enum Metric {
        QUESTIONS,
        ANSWERS,
        SCORE,
        ENGAGEMENT;

        public static Metric from(String raw) {
            return EnumSet.allOf(Metric.class).stream()
                    .filter(m -> m.name().equalsIgnoreCase(raw))
                    .findFirst()
                    .orElse(QUESTIONS);
        }
    }

}

