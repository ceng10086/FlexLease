package com.flexlease.order.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * CartItemResponse 响应 DTO。
 */
public record CartItemResponse(
        UUID id,
        UUID userId,
        UUID vendorId,
        UUID productId,
        UUID skuId,
        UUID planId,
        String productName,
        String skuCode,
        String planSnapshot,
        int quantity,
        BigDecimal unitRentAmount,
        BigDecimal unitDepositAmount,
        BigDecimal buyoutPrice,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
