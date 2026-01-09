package com.flexlease.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 外部 LLM 调用抽象（DeepSeek OpenAI 兼容接口）。
 * <p>
 * 目前仅用于“纠纷仲裁建议”生成场景；正常业务流程不依赖外网，未配置 Key 时会走离线模板兜底。
 */
public interface DeepSeekChatClient {

    String createChatCompletion(List<ChatMessage> messages);

    record ChatMessage(String role, String content) {
    }

    record ResponseFormat(String type) {
    }

    record ChatCompletionRequest(String model,
                                 List<ChatMessage> messages,
                                 ResponseFormat response_format,
                                 Double temperature,
                                 Integer max_tokens) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatCompletionResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(ChatMessage message) {
    }
}
