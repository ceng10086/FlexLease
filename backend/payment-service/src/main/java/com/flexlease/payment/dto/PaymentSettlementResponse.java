package com.flexlease.payment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 厂商结算汇总响应。
 *
 * <p>按厂商维度聚合已成功支付的流水，并拆分押金/租金/买断/违约金、平台抽成与退款净额。</p>
 */
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
