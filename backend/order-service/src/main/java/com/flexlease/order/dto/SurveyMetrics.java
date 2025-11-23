package com.flexlease.order.dto;

import java.math.BigDecimal;

public record SurveyMetrics(
        long pendingCount,
        long openCount,
        long completedCount,
        BigDecimal averageRating
) {
}
