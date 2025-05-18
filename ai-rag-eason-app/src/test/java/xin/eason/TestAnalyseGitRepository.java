package xin.eason;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.PathResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class TestAnalyseGitRepository {

    @Autowired
    private TokenTextSplitter tokenTextSplitter;
    @Autowired
    private PgVectorStore pgVectorStore;

    @Test
    public void testCloneRepository() throws GitAPIException, IOException {
        String repositoryUri = "https://github.com/eason8811/ai-rag-knowledge";
        String localRepositoryPath = "./temp-clone-repository";
        File localRepositoryFile = new File(localRepositoryPath);
        String username = "eason8811";
        String token = "***";

        FileUtils.deleteDirectory(localRepositoryFile);     // 避免重复

        // 克隆 git 仓库
        Git git = Git.cloneRepository()
                .setURI(repositoryUri)
                .setDirectory(localRepositoryFile)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, token))
                .call();
        log.info("克隆完成, 本地仓库地址为: {}", localRepositoryFile.toString());
        git.close();
    }

    @Test
    public void testUploadRepositoryFiles() throws IOException {
        String localRepositoryPath = "./temp-clone-repository";

        Files.walkFileTree(Paths.get(localRepositoryPath), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().contains("\\.git\\"))
                    return FileVisitResult.CONTINUE;

                String fileName = file.getFileName().toString();
                log.info("正在上传 {} ...", file.toString());

                PathResource resource = new PathResource(file);
                TikaDocumentReader reader = new TikaDocumentReader(resource);
                List<Document> documents = reader.get();
                List<Document> splitDocument = tokenTextSplitter.apply(documents);
                splitDocument.forEach(document -> document.getMetadata().put("knowledge", getRepositoryName("https://github.com/eason8811/ai-rag-knowledge")));
                pgVectorStore.add(splitDocument);

                return FileVisitResult.CONTINUE;
            }
        });
    }

    private String getRepositoryName(String repositoryUri) {
        String[] uriSplitArray = repositoryUri.split("/");
        return uriSplitArray[uriSplitArray.length - 1].replace(".git", "");
    }

    @Test
    public void deletePgVectorStoreKnowledge() {

    }
}
