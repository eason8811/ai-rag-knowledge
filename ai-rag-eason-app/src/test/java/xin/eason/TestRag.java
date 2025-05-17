package xin.eason;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class TestRag {

    @Autowired
    private TokenTextSplitter tokenTextSplitter;

    @Autowired
    private PgVectorStore pgVectorStore;

    @Autowired
    private OllamaChatClient ollamaChatClient;

    @Test
    public void testUploadKnowledge() {
        // 使用 TikaDocumentReader 获取知识库里的信息
        TikaDocumentReader reader = new TikaDocumentReader("./data/knowledge.txt");
        List<Document> documents = reader.get();    // 得到 document 列表
        List<Document> splitDocuments = tokenTextSplitter.apply(documents);
        splitDocuments.forEach(document -> document.getMetadata().put("knowledge", "主机配置知识库"));
        pgVectorStore.add(splitDocuments);
        log.info("知识库已上传完成!");
    }

    @Test
    public void testRagChat() {
        String message = "Eason 的电脑配置是什么";

        String SYSTEM_PROMPT = """
                Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
                If unsure, simply state that you don't know.
                Another thing you need to note is that your reply must be in Chinese!
                DOCUMENTS:
                    {documents}
                """;

        SearchRequest searchRequest = SearchRequest.query(message).withTopK(5).withFilterExpression("knowledge == '主机配置知识库'");
        List<Document> documents = pgVectorStore.similaritySearch(searchRequest);
        String documentCollect = documents.stream().map(Document::getContent).collect(Collectors.joining());
        Message systemPromptMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentCollect));

        ArrayList<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(message));
        messages.add(systemPromptMessage);

        ChatResponse response = ollamaChatClient.call(new Prompt(messages, OllamaOptions.create().withModel("deepseek-r1:1.5b")));
        log.info("测试结果为: {}", response);
    }
}
