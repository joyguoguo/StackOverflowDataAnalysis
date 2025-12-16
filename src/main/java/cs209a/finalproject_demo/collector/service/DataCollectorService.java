package cs209a.finalproject_demo.collector.service;

import cs209a.finalproject_demo.collector.client.StackOverflowApiClient;
import cs209a.finalproject_demo.collector.saver.ThreadDataSaver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据采集服务
 * 负责从 Stack Overflow API 采集完整的线程数据（问题 + 回答 + 评论）
 */
@Service
public class DataCollectorService {

    private static final Logger log = LoggerFactory.getLogger(DataCollectorService.class);

    private final StackOverflowApiClient apiClient;
    private final ThreadDataSaver dataSaver;

    public DataCollectorService(StackOverflowApiClient apiClient, ThreadDataSaver dataSaver) {
        this.apiClient = apiClient;
        this.dataSaver = dataSaver;
    }

    /**
     * 采集指定数量的 Java 线程
     * 
     * @param targetCount 目标采集数量
     * @param outputDir 输出目录
     * @param fromDate 起始日期（Unix 时间戳，可选）
     * @param toDate 结束日期（Unix 时间戳，可选）
     * @return 采集统计信息
     */
    public CollectionResult collectThreads(int targetCount, String outputDir, 
                                          Long fromDate, Long toDate) {
        log.info("Starting data collection. Target: {} threads, Output: {}", targetCount, outputDir);
        
        CollectionResult result = new CollectionResult();
        int collectedCount = 0;
        int page = 1;
        int pageSize = 100;
        
        // 如果没有指定日期范围，默认采集最近的数据
        if (fromDate == null && toDate == null) {
            toDate = Instant.now().getEpochSecond();
            fromDate = toDate - (365L * 24 * 60 * 60); // 默认过去一年
        }

        Set<Long> processedQuestionIds = new HashSet<>();

        try {
            while (collectedCount < targetCount) {
                // 检查配额
                int quotaRemaining = apiClient.getQuotaRemaining();
                if (quotaRemaining < 100 && quotaRemaining > 0) {
                    log.warn("Quota running low: {}. Consider pausing collection.", quotaRemaining);
                } else if (quotaRemaining <= 0) {
                    log.error("API quota exhausted. Stopping collection.");
                    result.addError("API quota exhausted");
                    break;
                }

                // 获取问题列表
                log.info("Fetching page {} (collected: {}/{})", page, collectedCount, targetCount);
                
                StackOverflowApiClient.ApiResponse questionsResponse;
                try {
                    questionsResponse = apiClient.fetchQuestions(
                            page, pageSize, "creation", "desc", fromDate, toDate);
                } catch (InterruptedException e) {
                    log.warn("Collection interrupted");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Failed to fetch questions (page {}): {}", page, e.getMessage());
                    result.addError("Failed to fetch page " + page + ": " + e.getMessage());
                    
                    // 如果连续失败，停止采集
                    if (result.getErrors().size() >= 5) {
                        log.error("Too many consecutive errors. Stopping collection.");
                        break;
                    }
                    
                    // 等待后重试
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }

                List<JsonNode> questions = questionsResponse.getItems();
                
                if (questions.isEmpty()) {
                    log.info("No more questions available. Stopping collection.");
                    break;
                }

                // 处理每个问题
                for (JsonNode questionNode : questions) {
                    if (collectedCount >= targetCount) {
                        break;
                    }

                    long questionId = questionNode.path("question_id").asLong();
                    
                    // 跳过已处理的问题
                    if (processedQuestionIds.contains(questionId)) {
                        continue;
                    }
                    processedQuestionIds.add(questionId);

                    try {
                        // 采集完整的线程数据
                        ObjectNode threadJson = collectFullThread(questionId, questionNode);
                        
                        // 保存到文件
                        int threadIndex = collectedCount + 1;
                        dataSaver.saveThread(outputDir, threadJson, threadIndex);
                        
                        collectedCount++;
                        result.incrementSuccess();
                        
                        log.info("Collected thread {}/{} (question_id: {})", 
                                collectedCount, targetCount, questionId);
                        
                        // 添加短暂延迟，避免过快请求
                        Thread.sleep(200);
                        
                    } catch (Exception e) {
                        log.error("Failed to collect thread for question {}: {}", 
                                questionId, e.getMessage());
                        result.incrementFailure();
                        result.addError("Question " + questionId + ": " + e.getMessage());
                    }
                }

                // 检查是否还有更多数据
                if (!questionsResponse.hasMore()) {
                    log.info("No more pages available. Stopping collection.");
                    break;
                }

                page++;
                
                // 每采集 10 个线程输出一次进度
                if (collectedCount % 10 == 0) {
                    log.info("Progress: {}/{} threads collected", collectedCount, targetCount);
                }
            }

            log.info("Collection completed. Total: {} threads, Success: {}, Failed: {}", 
                    collectedCount, result.getSuccessCount(), result.getFailureCount());

        } catch (Exception e) {
            log.error("Unexpected error during collection: {}", e.getMessage(), e);
            result.addError("Unexpected error: " + e.getMessage());
        }

        result.setTotalCollected(collectedCount);
        result.setQuotaRemaining(apiClient.getQuotaRemaining());
        return result;
    }

