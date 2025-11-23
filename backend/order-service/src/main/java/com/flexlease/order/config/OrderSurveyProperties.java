package com.flexlease.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flexlease.order.survey")
public class OrderSurveyProperties {

    /**
     * 延迟多少小时后开放满意度调查，默认 24 小时。
     */
    private int reminderDelayHours = 24;

    /**
     * 每轮调度激活的最大调查数量。
     */
    private int activationBatchSize = 20;

    /**
     * 调度执行间隔（毫秒）。
     */
    private long scanIntervalMs = 600_000L;

    public int getReminderDelayHours() {
        return reminderDelayHours;
    }

    public void setReminderDelayHours(int reminderDelayHours) {
        this.reminderDelayHours = reminderDelayHours;
    }

    public int getActivationBatchSize() {
        return activationBatchSize;
    }

    public void setActivationBatchSize(int activationBatchSize) {
        this.activationBatchSize = activationBatchSize;
    }

    public long getScanIntervalMs() {
        return scanIntervalMs;
    }

    public void setScanIntervalMs(long scanIntervalMs) {
        this.scanIntervalMs = scanIntervalMs;
    }
}
