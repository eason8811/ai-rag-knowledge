package xin.eason.trigger.http;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import xin.eason.api.IAiService;

@RequiredArgsConstructor
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/ollama")
public class OllamaController implements IAiService {

    private final OllamaChatClient ollamaChatClient;

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
        return ollamaChatClient.call(new Prompt(message, OllamaOptions.create().withModel(model)));
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
        return ollamaChatClient.stream(new Prompt(message, OllamaOptions.create().withModel(model)));
    }


}
