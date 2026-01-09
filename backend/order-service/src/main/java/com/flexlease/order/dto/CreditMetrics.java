package com.flexlease.order.dto;

import com.flexlease.common.user.CreditTier;
import java.math.BigDecimal;
import java.util.Map;

/**
 * CreditMetrics 指标 DTO。
 */
public record CreditMetrics(
        BigDecimal averageScore,
        Map<CreditTier, Long> tierDistribution
) {
}
