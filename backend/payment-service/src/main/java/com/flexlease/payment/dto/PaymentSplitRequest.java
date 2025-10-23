package com.flexlease.payment.dto;

import com.flexlease.payment.domain.PaymentSplitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record PaymentSplitRequest(
        @NotNull PaymentSplitType splitType,
        @NotNull @PositiveOrZero BigDecimal amount,
        @NotBlank String beneficiary
) {
}
