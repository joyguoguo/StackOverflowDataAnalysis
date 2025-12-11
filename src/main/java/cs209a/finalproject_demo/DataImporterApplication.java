package cs209a.finalproject_demo;

import cs209a.finalproject_demo.service.DataImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * 独立的数据导入工具
 * 只执行数据导入，不启动 Web 服务器
 *
 * 使用方式：
 *   java -jar app.jar --import.directory=Sample_SO_data
 *   或
 *   mvnw exec:java -Dexec.mainClass="cs209a.finalproject_demo.DataImporterApplication" -Dexec.args="--import.directory=Sample_SO_data"
 */
@SpringBootApplication
public class DataImporterApplication {

    private static final Logger log = LoggerFactory.getLogger(DataImporterApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(DataImporterApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE); // 不启动 Web 服务器
        app.run(args);
    }

    @Bean
    public CommandLineRunner importRunner(DataImportService importService) {
        return args -> {
            // 从命令行参数、系统属性或环境变量读取目录
            String directory = "Sample_SO_data"; // 默认值

            // 1. 先检查命令行参数 (--import.directory=xxx)
            for (String arg : args) {
                if (arg.startsWith("--import.directory=")) {
                    directory = arg.substring("--import.directory=".length());
                    break;
                }
            }

            // 2. 检查系统属性
            if (directory.equals("Sample_SO_data")) {
                String propDir = System.getProperty("import.directory");
                if (propDir != null && !propDir.isEmpty()) {
                    directory = propDir;
                }
            }

            // 3. 检查环境变量
            if (directory.equals("Sample_SO_data")) {
                String envDir = System.getenv("IMPORT_DIRECTORY");
                if (envDir != null && !envDir.isEmpty()) {
                    directory = envDir;
                }
            }

            log.info("========================================");
            log.info("Stack Overflow Data Importer");
            log.info("========================================");
            log.info("Import directory: {}", directory);
            log.info("========================================\n");

            // 每次导入前先清空现有数据，保证库与目录一致
            importService.clearAllData();

            DataImportService.ImportResult result = importService.importFromDirectory(directory);

            log.info("\n========================================");
            log.info("Import Summary");
            log.info("========================================");
            log.info("Success: {}", result.getSuccessCount());
            log.info("Failed: {}", result.getFailedCount());
            log.info("Skipped: {}", result.getSkippedCount());

            if (!result.getErrors().isEmpty()) {
                log.warn("\nErrors encountered (showing first 10):");
                result.getErrors().stream()
                        .limit(10)
                        .forEach(error -> log.warn("  - {}", error));
                if (result.getErrors().size() > 10) {
                    log.warn("  ... and {} more errors", result.getErrors().size() - 10);
                }
            }

            log.info("\n========================================");
            log.info("Import completed!");
            log.info("========================================");
        };
    }
}

