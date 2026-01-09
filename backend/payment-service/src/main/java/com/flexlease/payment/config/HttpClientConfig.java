package com.flexlease.payment.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP 客户端配置。
 *
 * <p>payment-service 通过 {@link RestTemplate} 调用 order-service/user-service/notification-service。
 * {@link LoadBalanced} 使其支持服务名（如 {@code http://order-service}）解析。</p>
 */
@Configuration
public class HttpClientConfig {

    @Bean
    @LoadBalanced
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
