package com.flexlease.order.dto;

import com.flexlease.common.user.CreditTier;
import java.math.BigDecimal;
import java.util.Map;

public record CreditMetrics(
        BigDecimal averageScore,
        Map<CreditTier, Long> tierDistribution
) {
}
