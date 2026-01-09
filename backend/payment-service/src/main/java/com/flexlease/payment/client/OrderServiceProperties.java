package com.flexlease.payment.client;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * order-service 访问配置。
 *
 * <p>用于支付服务回调订单服务内部接口。</p>
 */
@Validated
@ConfigurationProperties(prefix = "flexlease.order-service")
public class OrderServiceProperties {

    @NotBlank
    private String baseUrl = "http://order-service/api/v1";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
