package com.flexlease.product.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 通知服务（notification-service）调用配置。
 * <p>
 * 主要用于商品咨询等场景向厂商/用户发送站内信通知。
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
