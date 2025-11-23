package com.flexlease.order.dto;

import java.math.BigDecimal;

public record DisputeMetrics(
        long openCount,
        long pendingAdminCount,
        long resolvedCount,
        BigDecimal averageResolutionHours
) {
}
