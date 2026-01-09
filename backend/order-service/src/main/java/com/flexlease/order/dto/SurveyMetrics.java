package com.flexlease.order.dto;

import java.math.BigDecimal;

/**
 * SurveyMetrics 指标 DTO。
 */
public record SurveyMetrics(
        long pendingCount,
        long openCount,
        long completedCount,
        BigDecimal averageRating
) {
}
