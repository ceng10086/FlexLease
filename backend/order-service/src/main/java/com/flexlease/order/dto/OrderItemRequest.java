package com.flexlease.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * OrderItemRequest 请求 DTO。
 */
public record OrderItemRequest(
        @NotNull UUID productId,
        UUID skuId,
        UUID planId,
        @NotBlank String productName,
        String skuCode,
        String planSnapshot,
        @Min(1) int quantity,
        @NotNull BigDecimal unitRentAmount,
        @NotNull BigDecimal unitDepositAmount,
        BigDecimal buyoutPrice
) {
}
