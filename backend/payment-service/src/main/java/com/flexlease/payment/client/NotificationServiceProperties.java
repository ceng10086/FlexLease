package com.flexlease.payment.client;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * notification-service 访问配置。
 *
 * <p>默认使用服务名访问（配合 Eureka + LoadBalanced RestTemplate）。</p>
 */
@Validated
@ConfigurationProperties(prefix = "flexlease.notification-service")
public class NotificationServiceProperties {

    @NotBlank
    private String baseUrl = "http://notification-service/api/v1";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
