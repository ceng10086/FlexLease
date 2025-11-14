package com.flexlease.order.dto;

import java.math.BigDecimal;

public record PlanTypeMetric(
        String planType,
        long orders,
        BigDecimal gmv
) {
}
