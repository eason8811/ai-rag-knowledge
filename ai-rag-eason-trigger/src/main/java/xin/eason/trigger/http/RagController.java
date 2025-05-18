package xin.eason.trigger.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaEmbeddingClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xin.eason.api.IRagService;
import xin.eason.api.response.Result;

import java.util.List;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
public class RagController implements IRagService {

    /**
     * Token 文本分割器
     */
    private final TokenTextSplitter tokenTextSplitter;

    /**
     * Ollama 嵌入客户端
     */
    private final OllamaEmbeddingClient ollamaEmbeddingClient;

    /**
     * PG 向量库
     */
    private final PgVectorStore pgVectorStore;

    /**
     * Redisson 客户端
     */
    private final RedissonClient redissonClient;

    /**
     * 查询已有的 RAG 知识库的 Tag 标签
     *
     * @return Tag 标签列表
     */
    @Override
    @GetMapping("/query_rag_tag_list")
    public Result<List<String>> queryRagTagList() {
        log.info("正在查询现存的 RAG Tag 列表");
        RList<String> ragTagRList = redissonClient.getList("ragTag");
        log.info("查询已完成! 结果为: {}", ragTagRList);
        return Result.success(ragTagRList);
    }

    /**
     * 上传 RAG 知识库文件
     *
     * @param ragTag 上传的 RAG 知识库的 Tag 标签
     * @param files  上传的一系列文件, 使用列表封装
     * @return 上传
     */
    @Override
    @PostMapping(path = "file/upload", headers = "content-type=multipart/form-data")
    public Result<String> uploadRagFiles(String ragTag, List<MultipartFile> files) {
        log.info("开始上传知识库文件");
        for (MultipartFile file : files) {
            log.info("正在上传 {}...", file.getOriginalFilename());
            TikaDocumentReader reader = new TikaDocumentReader(file.getResource());
            List<Document> documents = reader.get();
            List<Document> splitDocuments = tokenTextSplitter.apply(documents);
            splitDocuments.forEach(document -> document.getMetadata().put("knowledge", ragTag));
            pgVectorStore.add(splitDocuments);

            RList<Object> ragTagRList = redissonClient.getList("ragTag");
            if (!ragTagRList.contains(ragTag)) {
                ragTagRList.add(ragTag);
            }
        }
        log.info("上传已完成!");
        return Result.success("上传成功!");
    }
}
