package com.flexlease.order.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP 客户端配置。
 * <ul>
 *   <li>{@code loadBalancedRestTemplate}：用于服务间调用，支持通过服务名走注册发现。</li>
 *   <li>{@code externalRestTemplate}：用于外部 LLM 调用（可配置超时）。</li>
 * </ul>
 */
@Configuration
public class HttpClientConfig {

    @Bean
    @LoadBalanced
    @Primary
    RestTemplate loadBalancedRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    @Bean(name = "externalRestTemplate")
    RestTemplate externalRestTemplate(RestTemplateBuilder builder, LlmProperties llmProperties) {
        return builder
                .setConnectTimeout(Duration.ofMillis(llmProperties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(llmProperties.getReadTimeoutMs()))
                .build();
    }
}
