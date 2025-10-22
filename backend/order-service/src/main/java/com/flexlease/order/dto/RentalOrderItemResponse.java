package com.flexlease.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

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
