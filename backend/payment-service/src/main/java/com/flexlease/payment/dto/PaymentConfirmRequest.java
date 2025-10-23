package com.flexlease.payment.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

public record PaymentConfirmRequest(
        @NotBlank String channelTransactionNo,
        OffsetDateTime paidAt
) {
}
