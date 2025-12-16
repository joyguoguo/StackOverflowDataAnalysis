package cs209a.finalproject_demo.service;

import cs209a.finalproject_demo.dto.MultithreadingPitfallResponse;
import cs209a.finalproject_demo.entity.AnswerEntity;
import cs209a.finalproject_demo.entity.QuestionEntity;
import cs209a.finalproject_demo.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Multithreading pitfalls analysis service.
 *
 * This implementation follows the project plan:
 *  - Use DB + tag based filtering to get candidate Java multithreading questions.
 *  - Build TF-IDF vectors for question texts (title + body + accepted answer).
 *  - Define fixed P1–P9 categories with English descriptions and regex seeds.
 *  - Build prototype vectors for each category and classify questions via cosine similarity.
 *  - Aggregate counts and return a flat Top-N list for visualization.
 */
@Service
public class MultithreadingInsightService {

    private static final Logger log = LoggerFactory.getLogger(MultithreadingInsightService.class);

    private final QuestionRepository questionRepository;
    
    // Java异常类名模式（用于识别异常类型）
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile(
            "\\b(java\\.(lang|util|io|nio|concurrent)\\.)?[A-Z]\\w*Exception\\b"
    );
    
    // Multithreading-related broad keyword patterns (used for coarse filtering)
    private static final Map<String, Pattern> MULTITHREADING_KEYWORD_PATTERNS = new HashMap<>();

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
        MULTITHREADING_KEYWORD_PATTERNS.put("deadlock", Pattern.compile("\\bdeadlock\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("race condition", Pattern.compile("\\brace\\s+condition\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("data race", Pattern.compile("\\bdata\\s+race\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("thread", Pattern.compile("\\bthread(s)?\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("synchronized", Pattern.compile("\\bsynchronized\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("volatile", Pattern.compile("\\bvolatile\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("lock", Pattern.compile("\\block\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("concurrent", Pattern.compile("\\bconcurrent\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("thread pool", Pattern.compile("\\bthread\\s+pool\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("executor", Pattern.compile("\\bexecutor\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("future", Pattern.compile("\\bfuture\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("semaphore", Pattern.compile("\\bsemaphore\\b", Pattern.CASE_INSENSITIVE));
        MULTITHREADING_KEYWORD_PATTERNS.put("latch", Pattern.compile("\\blatch\\b", Pattern.CASE_INSENSITIVE));
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
     * Analyze multithreading pitfalls and return Top-N fixed categories (P1–P9).
     */
    public MultithreadingPitfallResponse analyze(int topN) {
        // 1) DB-level coarse filtering by tags
        List<QuestionEntity> candidateQuestions = questionRepository.findPotentialMultithreadingQuestions();

        // 2) In-memory coarse filtering by broad multithreading regex keywords
        List<QuestionText> texts = new ArrayList<>();
        for (QuestionEntity question : candidateQuestions) {
            String merged = mergeQuestionText(question);
            if (!isLikelyMultithreadingQuestion(merged)) {
                continue;
            }

            Optional<AnswerEntity> acceptedAnswer = findAcceptedAnswer(question);
            String answerText = acceptedAnswer
                    .map(a -> filterCodeSnippets(a.getBody()))
                    .orElse("");

            String fullText = (merged + " " + answerText).toLowerCase(Locale.ROOT);
            texts.add(new QuestionText(question.getQuestionId(), fullText));
        }

        // Debug: log all multithreading candidate question IDs after in-memory filtering
        List<Long> allIds = texts.stream().map(QuestionText::id).toList();
        log.info("Multithreading candidate questions after in-memory filtering ({}): {}", allIds.size(), allIds);

        if (texts.isEmpty()) {
            return new MultithreadingPitfallResponse(List.of());
        }

        // 3) Build TF-IDF vectorizer over all question texts
        TfIdfVectorizer vectorizer = TfIdfVectorizer.fit(
                texts.stream().map(t -> t.text).toList()
        );

        // 4) Build prototype vector for each fixed category
        Map<PitfallCategory, TfIdfVector> categoryPrototypes =
                buildCategoryPrototypes(vectorizer, texts);

        // 5) Classify each question into the best category using cosine similarity
        Map<PitfallCategory, PitfallStats> statsByCategory = new HashMap<>();

        for (int i = 0; i < texts.size(); i++) {
            QuestionText qt = texts.get(i);
            TfIdfVector qVec = vectorizer.vectorize(qt.text);

            ClassificationResult result =
                    classifyQuestion(qt, qVec, categoryPrototypes);

            if (result == null) {
                continue; // similarity below threshold ⇒ skip
            }

            statsByCategory
                    .computeIfAbsent(result.category(), c -> new PitfallStats())
                    .addExample(qt.id);
        }

        // Debug: log per-category question ID lists
        for (Map.Entry<PitfallCategory, PitfallStats> entry : statsByCategory.entrySet()) {
            PitfallCategory category = entry.getKey();
            PitfallStats s = entry.getValue();
            log.info(
                    "Pitfall category {} ({}) count={} questionIds={}",
                    category.code(), category.label(), s.count(), s.examples()
            );
        }

        // 6) Aggregate into flat list, sort by count and limit topN categories
        List<MultithreadingPitfallResponse.PitfallStat> stats = statsByCategory.entrySet()
                .stream()
                .map(entry -> {
                    PitfallCategory category = entry.getKey();
                    PitfallStats s = entry.getValue();
                    return new MultithreadingPitfallResponse.PitfallStat(
                            category.code(),
                            category.label(),
                            s.count(),
                            s.examples().stream().limit(3).toList()
                    );
                })
                .sorted(Comparator.comparingLong(MultithreadingPitfallResponse.PitfallStat::count).reversed())
                .limit(topN)
                .toList();

        return new MultithreadingPitfallResponse(stats);
    }

    /**
     * Merge title and body, remove HTML and code for coarse text.
     */
    private String mergeQuestionText(QuestionEntity question) {
        String title = question.getTitle() == null ? "" : question.getTitle();
        String body = question.getBody() == null ? "" : question.getBody();
        return filterCodeSnippets(title + " " + body);
    }

    /**
     * Coarse in-memory filter using broad multithreading regex patterns.
     */
    private boolean isLikelyMultithreadingQuestion(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);

        for (Pattern pattern : MULTITHREADING_KEYWORD_PATTERNS.values()) {
            if (pattern.matcher(lower).find()) {
                return true;
            }
        }

        // fall back to exception based detection
        return EXCEPTION_PATTERN.matcher(lower).find();
    }

    /**
     * Build prototype vectors for each fixed category using description text and
     * seed-hit questions.
     */
    private Map<PitfallCategory, TfIdfVector> buildCategoryPrototypes(
            TfIdfVectorizer vectorizer,
            List<QuestionText> texts
    ) {
        Map<PitfallCategory, List<String>> categoryDocs = new HashMap<>();

        for (PitfallCategory category : PitfallCategory.values()) {
            categoryDocs.put(category, new ArrayList<>());
            // always include description text
            categoryDocs.get(category).add(category.description());
        }

        // add texts of questions that match seed patterns
        for (QuestionText qt : texts) {
            for (PitfallCategory category : PitfallCategory.values()) {
                if (category.matchesSeed(qt.text)) {
                    categoryDocs.get(category).add(qt.text);
                }
            }
        }

        Map<PitfallCategory, TfIdfVector> prototypes = new HashMap<>();
        for (Map.Entry<PitfallCategory, List<String>> entry : categoryDocs.entrySet()) {
            PitfallCategory category = entry.getKey();
            List<String> docs = entry.getValue();
            if (docs.isEmpty()) {
                prototypes.put(category, TfIdfVector.empty());
                continue;
            }
            // average vectors of all docs for this category
            TfIdfVector sum = TfIdfVector.empty();
            for (String doc : docs) {
                sum = sum.add(vectorizer.vectorize(doc));
            }
            prototypes.put(category, sum.divide(docs.size()));
        }

        return prototypes;
    }

    /**
     * Classify a question into one of the fixed categories using cosine similarity.
     */
    private ClassificationResult classifyQuestion(
            QuestionText questionText,
            TfIdfVector qVec,
            Map<PitfallCategory, TfIdfVector> prototypes
    ) {
        double bestScore = 0.0;
        PitfallCategory bestCategory = null;

        for (Map.Entry<PitfallCategory, TfIdfVector> entry : prototypes.entrySet()) {
            PitfallCategory category = entry.getKey();
            TfIdfVector proto = entry.getValue();
            if (proto.isEmpty()) {
                continue;
            }

            double sim = TfIdfVector.cosineSimilarity(qVec, proto);

            // bonus if question text matches any seed pattern (strong signal)
            if (category.matchesSeed(questionText.text)) {
                sim += 0.10;
            }

            if (sim > bestScore) {
                bestScore = sim;
                bestCategory = category;
            }
        }

        // similarity threshold – can be tuned
        final double THRESHOLD = 0.10;
        if (bestCategory == null || bestScore < THRESHOLD) {
            return null;
        }
        return new ClassificationResult(bestCategory, bestScore);
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

    private record QuestionText(long id, String text) {
    }

    private static class PitfallStats {
        private int count;
        private final List<Long> examples = new ArrayList<>();

        void addExample(long questionId) {
            count++;
            examples.add(questionId);
        }

        int count() {
            return count;
        }

        List<Long> examples() {
            return examples;
        }
    }

    private record ClassificationResult(PitfallCategory category, double score) {
    }

    /**
     * Fixed pitfall categories P1–P9 with English labels, descriptions and seed regex patterns.
     */
    private enum PitfallCategory {
        P1_RACE_CONDITION(
                "P1",
                "Race Condition",
                """
                        Multiple threads interleave operations causing inconsistent state or unexpected results.
                        Mentions race condition, data race or not thread-safe access to shared variables.
                        """,
                List.of(
                        Pattern.compile("\\brace\\s+condition\\b", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("\\bdata\\s+race\\b", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("\\bnot\\s+thread\\s+safe\\b", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("thread\\s+interference", Pattern.CASE_INSENSITIVE)
                )
        ),
        P2_DEADLOCK(
                "P2",
                "Deadlock",
                """
                        Threads are waiting on each other while holding locks leading to deadlock or livelock.
                        Typically stuck with Thread.State: BLOCKED or waiting for monitor forever.
                        """,
                List.of(
                        Pattern.compile("\\bdeadlock\\b", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("Thread\\.State\\s*:\\s*BLOCKED", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("waiting\\s+for\\s+monitor", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("circular\\s+wait", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("\\blivelock\\b", Pattern.CASE_INSENSITIVE)
                )
        ),
        P3_MEMORY_VISIBILITY(
                "P3",
                "Memory Visibility",
                """
                        Updates made by one thread are not visible to others because of Java Memory Model issues.
                        Often involves volatile, stale values or missing happens-before relationships.
                        """,
                List.of(
                        Pattern.compile("\\bvolatile\\b", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("stale\\s+(data|value)", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("visibility\\s+issue", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("happens-before", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("memory\\s+barrier", Pattern.CASE_INSENSITIVE)
                )
        ),
        P4_SYNCHRONIZATION_MISUSE(
                "P4",
                "Synchronization Misuse",
                """
                        Incorrect use of synchronized blocks or locks such as wrong monitor object or missing lock.
                        Often reported with IllegalMonitorStateException or monitor not owned errors.
                        """,
                List.of(
                        Pattern.compile("synchronized\\s+block", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("wrong\\s+lock\\s+object", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("monitor\\s+not\\s+owned", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("missing\\s+lock", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("IllegalMonitorStateException", Pattern.CASE_INSENSITIVE)
                )
        ),
        P5_UNSAFE_COLLECTIONS(
                "P5",
                "Unsafe Collections",
                """
                        Using non-thread-safe collections from multiple threads without proper synchronization.
                        Typical symptoms include ConcurrentModificationException or corrupted maps/lists.
                        """,
                List.of(
                        Pattern.compile("ConcurrentModificationException", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("ArrayList", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("HashMap", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("multithread", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("non-?thread-?safe\\s+(map|list|set)", Pattern.CASE_INSENSITIVE)
                )
        ),
        P6_THREAD_POOL(
                "P6",
                "Thread Pool Misconfiguration",
                """
                        Problems caused by wrong thread pool size, queue capacity or lifecycle management.
                        Often shows RejectedExecutionException, tasks never executed or OOM caused by too many threads.
                        """,
                List.of(
                        Pattern.compile("RejectedExecutionException", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("ThreadPoolExecutor", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("thread\\s+pool\\s+size", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("queue\\s+capacity", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("thread\\s+pool\\s+full", Pattern.CASE_INSENSITIVE)
                )
        ),
        P7_WAIT_NOTIFY(
                "P7",
                "Wait/Notify Misuse",
                """
                        Incorrect usage of wait/notify/notifyAll, such as calling wait without holding the monitor.
                        Typically involves IllegalMonitorStateException or threads waiting forever.
                        """,
                List.of(
                        Pattern.compile("IllegalMonitorStateException", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("wait\\s*\\(\\)", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("notify(All)?\\s*\\(\\)", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("wait\\s+without\\s+lock", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("wait\\s+never\\s+notified", Pattern.CASE_INSENSITIVE)
                )
        ),
        P8_UNEXPECTED_TERMINATION(
                "P8",
                "Unexpected Thread Termination",
                """
                        Threads terminate unexpectedly or exceptions are not handled correctly.
                        Includes uncaught exceptions in threads or Future.get() blocking forever.
                        """,
                List.of(
                        Pattern.compile("thread\\s+stopped\\s+unexpectedly", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("uncaught\\s+exception", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("Future\\.get\\(\\)\\s+hangs", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("ExecutionException", Pattern.CASE_INSENSITIVE)
                )
        ),
        P9_PERFORMANCE(
                "P9",
                "Performance Bottlenecks",
                """
                        Performance issues that only appear when using multiple threads.
                        Often caused by heavy locking, contention or poor scalability.
                        """,
                List.of(
                        Pattern.compile("slow\\s+with\\s+multiple\\s+threads", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("heavy\\s+locking", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("contention", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("scalability", Pattern.CASE_INSENSITIVE),
                        Pattern.compile("throughput", Pattern.CASE_INSENSITIVE)
                )
        );

        private final String code;
        private final String label;
        private final String description;
        private final List<Pattern> seedPatterns;

        PitfallCategory(String code, String label, String description, List<Pattern> seedPatterns) {
            this.code = code;
            this.label = label;
            this.description = description;
            this.seedPatterns = seedPatterns;
        }

        String code() {
            return code;
        }

        String label() {
            return label;
        }

        String description() {
            return description;
        }

        boolean matchesSeed(String text) {
            for (Pattern pattern : seedPatterns) {
                if (pattern.matcher(text).find()) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Simple sparse TF-IDF vector.
     */
    private static class TfIdfVector {
        private final Map<Integer, Double> values;

        TfIdfVector(Map<Integer, Double> values) {
            this.values = values;
        }

        static TfIdfVector empty() {
            return new TfIdfVector(Map.of());
        }

        boolean isEmpty() {
            return values.isEmpty();
        }

        TfIdfVector add(TfIdfVector other) {
            Map<Integer, Double> result = new HashMap<>(this.values);
            for (Map.Entry<Integer, Double> e : other.values.entrySet()) {
                result.merge(e.getKey(), e.getValue(), Double::sum);
            }
            return new TfIdfVector(result);
        }

        TfIdfVector divide(double scalar) {
            if (scalar == 0.0 || values.isEmpty()) {
                return empty();
            }
            Map<Integer, Double> result = new HashMap<>();
            for (Map.Entry<Integer, Double> e : values.entrySet()) {
                result.put(e.getKey(), e.getValue() / scalar);
            }
            return new TfIdfVector(result);
        }

        static double cosineSimilarity(TfIdfVector a, TfIdfVector b) {
            if (a.values.isEmpty() || b.values.isEmpty()) {
                return 0.0;
            }

            double dot = 0.0;
            // iterate over smaller map for efficiency
            Map<Integer, Double> small = a.values.size() <= b.values.size() ? a.values : b.values;
            Map<Integer, Double> large = small == a.values ? b.values : a.values;

            for (Map.Entry<Integer, Double> e : small.entrySet()) {
                Double v = large.get(e.getKey());
                if (v != null) {
                    dot += e.getValue() * v;
                }
            }

            double normA = 0.0;
            for (double v : a.values.values()) {
                normA += v * v;
            }
            double normB = 0.0;
            for (double v : b.values.values()) {
                normB += v * v;
            }
            if (normA == 0.0 || normB == 0.0) {
                return 0.0;
            }
            return dot / (Math.sqrt(normA) * Math.sqrt(normB));
        }
    }

    /**
     * Simple TF-IDF vectorizer for English text.
     */
    private static class TfIdfVectorizer {
        private final Map<String, Integer> vocabulary;
        private final double[] idf;

        private TfIdfVectorizer(Map<String, Integer> vocabulary, double[] idf) {
            this.vocabulary = vocabulary;
            this.idf = idf;
        }

        static TfIdfVectorizer fit(List<String> documents) {
            Map<String, Integer> vocab = new HashMap<>();
            List<Set<Integer>> docTermSets = new ArrayList<>();

            for (String doc : documents) {
                String[] tokens = tokenize(doc);
                Set<Integer> seenInDoc = new HashSet<>();
                for (String token : tokens) {
                    if (token.length() < 2) continue;
                    int index = vocab.computeIfAbsent(token, k -> vocab.size());
                    seenInDoc.add(index);
                }
                docTermSets.add(seenInDoc);
            }

            int vocabSize = vocab.size();
            double[] df = new double[vocabSize];
            for (Set<Integer> docTerms : docTermSets) {
                for (Integer idx : docTerms) {
                    df[idx] += 1.0;
                }
            }

            double[] idf = new double[vocabSize];
            double nDocs = documents.size();
            for (int i = 0; i < vocabSize; i++) {
                idf[i] = Math.log(nDocs / (1.0 + df[i]));
            }

            return new TfIdfVectorizer(vocab, idf);
        }

        TfIdfVector vectorize(String doc) {
            if (doc == null || doc.isEmpty() || vocabulary.isEmpty()) {
                return TfIdfVector.empty();
            }

            String[] tokens = tokenize(doc);
            Map<Integer, Double> tf = new HashMap<>();
            for (String token : tokens) {
                if (token.length() < 2) continue;
                Integer idx = vocabulary.get(token);
                if (idx != null) {
                    tf.merge(idx, 1.0, Double::sum);
                }
            }
            if (tf.isEmpty()) {
                return TfIdfVector.empty();
            }

            Map<Integer, Double> tfidf = new HashMap<>();
            for (Map.Entry<Integer, Double> e : tf.entrySet()) {
                int idx = e.getKey();
                double termFreq = e.getValue();
                tfidf.put(idx, termFreq * idf[idx]);
            }
            return new TfIdfVector(tfidf);
        }

        private static String[] tokenize(String text) {
            if (text == null) return new String[0];
            return text.toLowerCase(Locale.ROOT).split("\\W+");
        }
    }
}
