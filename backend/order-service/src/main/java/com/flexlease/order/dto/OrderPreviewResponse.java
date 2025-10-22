package com.flexlease.order.dto;

import java.math.BigDecimal;

public record OrderPreviewResponse(
        BigDecimal depositAmount,
        BigDecimal rentAmount,
        BigDecimal totalAmount
) {
}
