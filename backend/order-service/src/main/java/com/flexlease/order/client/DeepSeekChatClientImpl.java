package com.flexlease.order.client;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.order.config.LlmProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class DeepSeekChatClientImpl implements DeepSeekChatClient {

    private static final Logger LOG = LoggerFactory.getLogger(DeepSeekChatClientImpl.class);

    private final RestTemplate restTemplate;
    private final LlmProperties properties;

    public DeepSeekChatClientImpl(@Qualifier("externalRestTemplate") RestTemplate restTemplate,
                                  LlmProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public String createChatCompletion(List<ChatMessage> messages) {
        if (!properties.isEnabled()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "LLM 功能未启用");
        }
        if (!StringUtils.hasText(properties.getApiKey())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "LLM API Key 未配置");
        }
        String url = properties.getBaseUrl();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        url = url + "/v1/chat/completions";

        ChatCompletionRequest requestBody = new ChatCompletionRequest(
                properties.getModel(),
                messages,
                new ResponseFormat("json_object"),
                0.2,
                1100
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey().trim());

        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ResponseEntity<ChatCompletionResponse> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        new HttpEntity<>(requestBody, headers),
                        ChatCompletionResponse.class
                );
                ChatCompletionResponse body = response.getBody();
                if (body == null || body.choices() == null || body.choices().isEmpty()) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 返回为空");
                }
                ChatMessage message = body.choices().getFirst().message();
                if (message == null || !StringUtils.hasText(message.content())) {
                    if (attempt == maxAttempts) {
                        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 返回内容为空");
                    }
                    LOG.warn("LLM returned empty content (attempt {}/{}), retrying...", attempt, maxAttempts);
                    continue;
                }
                return message.content();
            } catch (BusinessException ex) {
                throw ex;
            } catch (ResourceAccessException ex) {
                LOG.warn("LLM request failed (attempt {}/{}): {}", attempt, maxAttempts, ex.getMessage());
                if (attempt == maxAttempts) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 调用超时");
                }
            } catch (RestClientException ex) {
                LOG.warn("LLM request failed (attempt {}/{}): {}", attempt, maxAttempts, ex.getMessage());
                if (attempt == maxAttempts) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 调用失败");
                }
            } catch (Exception ex) {
                LOG.warn("LLM request failed (attempt {}/{}): {}", attempt, maxAttempts, ex.getMessage());
                if (attempt == maxAttempts) {
                    throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 调用失败");
                }
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM 调用失败");
    }
}
