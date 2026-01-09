package com.flexlease.order.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 支付流水视图（从 payment-service 查询得到的只读 DTO）。
 * <p>
 * 由于对接的是外部微服务返回值，字段使用 {@link JsonIgnoreProperties} 以兼容后续字段扩展。
 */
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
