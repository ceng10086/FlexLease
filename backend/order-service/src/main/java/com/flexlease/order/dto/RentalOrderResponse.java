package com.flexlease.order.dto;

import com.flexlease.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RentalOrderResponse(
        UUID id,
        String orderNo,
        UUID userId,
        UUID vendorId,
        OrderStatus status,
        String planType,
        BigDecimal totalAmount,
        BigDecimal depositAmount,
        BigDecimal rentAmount,
        BigDecimal buyoutAmount,
        UUID paymentTransactionId,
        OffsetDateTime leaseStartAt,
        OffsetDateTime leaseEndAt,
        int extensionCount,
        String shippingCarrier,
        String shippingTrackingNo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<RentalOrderItemResponse> items,
        List<OrderEventResponse> events,
        List<OrderExtensionResponse> extensions,
        List<OrderReturnResponse> returns
) {
}
