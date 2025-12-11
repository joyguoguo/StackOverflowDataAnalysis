package cs209a.finalproject_demo.collector;

import cs209a.finalproject_demo.collector.client.StackOverflowApiClient;
import cs209a.finalproject_demo.collector.service.DataCollectorService;
import cs209a.finalproject_demo.collector.saver.ThreadDataSaver;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 简单的数据采集工具（不依赖 Spring）
 * 可直接运行：java cs209a.finalproject_demo.collector.SimpleDataCollector
 */
public class SimpleDataCollector {

    public static void main(String[] args) {
        // 从命令行参数或环境变量读取配置
        String countStr = args.length > 0 ? args[0] : 
                System.getenv().getOrDefault("COLLECT_COUNT", "1000");
        String outputDir = args.length > 1 ? args[1] : 
                System.getenv().getOrDefault("COLLECT_OUTPUT", "Sample_SO_data");
        // 支持 API Key（优先）或 Access Token
        String apiKey = args.length > 2 ? args[2] : 
                System.getenv().getOrDefault("SO_API_KEY", "");
        String accessToken = System.getenv().getOrDefault("SO_ACCESS_TOKEN", "");

        int targetCount;
        try {
            targetCount = Integer.parseInt(countStr);
            if (targetCount < 1) {
                System.err.println("Error: Count must be at least 1. Got: " + targetCount);
                System.exit(1);
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid count format: " + countStr);
            System.exit(1);
            return;
        }

        System.out.println("=== Stack Overflow Java Thread Data Collector ===");
        System.out.println("Target count: " + targetCount);
        System.out.println("Output directory: " + outputDir);
        System.out.println("API Key: " + (apiKey.isEmpty() ? "not set" : "set"));
        System.out.println("Access Token: " + (accessToken.isEmpty() ? "not set" : "set"));
        System.out.println("================================================");

        // 创建组件：优先使用 API Key，如果没有则使用 Access Token
        StackOverflowApiClient apiClient;
        if (!apiKey.isEmpty()) {
            // 使用 API Key（推荐用于只读操作）
            apiClient = new StackOverflowApiClient(null, apiKey);
        } else if (!accessToken.isEmpty()) {
            // 使用 Access Token
            apiClient = new StackOverflowApiClient(accessToken, null);
        } else {
            // 不使用认证（配额较低）
            apiClient = new StackOverflowApiClient();
            System.out.println("Warning: No API Key or Access Token provided. Using unauthenticated mode (lower quota).");
        }
        
        ThreadDataSaver dataSaver = new ThreadDataSaver();
        DataCollectorService collectorService = new DataCollectorService(apiClient, dataSaver);

        // 开始采集
        System.out.println("\nStarting collection...\n");
        DataCollectorService.CollectionResult result = collectorService.collectThreads(
                targetCount, outputDir, null, null);

        // 输出结果
        System.out.println("\n=== Collection Summary ===");
        System.out.println("Total collected: " + result.getTotalCollected());
        System.out.println("Successful: " + result.getSuccessCount());
        System.out.println("Failed: " + result.getFailureCount());
        System.out.println("Quota remaining: " + result.getQuotaRemaining());
        System.out.println("Duration: " + result.getDurationSeconds() + " seconds");

        if (!result.getErrors().isEmpty()) {
            System.out.println("\nErrors encountered (showing first 10):");
            result.getErrors().stream()
                    .limit(10)
                    .forEach(error -> System.out.println("  - " + error));
            if (result.getErrors().size() > 10) {
                System.out.println("  ... and " + (result.getErrors().size() - 10) + " more errors");
            }
        }

        System.out.println("\nCollection completed! Data saved to: " + outputDir);
    }
}

