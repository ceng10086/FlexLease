package com.flexlease.order;

import com.flexlease.order.client.NotificationServiceProperties;
import com.flexlease.order.client.PaymentServiceProperties;
import com.flexlease.order.client.ProductServiceProperties;
import com.flexlease.order.config.OrderMaintenanceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({PaymentServiceProperties.class, NotificationServiceProperties.class, ProductServiceProperties.class, OrderMaintenanceProperties.class})
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
