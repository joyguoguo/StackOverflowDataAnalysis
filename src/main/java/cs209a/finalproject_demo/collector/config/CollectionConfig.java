package cs209a.finalproject_demo.collector.config;

import cs209a.finalproject_demo.collector.client.StackOverflowApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据采集模块配置
 * 注意：ThreadDataSaver 使用 @Component 注解，会自动注册为 bean，不需要在这里定义
 */
@Configuration
public class CollectionConfig {

    @Bean
    public StackOverflowApiClient stackOverflowApiClient(
            @Value("${so.api.access-token:}") String accessToken,
            @Value("${so.api.key:}") String apiKey) {
        if (!apiKey.isEmpty()) {
            return new StackOverflowApiClient(accessToken.isEmpty() ? null : accessToken, apiKey);
        }
        return accessToken.isEmpty() 
                ? new StackOverflowApiClient() 
                : new StackOverflowApiClient(accessToken);
    }
}




