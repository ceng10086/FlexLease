package com.flexlease.user.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 通知服务（notification-service）调用配置。
 * <p>
 * user-service 通过通知服务发送信用分变更、账号解冻等站内信提醒。
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
