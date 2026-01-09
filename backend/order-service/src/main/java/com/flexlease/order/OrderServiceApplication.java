package com.flexlease.order;

import com.flexlease.order.client.NotificationServiceProperties;
import com.flexlease.order.client.PaymentServiceProperties;
import com.flexlease.order.client.ProductServiceProperties;
import com.flexlease.order.config.LlmProperties;
import com.flexlease.order.config.OrderMaintenanceProperties;
import com.flexlease.order.config.OrderSurveyProperties;
import com.flexlease.order.config.ProofPolicyProperties;
import com.flexlease.order.config.ProofStorageProperties;
import com.flexlease.order.client.UserServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 订单服务启动类。
 * <p>
 * 订单服务覆盖：购物车、下单/支付回执、履约（发货/收货/续租/退租/买断）、取证与纠纷、满意度调研、运营指标等能力。
 */
@SpringBootApplication(scanBasePackages = "com.flexlease")
@EnableScheduling
@EnableConfigurationProperties({
        PaymentServiceProperties.class,
        NotificationServiceProperties.class,
        ProductServiceProperties.class,
        UserServiceProperties.class,
        OrderMaintenanceProperties.class,
        ProofStorageProperties.class,
        ProofPolicyProperties.class,
        OrderSurveyProperties.class,
        LlmProperties.class
})
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
