package com.flexlease.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentTransactionView(
        UUID id,
        String transactionNo,
        UUID orderId,
        UUID userId,
        UUID vendorId,
    PaymentScene scene,
        PaymentStatus status,
        BigDecimal amount,
        OffsetDateTime paidAt,
        List<RefundView> refunds
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RefundView(UUID id, BigDecimal amount, OffsetDateTime refundedAt) {
    }
}
