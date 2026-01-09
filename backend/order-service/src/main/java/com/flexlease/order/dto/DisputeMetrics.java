package com.flexlease.order.dto;

import java.math.BigDecimal;

/**
 * DisputeMetrics 指标 DTO。
 */
public record DisputeMetrics(
        long openCount,
        long pendingAdminCount,
        long resolvedCount,
        BigDecimal averageResolutionHours
) {
}
