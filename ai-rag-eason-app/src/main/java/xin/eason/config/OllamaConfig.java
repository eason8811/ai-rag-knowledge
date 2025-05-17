package xin.eason.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.OllamaEmbeddingClient;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

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

    /**
     * 注入 Token 文本分割器对象
     * @return Token 文本分割器 Bean
     */
    @Bean
    public TokenTextSplitter textSplitter() {
        return new TokenTextSplitter();
    }

    /**
     * 注入 {@link SimpleVectorStore} 简易向量存储器对象, 用于本地存储向量信息
     * @param ollamaApi {@link  OllamaApi} 的 Bean 对象
     * @return {@link SimpleVectorStore} Bean 对象
     */
    @Bean
    public SimpleVectorStore simpleVectorStore(OllamaApi ollamaApi) {
        OllamaEmbeddingClient ollamaEmbeddingClient = new OllamaEmbeddingClient(ollamaApi);
        ollamaEmbeddingClient.withDefaultOptions(OllamaOptions.create().withModel("nomic-embed-text"));
        return new SimpleVectorStore(ollamaEmbeddingClient);
    }

    /**
     * 注入 {@link PgVectorStore} 可使用数据库存储的向量储存器对象
     * @param ollamaApi {@link  OllamaApi} 的 Bean 对象
     * @param jdbcTemplate 用于调用数据库的对象
     * @return {@link PgVectorStore} Bean 对象
     */
    @Bean
    public PgVectorStore pgVectorStore(OllamaApi ollamaApi, JdbcTemplate jdbcTemplate) {
        OllamaEmbeddingClient ollamaEmbeddingClient = new OllamaEmbeddingClient(ollamaApi);
        ollamaEmbeddingClient.withDefaultOptions(OllamaOptions.create().withModel("nomic-embed-text"));
        return new PgVectorStore(jdbcTemplate, ollamaEmbeddingClient);
    }


}
