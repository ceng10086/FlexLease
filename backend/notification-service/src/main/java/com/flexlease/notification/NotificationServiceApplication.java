package com.flexlease.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 通知服务启动类。
 * <p>
 * 当前通知服务仅保留“站内信”能力：提供通知模板、发送记录查询，并订阅订单事件推送提醒。
 */
@SpringBootApplication(scanBasePackages = "com.flexlease")
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
