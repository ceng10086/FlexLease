package com.flexlease.product.integration;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.notification.NotificationSendRequest;
import com.flexlease.common.security.JwtAuthProperties;
import com.flexlease.product.config.NotificationServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 通知服务客户端（商品服务 -> 通知服务）。
 * <p>
 * 通过站内信向厂商/用户发送咨询相关提醒；内部调用使用 {@code X-Internal-Token} 作为微服务互信凭证。
 */
@Component
public class NotificationClient {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationClient.class);

    private final RestTemplate restTemplate;
    private final NotificationServiceProperties properties;
    private final JwtAuthProperties jwtAuthProperties;

    public NotificationClient(RestTemplate restTemplate,
                              NotificationServiceProperties properties,
                              JwtAuthProperties jwtAuthProperties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.jwtAuthProperties = jwtAuthProperties;
    }

    public void send(NotificationSendRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Token", jwtAuthProperties.getInternalAccessToken());
            ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
                    properties.getBaseUrl() + "/notifications/send",
                    new HttpEntity<>(request, headers),
                    ApiResponse.class
            );
            ApiResponse body = response.getBody();
            if (body != null && body.code() != ErrorCode.SUCCESS.code()) {
                LOG.warn("Notification service responded with error: {}", body.message());
            }
        } catch (RestClientException ex) {
            LOG.warn("Failed to send notification via {}: {}", properties.getBaseUrl(), ex.getMessage());
        }
    }
}
