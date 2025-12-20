package com.flexlease.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flexlease.storage")
public class StorageProperties {

    /**
     * 媒体文件上传存储根目录。
     */
    private String root = "storage/uploads";

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }
}
