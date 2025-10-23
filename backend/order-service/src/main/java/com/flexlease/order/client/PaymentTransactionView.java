package com.flexlease.order.client;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PaymentTransactionView(
        UUID id,
        String transactionNo,
        UUID orderId,
        UUID userId,
        UUID vendorId,
        PaymentStatus status,
        BigDecimal amount,
        OffsetDateTime paidAt,
        List<RefundView> refunds
) {
    public record RefundView(UUID id, BigDecimal amount, OffsetDateTime refundedAt) {
    }
}
