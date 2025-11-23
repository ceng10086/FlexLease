package com.flexlease.payment;

import com.flexlease.payment.client.NotificationServiceProperties;
import com.flexlease.payment.client.OrderServiceProperties;
import com.flexlease.payment.client.UserServiceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = "com.flexlease")
@EnableConfigurationProperties({NotificationServiceProperties.class, OrderServiceProperties.class, UserServiceProperties.class})
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
