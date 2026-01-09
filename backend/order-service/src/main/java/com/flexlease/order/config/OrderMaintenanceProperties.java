package com.flexlease.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 订单维护调度相关配置。
 * <p>
 * 主要用于“待支付订单超时自动取消”扫描。
 */
@ConfigurationProperties(prefix = "flexlease.order.maintenance")
public class OrderMaintenanceProperties {

    private long pendingPaymentExpireMinutes = 30;
    private long scanIntervalMs = 60_000;

    public long getPendingPaymentExpireMinutes() {
        return pendingPaymentExpireMinutes;
    }

    public void setPendingPaymentExpireMinutes(long pendingPaymentExpireMinutes) {
        this.pendingPaymentExpireMinutes = pendingPaymentExpireMinutes;
    }

    public long getScanIntervalMs() {
        return scanIntervalMs;
    }

    public void setScanIntervalMs(long scanIntervalMs) {
        this.scanIntervalMs = scanIntervalMs;
    }
}
