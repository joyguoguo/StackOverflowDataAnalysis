package cs209a.finalproject_demo.collector;

import cs209a.finalproject_demo.collector.client.StackOverflowApiClient;
import cs209a.finalproject_demo.collector.config.CollectionConfig;
import cs209a.finalproject_demo.collector.service.DataCollectorService;
import cs209a.finalproject_demo.collector.saver.ThreadDataSaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * 数据采集命令行工具
 * 使用方式：
 *   java -jar app.jar --collect.count=1000 --collect.output=Sample_SO_data
 *   或者设置环境变量后运行
 */
/**
 * 默认禁用，只有激活 profile "collector" 时才会加载并触发采集。
 */
@Profile("collector")
@SpringBootApplication
@Import(CollectionConfig.class)
public class DataCollectionRunner {

    private static final Logger log = LoggerFactory.getLogger(DataCollectionRunner.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DataCollectionRunner.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        app.run(args);
    }

    @Bean
    public CommandLineRunner collectorCommandLineRunner(
            StackOverflowApiClient apiClient,
            ThreadDataSaver dataSaver) {
        return args -> {
            log.info("=== Stack Overflow Java Thread Data Collector ===");
            log.info("This tool will collect Java-related threads from Stack Overflow API");
            log.info("================================================");

            // 从系统属性或环境变量读取配置
            String countStr = System.getProperty("collect.count", 
                    System.getenv().getOrDefault("COLLECT_COUNT", "1000"));
            String outputDir = System.getProperty("collect.output",
                    System.getenv().getOrDefault("COLLECT_OUTPUT", "Sample_SO_data"));
            String accessToken = System.getProperty("collect.token",
                    System.getenv().getOrDefault("SO_ACCESS_TOKEN", ""));
            String fromDateStr = System.getProperty("collect.from",
                    System.getenv().getOrDefault("COLLECT_FROM", null));
            String toDateStr = System.getProperty("collect.to",
                    System.getenv().getOrDefault("COLLECT_TO", null));

            int targetCount;
            try {
                targetCount = Integer.parseInt(countStr);
                if (targetCount < 1) {
                    log.error("Invalid count: {}. Must be at least 1.", targetCount);
                    return;
                }
            } catch (NumberFormatException e) {
                log.error("Invalid count format: {}. Must be a number.", countStr);
                return;
            }

            // 解析日期范围
            Long fromDate = parseDate(fromDateStr);
            Long toDate = parseDate(toDateStr);

            log.info("Configuration:");
            log.info("  Target count: {}", targetCount);
            log.info("  Output directory: {}", outputDir);
            log.info("  Date range: {} to {}", 
                    fromDate != null ? formatDate(fromDate) : "default",
                    toDate != null ? formatDate(toDate) : "default");
            log.info("  Access token: {}", accessToken.isEmpty() ? "not set" : "set");

            // 创建 API 客户端
            StackOverflowApiClient client = accessToken.isEmpty() 
                    ? new StackOverflowApiClient() 
                    : new StackOverflowApiClient(accessToken);

            // 创建服务
            DataCollectorService collectorService = new DataCollectorService(client, dataSaver);

            // 开始采集
            log.info("\nStarting collection...\n");
            DataCollectorService.CollectionResult result = collectorService.collectThreads(
                    targetCount, outputDir, fromDate, toDate);

            // 输出结果
            log.info("\n=== Collection Summary ===");
            log.info("Total collected: {}", result.getTotalCollected());
            log.info("Successful: {}", result.getSuccessCount());
            log.info("Failed: {}", result.getFailureCount());
            log.info("Quota remaining: {}", result.getQuotaRemaining());
            log.info("Duration: {} seconds", result.getDurationSeconds());
            
            if (!result.getErrors().isEmpty()) {
                log.warn("\nErrors encountered (showing first 10):");
                result.getErrors().stream()
                        .limit(10)
                        .forEach(error -> log.warn("  - {}", error));
                if (result.getErrors().size() > 10) {
                    log.warn("  ... and {} more errors", result.getErrors().size() - 10);
                }
            }

            log.info("\nCollection completed! Data saved to: {}", outputDir);
        };
    }

    private Long parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        try {
            // 支持多种日期格式
            if (dateStr.matches("\\d{10}")) {
                // Unix 时间戳（秒）
                return Long.parseLong(dateStr);
            } else if (dateStr.matches("\\d{13}")) {
                // Unix 时间戳（毫秒）
                return Long.parseLong(dateStr) / 1000;
            } else {
                // ISO 日期格式：YYYY-MM-DD
                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
                return date.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
            }
        } catch (Exception e) {
            log.warn("Failed to parse date '{}': {}. Using default.", dateStr, e.getMessage());
            return null;
        }
    }

    private String formatDate(long timestamp) {
        return Instant.ofEpochSecond(timestamp).toString();
    }
}

