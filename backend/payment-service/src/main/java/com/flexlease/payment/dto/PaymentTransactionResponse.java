package com.flexlease.payment.dto;

import com.flexlease.payment.domain.PaymentChannel;
import com.flexlease.payment.domain.PaymentScene;
import com.flexlease.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 支付流水详情响应。
 *
 * <p>包含分账明细与退款流水，用于订单详情/结算页展示与对账。</p>
 */
public record PaymentTransactionResponse(
        UUID id,
        String transactionNo,
        UUID orderId,
        UUID userId,
        UUID vendorId,
        PaymentScene scene,
        PaymentStatus status,
        PaymentChannel channel,
        BigDecimal amount,
        String description,
        String channelTransactionNo,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        BigDecimal commissionRate,
        BigDecimal platformCommissionAmount,
        List<PaymentSplitResponse> splits,
        List<RefundTransactionResponse> refunds
) {
}
