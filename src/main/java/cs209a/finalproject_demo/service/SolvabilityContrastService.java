package cs209a.finalproject_demo.service;

import cs209a.finalproject_demo.dataset.LocalDatasetRepository;
import cs209a.finalproject_demo.dto.SolvabilityContrastResponse;
import cs209a.finalproject_demo.dto.SolvabilityContrastResponse.DistributionStats;
import cs209a.finalproject_demo.dto.SolvabilityContrastResponse.FeatureStats;
import cs209a.finalproject_demo.model.Question;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Locale;

@Service
public class SolvabilityContrastService {

    private final LocalDatasetRepository repository;
    private final ZoneId zoneId = ZoneId.systemDefault();

    public SolvabilityContrastService(LocalDatasetRepository repository) {
        this.repository = repository;
    }

    public SolvabilityContrastResponse analyze(LocalDate from, LocalDate to) {
        List<Question> questions = repository.findAllQuestions();
        if (questions.isEmpty()) {
            return new SolvabilityContrastResponse(List.of());
        }
        LocalDate start = from == null ? questions.stream()
                .map(q -> q.creationDate(zoneId))
                .min(LocalDate::compareTo).orElse(LocalDate.now()) : from;
        LocalDate end = to == null ? questions.stream()
                .map(q -> q.creationDate(zoneId))
                .max(LocalDate::compareTo).orElse(LocalDate.now()) : to;

        List<QuestionProfile> solvable = new ArrayList<>();
        List<QuestionProfile> hard = new ArrayList<>();

        for (Question question : questions) {
            LocalDate created = question.creationDate(zoneId);
            if (created.isBefore(start) || created.isAfter(end)) {
                continue;
            }
            QuestionProfile profile = new QuestionProfile(question);
            if (isSolvable(question)) {
                solvable.add(profile);
            } else if (isHard(question)) {
                hard.add(profile);
            }
        }

        List<FeatureStats> features = List.of(
                buildFeature("title_length", solvable, hard, QuestionProfile::titleWordCount),
                buildFeature("tag_count", solvable, hard, QuestionProfile::tagCount),
                buildFeature("asker_reputation", solvable, hard, QuestionProfile::askerReputation),
                buildFeature("question_score", solvable, hard, QuestionProfile::score),
                buildFeature("answer_count", solvable, hard, QuestionProfile::answerCount)
        );

        return new SolvabilityContrastResponse(features);
    }

    private boolean isSolvable(Question question) {
        return question.answered() || (question.answerCount() > 0 && question.score() >= 1);
    }

    private boolean isHard(Question question) {
        return !question.answered() && question.answerCount() == 0;
    }

    private FeatureStats buildFeature(String name,
                                      List<QuestionProfile> solvable,
                                      List<QuestionProfile> hard,
                                      java.util.function.ToDoubleFunction<QuestionProfile> extractor) {
        DistributionStats solvableStats = summarize(solvable, extractor);
        DistributionStats hardStats = summarize(hard, extractor);
        return new FeatureStats(name, solvableStats, hardStats);
    }

    private DistributionStats summarize(List<QuestionProfile> profiles,
                                        java.util.function.ToDoubleFunction<QuestionProfile> extractor) {
        if (profiles.isEmpty()) {
            return DistributionStats.empty();
        }
        DoubleSummaryStatistics stats = profiles.stream().mapToDouble(extractor).summaryStatistics();
        double median = median(profiles.stream().mapToDouble(extractor).sorted().toArray());
        return new DistributionStats(stats.getAverage(), median, stats.getMin(), stats.getMax());
    }

    private double median(double[] values) {
        if (values.length == 0) {
            return 0;
        }
        int middle = values.length / 2;
        if (values.length % 2 == 0) {
            return (values[middle - 1] + values[middle]) / 2.0;
        }
        return values[middle];
    }

    private static class QuestionProfile {
        private final int titleWordCount;
        private final int tagCount;
        private final int askerReputation;
        private final int score;
        private final int answerCount;

        QuestionProfile(Question question) {
            this.titleWordCount = question.normalizedTitle().isEmpty()
                    ? 0
                    : question.normalizedTitle().split("\\s+").length;
            this.tagCount = question.tags().size();
            this.askerReputation = question.owner() != null ? question.owner().reputation() : 0;
            this.score = question.score();
            this.answerCount = question.answerCount();
        }

        int titleWordCount() {
            return titleWordCount;
        }

        int tagCount() {
            return tagCount;
        }

        int askerReputation() {
            return askerReputation;
        }

        int score() {
            return score;
        }

        int answerCount() {
            return answerCount;
        }
    }
}


















