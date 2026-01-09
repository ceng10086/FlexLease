package com.flexlease.order.dto;

import java.math.BigDecimal;

/**
 * 租赁模式拆分指标 DTO。
 */
public record PlanTypeMetric(
        String planType,
        long orders,
        BigDecimal gmv
) {
}
