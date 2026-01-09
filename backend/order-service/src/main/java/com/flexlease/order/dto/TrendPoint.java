package com.flexlease.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 趋势点 DTO。
 */
public record TrendPoint(
        LocalDate date,
        long orders,
        BigDecimal gmv
) {
}
