package com.flexlease.payment.dto;

import com.flexlease.payment.domain.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record PaymentCallbackRequest(
        @NotNull PaymentStatus status,
        @NotBlank String channelTransactionNo,
        OffsetDateTime paidAt,
        String failureReason
) {
}
