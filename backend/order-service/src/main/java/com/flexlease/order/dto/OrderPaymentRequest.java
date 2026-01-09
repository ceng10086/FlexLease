package com.flexlease.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * OrderPaymentRequest 请求 DTO。
 */
public record OrderPaymentRequest(
        @NotNull UUID userId,
        @NotBlank String paymentReference,
        @NotNull BigDecimal paidAmount
) {
}
