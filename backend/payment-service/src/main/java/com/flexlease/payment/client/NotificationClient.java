package com.flexlease.payment.client;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.notification.NotificationSendRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class NotificationClient {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationClient.class);
    private static final ParameterizedTypeReference<ApiResponse<Map<String, Object>>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public NotificationClient(RestTemplate restTemplate, NotificationServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.baseUrl = properties.getBaseUrl();
    }

    public void send(NotificationSendRequest request) {
        try {
            ResponseEntity<ApiResponse<Map<String, Object>>> response = restTemplate.exchange(
                    baseUrl + "/notifications/send",
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    RESPONSE_TYPE
            );
            ApiResponse<Map<String, Object>> body = response.getBody();
            if (body != null && body.code() != ErrorCode.SUCCESS.code()) {
                LOG.warn("Notification service responded with non-success code: {} - {}", body.code(), body.message());
            }
        } catch (RestClientException ex) {
            LOG.warn("Failed to send notification: {}", ex.getMessage());
        }
    }
}
