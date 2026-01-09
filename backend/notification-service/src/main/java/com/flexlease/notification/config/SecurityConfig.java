package com.flexlease.notification.config;

import com.flexlease.common.security.JwtAuthProperties;
import com.flexlease.common.security.JwtAuthenticationFilter;
import com.flexlease.common.security.JwtTokenVerifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 通知服务安全配置：复用平台统一的 JWT 鉴权。
 * <p>
 * 由于通知属于“用户可见数据”，除 permitAll 白名单外的接口均要求携带有效的访问令牌。
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtAuthProperties.class)
public class SecurityConfig {

    @Bean
    public JwtTokenVerifier jwtTokenVerifier(JwtAuthProperties properties) {
        return new JwtTokenVerifier(properties);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenVerifier verifier,
                                                            JwtAuthProperties properties) {
        return new JwtAuthenticationFilter(verifier, properties);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter authenticationFilter,
                                                   JwtAuthProperties properties) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(properties.getPermitAll().toArray(String[]::new)).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
