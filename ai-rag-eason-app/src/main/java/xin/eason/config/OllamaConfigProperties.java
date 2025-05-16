package xin.eason.config;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ollama 模型配置属性类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties("spring.ai.ollama")
public class OllamaConfigProperties {
    /**
     * 根 URL
     */
    private String baseUrl;
    /**
     * 嵌入参数
     */
    private Embed embedding;

    /**
     * 嵌入参数对象
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Embed {
        /**
         * 配置选项
         */
        private EmbedOptions options;
        /**
         * 模型名称
         */
        private String model;


    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmbedOptions {
        /**
         * Batch 数量
         */
        private Integer numBatch;
    }
}
