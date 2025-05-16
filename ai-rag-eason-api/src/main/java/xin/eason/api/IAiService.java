package xin.eason.api;

import org.springframework.ai.chat.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * 对外提供的 AI 生成 API 接口, 用于 RPC 调用或其他调用, 调用者无需关心底层实现
 */
public interface IAiService {

    /**
     * 根据消息生成响应结果
     * @param model 需要使用的模型
     * @param message 发送的消息
     * @return 返回 Spring AI 对话响应对象 ( 非流式传输 )
     */
    ChatResponse generate(String model, String message);

    /**
     * 根据消息生成响应结果
     * @param model 需要使用的模型
     * @param message 发送的消息
     * @return 返回 Spring AI 对话响应对象 ( 流式传输 )
     */
    Flux<ChatResponse> generateStream(String model, String message);
}
