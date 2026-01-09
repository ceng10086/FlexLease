package com.flexlease.payment.client;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * user-service 访问配置。
 *
 * <p>支付服务通过它读取厂商抽成档案（commission profile）。</p>
 */
@Validated
@ConfigurationProperties(prefix = "flexlease.user-service")
public class UserServiceProperties {

    @NotBlank
    private String baseUrl = "http://user-service/api/v1";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