    /**
     * 采集完整的线程数据（问题 + 回答 + 评论）
     */
    private ObjectNode collectFullThread(long questionId, JsonNode questionNode) 
            throws IOException, InterruptedException {
        
        // 1. 获取所有回答
        List<JsonNode> answers = new ArrayList<>();
        try {
            StackOverflowApiClient.ApiResponse answersResponse = 
                    apiClient.fetchAnswers(List.of(questionId));
            answers = answersResponse.getItems();
            
            // 添加延迟
            Thread.sleep(100);
        } catch (Exception e) {
            log.warn("Failed to fetch answers for question {}: {}", questionId, e.getMessage());
        }

        // 2. 获取问题评论
        List<JsonNode> questionComments = new ArrayList<>();
        try {
            StackOverflowApiClient.ApiResponse commentsResponse = 
                    apiClient.fetchQuestionComments(List.of(questionId));
            questionComments = commentsResponse.getItems();
            
            Thread.sleep(100);
        } catch (Exception e) {
            log.warn("Failed to fetch question comments for question {}: {}", questionId, e.getMessage());
        }

        // 3. 获取回答的评论
        Map<Long, List<JsonNode>> answerComments = new HashMap<>();
        if (!answers.isEmpty()) {
            List<Long> answerIds = answers.stream()
                    .map(a -> a.path("answer_id").asLong())
                    .collect(Collectors.toList());
            
            try {
                StackOverflowApiClient.ApiResponse answerCommentsResponse = 
                        apiClient.fetchAnswerComments(answerIds);
                
                // 按 answer_id 分组
                for (JsonNode commentNode : answerCommentsResponse.getItems()) {
                    long answerId = commentNode.path("post_id").asLong();
                    answerComments.computeIfAbsent(answerId, k -> new ArrayList<>()).add(commentNode);
                }
                
                Thread.sleep(100);
            } catch (Exception e) {
                log.warn("Failed to fetch answer comments for question {}: {}", questionId, e.getMessage());
            }
        }

        // 4. 构建完整的线程 JSON
        return dataSaver.buildThreadJson(questionNode, answers, questionComments, answerComments);
    }

    /**
     * 采集结果统计
     */
    public static class CollectionResult {
        private int totalCollected = 0;
        private int successCount = 0;
        private int failureCount = 0;
        private int quotaRemaining = 0;
        private final List<String> errors = new ArrayList<>();
        private final Instant startTime = Instant.now();
        private Instant endTime;

        public void incrementSuccess() {
            successCount++;
        }

        public void incrementFailure() {
            failureCount++;
        }

        public void addError(String error) {
            errors.add(error);
        }

        public void setTotalCollected(int totalCollected) {
            this.totalCollected = totalCollected;
            this.endTime = Instant.now();
        }

        public void setQuotaRemaining(int quotaRemaining) {
            this.quotaRemaining = quotaRemaining;
        }

        public int getTotalCollected() {
            return totalCollected;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailureCount() {
            return failureCount;
        }

        public int getQuotaRemaining() {
            return quotaRemaining;
        }

        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        public long getDurationSeconds() {
            if (endTime == null) {
                return ChronoUnit.SECONDS.between(startTime, Instant.now());
            }
            return ChronoUnit.SECONDS.between(startTime, endTime);
        }

        @Override
        public String toString() {
            return String.format(
                    "CollectionResult{total=%d, success=%d, failed=%d, quota=%d, duration=%ds, errors=%d}",
                    totalCollected, successCount, failureCount, quotaRemaining, 
                    getDurationSeconds(), errors.size());
        }
    }
}






























