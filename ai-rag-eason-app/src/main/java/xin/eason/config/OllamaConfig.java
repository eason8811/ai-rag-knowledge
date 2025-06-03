package xin.eason.config;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.document.splitter.DocumentByWordSplitter;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.OllamaEmbeddingClient;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiEmbeddingClient;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

/**
 * Ollama 模型配置注入类
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(OllamaConfigProperties.class)
public class OllamaConfig {

    /**
     * @see OllamaConfigProperties
     */
    private final OllamaConfigProperties ollamaConfigProperties;

    /**
     * 从配置文件读取 base-url, 然后新建并注入 {@link  OllamaApi} 对象
     *
     * @return {@link  OllamaApi} 的 Bean 对象
     */
    @Bean
    public OllamaApi ollamaApi() {
        return new OllamaApi(ollamaConfigProperties.getBaseUrl());
    }

    /**
     * 根据上面注入的 {@link  OllamaApi} 对象, 新建并注入 {@link  OllamaChatClient} 对象
     *
     * @param ollamaApi 注入的 {@link  OllamaApi} 的 Bean 对象
     * @return {@link  OllamaChatClient} 的 Bean 对象
     */
    @Bean
    public OllamaChatClient ollamaChatClient(OllamaApi ollamaApi) {
        return new OllamaChatClient(ollamaApi);
    }

    /**
     * 注入 文本分割器对象
     *
     * @return 文本分割器 Bean
     */
    @Bean
    public DocumentSplitter textSegment() {
        return new DocumentBySentenceSplitter(1000, 200);
    }

    /**
     * 注入 {@link SimpleVectorStore} 简易向量存储器对象, 用于本地存储向量信息 ( 嵌入模型: nomic-embed-text )
     *
     * @param ollamaApi {@link  OllamaApi} 的 Bean 对象
     * @return {@link SimpleVectorStore} Bean 对象
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.rag", name = "embed", havingValue = "nomic-embed-text", matchIfMissing = true)
    public SimpleVectorStore simpleVectorStoreOllamaAi(OllamaApi ollamaApi) {
        OllamaEmbeddingClient ollamaEmbeddingClient = new OllamaEmbeddingClient(ollamaApi);
        ollamaEmbeddingClient.withDefaultOptions(OllamaOptions.create().withModel("nomic-embed-text"));
        return new SimpleVectorStore(ollamaEmbeddingClient);
    }

    /**
     * 注入 {@link SimpleVectorStore} 简易向量存储器对象, 用于本地存储向量信息 ( 嵌入模型: text-embedding-ada-002 )
     *
     * @param openAiApi {@link  OpenAiApi} 的 Bean 对象
     * @return {@link SimpleVectorStore} Bean 对象
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.rag", name = "embed", havingValue = "text-embedding-ada-002")
    public SimpleVectorStore simpleVectorStoreOpenAi(OpenAiApi openAiApi) {
        OpenAiEmbeddingClient openAiEmbeddingClient = new OpenAiEmbeddingClient(openAiApi);
        return new SimpleVectorStore(openAiEmbeddingClient);
    }

    /**
     * 注入 {@link PgVectorStore} 可使用数据库存储的向量储存器对象 ( 嵌入模型: nomic-embed-text )
     *
     * @param ollamaApi    {@link  OllamaApi} 的 Bean 对象
     * @param jdbcTemplate 用于调用数据库的对象
     * @return {@link PgVectorStore} Bean 对象
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.rag", name = "embed", havingValue = "nomic-embed-text", matchIfMissing = true)
    public PgVectorStore pgVectorStoreOllamaAi(OllamaApi ollamaApi, JdbcTemplate jdbcTemplate) {
        OllamaEmbeddingClient ollamaEmbeddingClient = new OllamaEmbeddingClient(ollamaApi);
        ollamaEmbeddingClient.withDefaultOptions(OllamaOptions.create().withModel("nomic-embed-text"));
        return new PgVectorStore(jdbcTemplate, ollamaEmbeddingClient);
    }

    /**
     * 注入 {@link PgVectorStore} 可使用数据库存储的向量储存器对象 ( 嵌入模型: text-embedding-ada-002 )
     *
     * @param openAiApi    {@link  OpenAiApi} 的 Bean 对象
     * @param jdbcTemplate 用于调用数据库的对象
     * @return {@link PgVectorStore} Bean 对象
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.ai.rag", name = "embed", havingValue = "text-embedding-ada-002")
    public PgVectorStore pgVectorStoreOpenAi(OpenAiApi openAiApi, JdbcTemplate jdbcTemplate) {
        OpenAiEmbeddingClient openAiEmbeddingClient = new OpenAiEmbeddingClient(openAiApi);
        return new PgVectorStore(jdbcTemplate, openAiEmbeddingClient);
    }

}
