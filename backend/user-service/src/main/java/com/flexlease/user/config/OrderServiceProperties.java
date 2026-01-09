package com.flexlease.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 订单服务（order-service）调用配置。
 * <p>
 * 目前主要用于计算厂商 SLA 等运营指标（user-service 在调度器中触发）。
 */
@Component
@ConfigurationProperties(prefix = "flexlease.order-service")
public class OrderServiceProperties {

    private String baseUrl = "http://order-service";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        if (baseUrl != null && !baseUrl.isBlank()) {
            this.baseUrl = baseUrl;
        }
    }
}
