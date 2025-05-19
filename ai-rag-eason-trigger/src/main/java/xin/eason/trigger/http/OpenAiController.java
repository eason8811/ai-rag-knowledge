package xin.eason.trigger.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import xin.eason.api.IAiService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/openai")
@RequiredArgsConstructor
public class OpenAiController implements IAiService {

    /**
     * OpenAI 的对话客户端
     */
    private final OpenAiChatClient openAiChatClient;

    /**
     * PostgreSQL 向量库
     */
    private final PgVectorStore pgVectorStore;

    /**
     * 根据消息生成响应结果
     *
     * @param model   需要使用的模型
     * @param message 发送的消息
     * @return 返回 Spring AI 对话响应对象 ( 非流式传输 )
     */
    @Override
    @GetMapping("/generate")
    public ChatResponse generate(String model, String message) {
        return openAiChatClient.call(new Prompt(message, OpenAiChatOptions.builder().withModel(model).build()));
    }

    /**
     * 根据消息生成响应结果
     *
     * @param model   需要使用的模型
     * @param message 发送的消息
     * @return 返回 Spring AI 对话响应对象 ( 流式传输 )
     */
    @Override
    @GetMapping("/generate_stream")
    public Flux<ChatResponse> generateStream(String model, String message) {
        log.info("正在与 {} 对话...  用户信息为: {}", model, message);
        return openAiChatClient.stream(new Prompt(message, OpenAiChatOptions.builder().withModel(model).build()));
    }

    /**
     * 根据选定的 知识库Tag 流式生成消息的响应结果
     *
     * @param model   需要使用的模型
     * @param ragTag  知识库 Tag
     * @param message 发送的消息
     * @return 返回 Spring AI 对话响应对象 ( 流式传输 )
     */
    @Override
    @GetMapping("/generate_stream_rag")
    public Flux<ChatResponse> generateStreamWithRag(String model, String ragTag, String message) {
        String SYSTEM_PROMPT = """
                Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
                If unsure, simply state that you don't know.
                Another thing you need to note is that your reply must be in Chinese!
                DOCUMENTS:
                    {documents}
                """;
        SearchRequest searchRequest = SearchRequest.query(message).withTopK(31).withFilterExpression("knowledge == '" + ragTag + "'");
        List<Document> documents = pgVectorStore.similaritySearch(searchRequest);
        String documentString = documents.stream().map(Document::getContent).collect(Collectors.joining());
        Message systemMsg = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentString));
        Message userMessage = new UserMessage(message);

        List<Message> messageList = new ArrayList<>();
        messageList.add(systemMsg);
        messageList.add(userMessage);
        return openAiChatClient.stream(new Prompt(messageList, OpenAiChatOptions.builder().withModel(model).build()));
    }
}
