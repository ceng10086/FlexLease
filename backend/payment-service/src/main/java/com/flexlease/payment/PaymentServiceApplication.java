package com.flexlease.payment;

import com.flexlease.payment.client.NotificationServiceProperties;
import com.flexlease.payment.client.OrderServiceProperties;
import com.flexlease.payment.client.UserServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * payment-service 启动入口。
 *
 * <p>职责：支付流水、分账、退款、结算汇总。当前实现以“模拟支付”为主：
 * 创建流水后可按配置自动确认，并通过内部接口回调 order-service 更新订单状态。</p>
 */
@SpringBootApplication(scanBasePackages = "com.flexlease")
@EnableConfigurationProperties({NotificationServiceProperties.class, OrderServiceProperties.class, UserServiceProperties.class})
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
