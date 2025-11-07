package com.flexlease.user.integration;

import com.flexlease.common.exception.BusinessException;
import com.flexlease.common.exception.ErrorCode;
import com.flexlease.user.config.AuthServiceProperties;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);

    private final RestClient restClient;
    private final AuthServiceProperties properties;

    public AuthServiceClient(RestClient.Builder builder, AuthServiceProperties properties) {
        this.restClient = builder
                .baseUrl(properties.getBaseUrl())
                .build();
        this.properties = properties;
    }

    public void activateAccount(UUID userId) {
        updateAccountStatus(userId, "ENABLED");
    }

    public void updateAccountStatus(UUID userId, String status) {
        try {
            restClient.patch()
                    .uri("/api/v1/internal/users/{id}/status", userId)
                    .header("X-Internal-Token", properties.getInternalToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("status", status))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("Failed to update account {} status to {}", userId, status, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "更新账号状态失败，请稍后重试");
        }
    }

    public void assignVendor(UUID userId, UUID vendorId) {
        try {
            restClient.patch()
                    .uri("/api/v1/internal/users/{id}/vendor", userId)
                    .header("X-Internal-Token", properties.getInternalToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("vendorId", vendorId))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("Failed to associate account {} with vendor {}", userId, vendorId, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "同步厂商账号映射失败，请稍后重试");
        }
    }
}
