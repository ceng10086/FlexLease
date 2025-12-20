package com.flexlease.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

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
