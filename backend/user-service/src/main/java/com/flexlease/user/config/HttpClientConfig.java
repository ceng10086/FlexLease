package com.flexlease.user.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * HTTP 客户端配置。
 * <p>
 * user-service 采用 Spring 的 {@link RestClient}，并开启 {@code @LoadBalanced} 以支持按服务名调用其他微服务。
 */
@Configuration
public class HttpClientConfig {

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}
