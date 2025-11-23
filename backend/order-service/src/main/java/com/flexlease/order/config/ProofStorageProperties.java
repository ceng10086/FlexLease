package com.flexlease.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flexlease.proof-storage")
public class ProofStorageProperties {

    /**
     * Root directory for storing uploaded order proofs.
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
