package com.flexlease.order.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

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
