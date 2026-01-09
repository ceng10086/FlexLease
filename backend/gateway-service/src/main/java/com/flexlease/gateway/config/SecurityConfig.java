package com.flexlease.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Gateway 安全配置（WebFlux）。
 *
 * <p>网关层不做 JWT/RBAC（避免在网关和微服务重复校验）；所有请求在这里先放行，
 * 由下游服务根据自身的 SecurityConfig 进行鉴权与授权。</p>
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(Customizer.withDefaults())
            .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
            .build();
    }
}
