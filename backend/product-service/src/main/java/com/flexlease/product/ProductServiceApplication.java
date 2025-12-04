package com.flexlease.product;

import com.flexlease.product.config.InventoryConcurrencyProperties;
import com.flexlease.product.config.NotificationServiceProperties;
import com.flexlease.product.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({StorageProperties.class, InventoryConcurrencyProperties.class, NotificationServiceProperties.class})
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
