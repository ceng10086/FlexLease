package com.flexlease.user;

import com.flexlease.user.config.AuthServiceProperties;
import com.flexlease.user.config.NotificationServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.flexlease")
@EnableConfigurationProperties({AuthServiceProperties.class, NotificationServiceProperties.class})
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
