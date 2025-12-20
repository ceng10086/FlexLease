package com.flexlease.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flexlease.proof-storage")
public class ProofStorageProperties {

    /**
     * 订单取证文件存储根目录。
     */
    private String root = "storage/order-proofs";

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        if (root != null && !root.isBlank()) {
            this.root = root;
        }
    }
}
