package com.flexlease.payment.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

/**
 * 手动确认支付成功请求。
 *
 * <p>用于后台/内部将流水确认成功（不走通道回调）。</p>
 */
public record PaymentConfirmRequest(
        @NotBlank String channelTransactionNo,
        OffsetDateTime paidAt
) {
}
