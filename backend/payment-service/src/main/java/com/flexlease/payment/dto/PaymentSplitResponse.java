package com.flexlease.payment.dto;

import com.flexlease.payment.domain.PaymentSplitType;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * 分账明细响应。
 */
public record PaymentSplitResponse(
        UUID id,
        PaymentSplitType splitType,
        BigDecimal amount,
        String beneficiary
) {
}
