package com.flexlease.product.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP 客户端配置。
 * <p>
 * 这里使用 {@link RestTemplate} 并开启 {@code @LoadBalanced}，以便通过服务名（如
 * {@code http://notification-service}）在 Docker Compose / Eureka 环境下发起调用。
 */
@Configuration
public class HttpClientConfig {

    @Bean
    @LoadBalanced
    RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
