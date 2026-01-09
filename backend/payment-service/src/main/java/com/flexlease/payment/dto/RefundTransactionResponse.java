package com.flexlease.payment.dto;

import com.flexlease.payment.domain.RefundStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 退款流水响应。
 */
public record RefundTransactionResponse(
        UUID id,
        String refundNo,
        RefundStatus status,
        BigDecimal amount,
        String reason,
        OffsetDateTime refundedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
