package com.flexlease.order.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TrendPoint(
        LocalDate date,
        long orders,
        BigDecimal gmv
) {
}
