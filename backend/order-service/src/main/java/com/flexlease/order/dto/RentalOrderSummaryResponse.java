package com.flexlease.order.dto;

import com.flexlease.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RentalOrderSummaryResponse(
        UUID id,
        String orderNo,
        UUID userId,
        UUID vendorId,
        OrderStatus status,
        BigDecimal totalAmount,
        BigDecimal depositAmount,
        BigDecimal rentAmount,
        OffsetDateTime createdAt,
        OffsetDateTime leaseEndAt,
        boolean requiresManualReview
) {
}
