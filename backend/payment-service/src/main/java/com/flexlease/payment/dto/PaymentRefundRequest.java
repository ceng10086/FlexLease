package com.flexlease.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 退款请求。
 *
 * <p>金额为正数；是否可退、可退额度等由支付流水本地校验。</p>
 */
public record PaymentRefundRequest(
        @NotNull @Positive BigDecimal amount,
        String reason
) {
}
