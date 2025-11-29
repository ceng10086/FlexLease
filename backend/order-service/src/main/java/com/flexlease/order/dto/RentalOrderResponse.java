package com.flexlease.order.dto;

import com.flexlease.common.user.CreditTier;
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
        BigDecimal originalDepositAmount,
        BigDecimal rentAmount,
        BigDecimal buyoutAmount,
        Integer creditScore,
        CreditTier creditTier,
        BigDecimal depositAdjustmentRate,
        boolean requiresManualReview,
        UUID paymentTransactionId,
        OffsetDateTime leaseStartAt,
        OffsetDateTime leaseEndAt,
        int extensionCount,
        String shippingCarrier,
        String shippingTrackingNo,
        String customerRemark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<RentalOrderItemResponse> items,
        List<OrderEventResponse> events,
        List<OrderExtensionResponse> extensions,
        List<OrderReturnResponse> returns,
        List<OrderProofResponse> proofs,
        List<OrderDisputeResponse> disputes,
        List<OrderSurveyResponse> surveys,
        int shipmentPhotoRequired,
        int shipmentVideoRequired,
        int returnPhotoRequired,
        int returnVideoRequired
) {
}
