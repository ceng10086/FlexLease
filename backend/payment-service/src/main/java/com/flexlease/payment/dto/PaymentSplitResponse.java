package com.flexlease.payment.dto;

import com.flexlease.payment.domain.PaymentSplitType;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSplitResponse(
        UUID id,
        PaymentSplitType splitType,
        BigDecimal amount,
        String beneficiary
) {
}
