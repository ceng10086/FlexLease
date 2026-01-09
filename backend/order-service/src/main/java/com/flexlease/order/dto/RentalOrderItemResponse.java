package com.flexlease.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * RentalOrderItemResponse 响应 DTO。
 */
public record RentalOrderItemResponse(
        UUID id,
        UUID productId,
        UUID skuId,
        UUID planId,
        String productName,
        String skuCode,
        String planSnapshot,
        int quantity,
        BigDecimal unitRentAmount,
        BigDecimal unitDepositAmount,
        BigDecimal buyoutPrice
) {
}
