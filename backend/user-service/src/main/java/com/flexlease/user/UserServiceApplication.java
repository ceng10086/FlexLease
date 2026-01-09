package com.flexlease.user;

import com.flexlease.user.config.AuthServiceProperties;
import com.flexlease.user.config.NotificationServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 用户与厂商服务启动类。
 * <p>
 * 负责厂商入驻、厂商资料、用户档案与信用分等能力，并提供内部接口供其他微服务读取信用档案。
 */
@SpringBootApplication(scanBasePackages = "com.flexlease")
@EnableConfigurationProperties({AuthServiceProperties.class, NotificationServiceProperties.class})
@EnableScheduling
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
