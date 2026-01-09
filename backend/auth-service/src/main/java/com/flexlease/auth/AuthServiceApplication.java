package com.flexlease.auth;

import com.flexlease.auth.config.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 认证中心（auth-service）启动入口。
 *
 * <p>职责：账号注册/登录、JWT 颁发与刷新、内部账号状态/厂商绑定接口。</p>
 */
@SpringBootApplication(scanBasePackages = "com.flexlease")
@EnableConfigurationProperties(SecurityProperties.class)
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
