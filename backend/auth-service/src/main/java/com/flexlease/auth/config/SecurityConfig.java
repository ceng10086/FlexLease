package com.flexlease.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexlease.auth.service.CustomUserDetailsService;
import com.flexlease.common.dto.ApiResponse;
import com.flexlease.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置（Servlet/MVC）。
 *
 * <p>要点：</p>
 * <ul>
 *   <li>无状态：JWT 鉴权，不使用 Session。</li>
 *   <li>登录失败：统一返回 JSON 格式的错误响应（前端更好处理）。</li>
 *   <li>内部接口：/api/v1/internal/** 走自定义 Header 鉴权，不走 JWT。</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomUserDetailsService userDetailsService,
                          ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/token",
                                "/api/v1/auth/register/customer",
                                "/api/v1/auth/register/vendor",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/password/reset",
                                "/api/v1/auth/token/refresh",
                                "/api/v1/internal/**",
                                "/actuator/health",
                                "/actuator/info").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(config -> config
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint())
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeErrorResponse(response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, null))
                )
                .authenticationProvider(authenticationProvider())
                // 先解析 JWT 并将认证信息写入 SecurityContext，后续 Controller 才能做 RBAC 校验
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public AuthenticationEntryPoint jsonAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            boolean invalidCredentials = authException instanceof BadCredentialsException;
            ErrorCode errorCode = invalidCredentials ? ErrorCode.INVALID_CREDENTIALS : ErrorCode.UNAUTHORIZED;
            String message = invalidCredentials ? "账号或密码错误，请重新输入" : null;
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, errorCode, message);
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    private void writeErrorResponse(HttpServletResponse response,
                                    HttpStatus status,
                                    ErrorCode errorCode,
                                    String customMessage) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String message = (customMessage != null && !customMessage.isBlank())
            ? customMessage
            : errorCode.defaultMessage();
        ApiResponse<Void> body = ApiResponse.failure(errorCode.code(), message);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
