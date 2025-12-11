package cs209a.finalproject_demo.collector.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Stack Overflow API 客户端
 * 处理 API 请求、速率限制和重试逻辑
 */
@Component
public class StackOverflowApiClient {

    private static final Logger log = LoggerFactory.getLogger(StackOverflowApiClient.class);
    
    private static final String BASE_URL = "https://api.stackexchange.com/2.3";
    private static final String SITE = "stackoverflow";
    
    // 速率限制：每秒最多 30 个请求（保守估计）
    private static final long MIN_REQUEST_INTERVAL_MS = 100;
    
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String accessToken;
    private final String apiKey;
    
    private long lastRequestTime = 0;
    private int quotaRemaining = Integer.MAX_VALUE;
    private int backoffSeconds = 0;

    public StackOverflowApiClient(String accessToken) {
        this(accessToken, null);
    }

    public StackOverflowApiClient(String accessToken, String apiKey) {
        this.accessToken = accessToken;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public StackOverflowApiClient() {
        this(null, null);
    }

    /**
     * 获取带 java 标签的问题列表
     * 
     * @param page 页码（从 1 开始）
     * @param pageSize 每页数量（最大 100）
     * @param sort 排序方式：activity, votes, creation, relevance
     * @param order 排序顺序：desc, asc
     * @param fromDate Unix 时间戳（可选）
     * @param toDate Unix 时间戳（可选）
     * @return API 响应 JSON
     */
    public ApiResponse fetchQuestions(int page, int pageSize, String sort, String order, 
                                     Long fromDate, Long toDate) throws IOException, InterruptedException {
        List<String> params = new ArrayList<>();
        params.add("tagged=java");
        params.add("site=" + SITE);
        params.add("page=" + page);
        params.add("pagesize=" + Math.min(pageSize, 100));
        params.add("sort=" + sort);
        params.add("order=" + order);
        params.add("filter=withbody"); // 包含问题正文
        
        if (fromDate != null) {
            params.add("fromdate=" + fromDate);
        }
        if (toDate != null) {
            params.add("todate=" + toDate);
        }
        // 添加认证参数（key 和 access_token 都是可选的）
        if (apiKey != null && !apiKey.isEmpty()) {
            params.add("key=" + apiKey);
        }
        if (accessToken != null && !accessToken.isEmpty()) {
            params.add("access_token=" + accessToken);
        }

        String url = BASE_URL + "/questions?" + String.join("&", params);
        return executeRequest(url);
    }

    /**
     * 获取问题的所有回答
     */
    public ApiResponse fetchAnswers(List<Long> questionIds) throws IOException, InterruptedException {
        if (questionIds.isEmpty()) {
            return new ApiResponse(null, Map.of("items", List.of()), false, 0);
        }

        // API 最多支持 100 个 ID
        String ids = questionIds.stream()
                .limit(100)
                .map(String::valueOf)
                .reduce((a, b) -> a + ";" + b)
                .orElse("");

        List<String> params = new ArrayList<>();
        params.add("site=" + SITE);
        params.add("filter=withbody");
        params.add("order=desc");
        params.add("sort=votes");
        
        if (apiKey != null && !apiKey.isEmpty()) {
            params.add("key=" + apiKey);
        }
        if (accessToken != null && !accessToken.isEmpty()) {
            params.add("access_token=" + accessToken);
        }

        String url = BASE_URL + "/questions/" + ids + "/answers?" + String.join("&", params);
        return executeRequest(url);
    }

    /**
     * 获取问题的评论
     */
    public ApiResponse fetchQuestionComments(List<Long> questionIds) throws IOException, InterruptedException {
        if (questionIds.isEmpty()) {
            return new ApiResponse(null, Map.of("items", List.of()), false, 0);
        }

        String ids = questionIds.stream()
                .limit(100)
                .map(String::valueOf)
                .reduce((a, b) -> a + ";" + b)
                .orElse("");

        List<String> params = new ArrayList<>();
        params.add("site=" + SITE);
        params.add("filter=withbody");
        params.add("order=desc");
        params.add("sort=creation");
        
        if (apiKey != null && !apiKey.isEmpty()) {
            params.add("key=" + apiKey);
        }
        if (accessToken != null && !accessToken.isEmpty()) {
            params.add("access_token=" + accessToken);
        }

        String url = BASE_URL + "/questions/" + ids + "/comments?" + String.join("&", params);
        return executeRequest(url);
    }

    /**
     * 获取回答的评论
     */
    public ApiResponse fetchAnswerComments(List<Long> answerIds) throws IOException, InterruptedException {
        if (answerIds.isEmpty()) {
            return new ApiResponse(null, Map.of("items", List.of()), false, 0);
        }

        String ids = answerIds.stream()
                .limit(100)
                .map(String::valueOf)
                .reduce((a, b) -> a + ";" + b)
                .orElse("");

        List<String> params = new ArrayList<>();
        params.add("site=" + SITE);
        params.add("filter=withbody");
        params.add("order=desc");
        params.add("sort=creation");
        
        if (apiKey != null && !apiKey.isEmpty()) {
            params.add("key=" + apiKey);
        }
        if (accessToken != null && !accessToken.isEmpty()) {
            params.add("access_token=" + accessToken);
        }

        String url = BASE_URL + "/answers/" + ids + "/comments?" + String.join("&", params);
        return executeRequest(url);
    }

    /**
     * 执行 HTTP 请求，处理速率限制和错误重试
     */
    private ApiResponse executeRequest(String url) throws IOException, InterruptedException {
        // 处理 backoff
        if (backoffSeconds > 0) {
            log.warn("Waiting {} seconds due to backoff", backoffSeconds);
            TimeUnit.SECONDS.sleep(backoffSeconds);
            backoffSeconds = 0;
        }

        // 速率限制：确保请求间隔
        long now = System.currentTimeMillis();
        long timeSinceLastRequest = now - lastRequestTime;
        if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
            Thread.sleep(MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest);
        }
        lastRequestTime = System.currentTimeMillis();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        int maxRetries = 3;
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < maxRetries) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                // 检查 HTTP 状态码
                if (response.statusCode() == 429) {
                    // 速率限制
                    String retryAfter = response.headers().firstValue("retry-after").orElse("60");
                    backoffSeconds = Integer.parseInt(retryAfter);
                    log.warn("Rate limit reached. Backoff: {} seconds", backoffSeconds);
                    Thread.sleep(backoffSeconds * 1000L);
                    backoffSeconds = 0;
                    retryCount++;
                    continue;
                }

