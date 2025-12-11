package cs209a.finalproject_demo.dataset;

import cs209a.finalproject_demo.model.Question;
import cs209a.finalproject_demo.model.QuestionThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class LocalDatasetRepository {

    private static final Logger log = LoggerFactory.getLogger(LocalDatasetRepository.class);

    private final List<QuestionThread> threads;

    public LocalDatasetRepository(@Value("${dataset.folder:Sample_SO_data}") String datasetFolder,
                                  ThreadFileLoader loader) {
        this.threads = loadThreads(datasetFolder, loader);
        log.info("Loaded {} threads from {}", threads.size(), datasetFolder);
    }

    private List<QuestionThread> loadThreads(String datasetFolder, ThreadFileLoader loader) {
        Path folderPath = Paths.get(datasetFolder);
        if (!Files.exists(folderPath)) {
            log.warn("Dataset folder {} not found, continuing with empty dataset", datasetFolder);
            return List.of();
        }
        try {
            return Files.list(folderPath)
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::getFileName))
                    .map(path -> loader.load(path).orElse(null))
                    .filter(thread -> thread != null && thread.question() != null)
                    .collect(Collectors.toUnmodifiableList());
        } catch (Exception e) {
            log.error("Failed to load dataset from {}: {}", datasetFolder, e.getMessage());
            return List.of();
        }
    }

    public List<QuestionThread> findAllThreads() {
        return threads;
    }

    public List<Question> findAllQuestions() {
        return threads.stream().map(QuestionThread::question).toList();
    }

    public Optional<QuestionThread> findByQuestionId(long questionId) {
        return threads.stream().filter(t -> t.question().id() == questionId).findFirst();
    }

    public Optional<Instant> minCreationInstant() {
        return threads.stream()
                .map(t -> t.question().creationInstant())
                .min(Instant::compareTo);
    }

    public Optional<Instant> maxCreationInstant() {
        return threads.stream()
                .map(t -> t.question().creationInstant())
                .max(Instant::compareTo);
    }

    public int totalAnswerCount() {
        return threads.stream().mapToInt(t -> t.answers().size()).sum();
    }

    public int totalCommentCount() {
        return (int) threads.stream()
                .mapToLong(t -> t.questionComments().size() +
                        t.answerComments().values().stream().mapToInt(List::size).sum())
                .sum();
    }
}

