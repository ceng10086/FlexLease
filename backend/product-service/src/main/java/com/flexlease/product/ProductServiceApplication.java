package com.flexlease.product;

import com.flexlease.product.config.InventoryConcurrencyProperties;
import com.flexlease.product.config.NotificationServiceProperties;
import com.flexlease.product.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 商品服务启动类。
 * <p>
 * 负责商品/租赁方案/SKU/库存/媒体资源与商品咨询等能力，并对外提供 Catalog 查询接口。
 */
@SpringBootApplication(scanBasePackages = "com.flexlease")
@EnableConfigurationProperties({StorageProperties.class, InventoryConcurrencyProperties.class, NotificationServiceProperties.class})
@EnableScheduling
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
