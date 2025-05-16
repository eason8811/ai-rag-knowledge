package xin.eason.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ollama 模型配置注入类
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(OllamaConfigProperties.class)
public class OllamaConfig {

    private final OllamaConfigProperties ollamaConfigProperties;

    /**
     * 从配置文件读取 base-url, 然后新建并注入 {@link  OllamaApi} 对象
     * @return {@link  OllamaApi} 的 Bean 对象
     */
    @Bean
    public OllamaApi ollamaApi() {
        return new OllamaApi(ollamaConfigProperties.getBaseUrl());
    }

    /**
     * 根据上面注入的 {@link  OllamaApi} 对象, 新建并注入 {@link  OllamaChatClient} 对象
     * @param ollamaApi 注入的 {@link  OllamaApi} 的 Bean 对象
     * @return {@link  OllamaChatClient} 的 Bean 对象
     */
    @Bean
    public OllamaChatClient ollamaChatClient(OllamaApi ollamaApi) {
        return new OllamaChatClient(ollamaApi);
    }
}
