package com.flexlease.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 文件存储配置。
 * <p>
 * 当前实现使用本地文件系统存储媒体文件（大学生项目/演示用途），并通过 {@code /media/**} 提供访问。
 */
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
