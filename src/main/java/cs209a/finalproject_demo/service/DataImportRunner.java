package cs209a.finalproject_demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 数据导入命令行工具
 * 使用方式：java -jar app.jar --import.data=true --import.directory=Sample_SO_data
 */
@Component
@ConditionalOnProperty(name = "import.data", havingValue = "true")
public class DataImportRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataImportRunner.class);

    private final DataImportService importService;

    public DataImportRunner(DataImportService importService) {
        this.importService = importService;
    }

    @Override
    public void run(String... args) {
        String directory = System.getProperty("import.directory", 
                System.getenv().getOrDefault("IMPORT_DIRECTORY", "Sample_SO_data"));

        log.info("=== Starting Data Import ===");
        log.info("Import directory: {}", directory);

        DataImportService.ImportResult result = importService.importFromDirectory(directory);

        log.info("=== Import Summary ===");
        log.info("Success: {}", result.getSuccessCount());
        log.info("Failed: {}", result.getFailedCount());
        log.info("Skipped: {}", result.getSkippedCount());

        if (!result.getErrors().isEmpty()) {
            log.warn("Errors encountered (showing first 10):");
            result.getErrors().stream()
                    .limit(10)
                    .forEach(error -> log.warn("  - {}", error));
        }

        log.info("Import completed!");
    }
}






