                if (response.statusCode() != 200) {
                    throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
                }

                JsonNode root = objectMapper.readTree(response.body());
                
                // 提取配额信息
                if (root.has("quota_remaining")) {
                    quotaRemaining = root.path("quota_remaining").asInt();
                }
                if (root.has("backoff")) {
                    backoffSeconds = root.path("backoff").asInt();
                }

                // 检查错误
                if (root.has("error_id")) {
                    int errorId = root.path("error_id").asInt();
                    String errorMessage = root.path("error_message").asText("Unknown error");
                    throw new IOException("API Error " + errorId + ": " + errorMessage);
                }

                List<JsonNode> items = new ArrayList<>();
                if (root.has("items") && root.path("items").isArray()) {
                    root.path("items").forEach(items::add);
                }

                boolean hasMore = root.path("has_more").asBoolean(false);
                
                log.debug("API request successful. Items: {}, Has more: {}, Quota remaining: {}", 
                         items.size(), hasMore, quotaRemaining);

                return new ApiResponse(root, Map.of("items", items), hasMore, quotaRemaining);

            } catch (IOException | InterruptedException e) {
                lastException = e;
                retryCount++;
                
                if (retryCount < maxRetries) {
                    long waitTime = (long) Math.pow(2, retryCount) * 1000; // 指数退避
                    log.warn("Request failed (attempt {}/{}), retrying in {} ms: {}", 
                            retryCount, maxRetries, waitTime, e.getMessage());
                    Thread.sleep(waitTime);
                }
            }
        }

        throw new IOException("Failed after " + maxRetries + " attempts", lastException);
    }

    public int getQuotaRemaining() {
        return quotaRemaining;
    }

    /**
     * API 响应封装
     */
    public static class ApiResponse {
        private final JsonNode rootNode;
        private final Map<String, Object> metadata;
        private final boolean hasMore;
        private final int quotaRemaining;

        public ApiResponse(JsonNode rootNode, Map<String, Object> metadata, boolean hasMore, int quotaRemaining) {
            this.rootNode = rootNode;
            this.metadata = metadata;
            this.hasMore = hasMore;
            this.quotaRemaining = quotaRemaining;
        }

        @SuppressWarnings("unchecked")
        public List<JsonNode> getItems() {
            return (List<JsonNode>) metadata.getOrDefault("items", List.of());
        }

        public JsonNode getRootNode() {
            return rootNode;
        }

        public boolean hasMore() {
            return hasMore;
        }

        public int getQuotaRemaining() {
            return quotaRemaining;
        }
    }
}

