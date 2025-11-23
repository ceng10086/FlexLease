package com.flexlease.payment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentSettlementResponse(
        UUID vendorId,
        BigDecimal totalAmount,
        BigDecimal depositAmount,
        BigDecimal rentAmount,
        BigDecimal buyoutAmount,
        BigDecimal penaltyAmount,
        BigDecimal platformCommissionAmount,
        BigDecimal refundedAmount,
        BigDecimal netAmount,
        OffsetDateTime lastPaidAt,
        long transactionCount
) {
}
