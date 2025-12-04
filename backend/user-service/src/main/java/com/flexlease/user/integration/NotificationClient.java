package com.flexlease.user.integration;

import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.common.notification.NotificationSendRequest;
import com.flexlease.common.security.JwtAuthProperties;
import com.flexlease.user.config.NotificationServiceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NotificationClient {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient restClient;
    private final NotificationServiceProperties properties;
    private final JwtAuthProperties jwtAuthProperties;

    public NotificationClient(RestClient.Builder builder,
                              NotificationServiceProperties properties,
                              JwtAuthProperties jwtAuthProperties) {
        this.restClient = builder
                .baseUrl(properties.getBaseUrl())
                .build();
        this.properties = properties;
        this.jwtAuthProperties = jwtAuthProperties;
    }

    public void send(NotificationSendRequest request) {
        try {
            restClient.post()
                    .uri("/notifications/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-Token", jwtAuthProperties.getInternalAccessToken())
                    .body(request)
                    .retrieve()
                    .body(ApiResponse.class);
        } catch (RestClientException ex) {
            LOG.warn("Failed to send notification via {}: {}", properties.getBaseUrl(), ex.getMessage());
        }
    }
}
