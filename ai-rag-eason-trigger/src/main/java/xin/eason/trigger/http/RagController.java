package xin.eason.trigger.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaEmbeddingClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.core.io.PathResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import xin.eason.api.IRagService;
import xin.eason.api.response.Result;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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
            if (ragTagRList.contains(ragTag)) {
                return Result.error("该 RagTag 已经存在!");
            }
            ragTagRList.add(ragTag);
        }
        log.info("上传已完成!");
        return Result.success("上传成功!");
    }

    /**
     * 根据传入的仓库 URL 和用户名, Token 克隆 Git 仓库, 然后上传知识库
     *
     * @param repositoryUrl 仓库 URL
     * @param userName      用户名
     * @param token         用户 Token
     * @return 分析结果
     */
    @Override
    @PostMapping("/analyze_git_repository")
    public Result<String> analyseGitRepository(String repositoryUrl, String userName, String token) {
        String repositoryName = getRepositoryName(repositoryUrl);
        if (redissonClient.getList("knowledge").contains(repositoryName))
            return Result.error("代码仓库已存在在知识库中!");

        String tempCloneRepositoryPath = "./temp-clone-repository";
        File localRepositoryFile = new File(tempCloneRepositoryPath);
        try {
            log.info("正在清理临时本地仓库文件夹...");
            FileUtils.deleteDirectory(localRepositoryFile);
        } catch (IOException e) {
            log.error("清理本地仓库文件夹错误! 目录位置: {}", localRepositoryFile.getPath(), e);
            return Result.error("清理本地仓库文件夹错误! 目录位置: " + localRepositoryFile.getPath());
        }
        try (Git git = Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(localRepositoryFile)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, token))
                .call()) {
            log.info("克隆完毕! 仓库 URL: {}", repositoryUrl);
        } catch (GitAPIException e) {
            log.error("连接 GitHub 仓库失败, URL: {}", repositoryUrl, e);
            return Result.error("连接 GitHub 仓库失败, URL: " + repositoryUrl);
        }

        // 遍历 临时本地仓库文件夹 中的文件, 并上传知识库
        try {
            Path gitHandlerPath = Paths.get(tempCloneRepositoryPath).resolve(".git");
            Files.walkFileTree(Paths.get(localRepositoryFile.toURI()), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    log.info("正在上传知识库文件 {} ...", file);
                    FileVisitResult originalSuccessResult = super.visitFile(file, attrs);
                    if (file.startsWith(gitHandlerPath))
                        return originalSuccessResult;
                    TikaDocumentReader reader = new TikaDocumentReader(new PathResource(file));
                    List<Document> documentList = reader.get();
                    List<Document> splitDocumentList = tokenTextSplitter.apply(documentList);
                    splitDocumentList.forEach(document -> document.getMetadata().put("knowledge", repositoryName));
                    pgVectorStore.add(splitDocumentList);
                    return originalSuccessResult;
                }
            });

            // 上传完成后上报 Redis
            RList<String> knowledgeRlist = redissonClient.getList("knowledge");
            if (knowledgeRlist.contains(repositoryName))
                return Result.error("代码仓库已存在在知识库中!");
            knowledgeRlist.add(repositoryName);

        } catch (IOException e) {
            log.error("遍历本地临时文件并上传知识库时出错!", e);
            return Result.error("遍历本地临时文件并上传知识库时出错!");
        }
        return Result.success("代码仓库 \"" + repositoryName + "\" 已经上传到知识库");
    }

    private String getRepositoryName(String repositoryUrl) {
        String[] uriSplitArray = repositoryUrl.split("/");
        return uriSplitArray[uriSplitArray.length - 1].replace(".git", "");
    }
}
