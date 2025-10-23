package com.flexlease.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PaymentRefundRequest(
        @NotNull @Positive BigDecimal amount,
        String reason
) {
}
