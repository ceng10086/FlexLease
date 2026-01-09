package com.flexlease.payment.dto;

import com.flexlease.payment.domain.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * 模拟支付通道回调请求。
 *
 * <p>由管理员/内部调用触发，用于把支付流水从 PENDING 推进到 SUCCEEDED/FAILED。</p>
 */
public record PaymentCallbackRequest(
        @NotNull PaymentStatus status,
        @NotBlank String channelTransactionNo,
        OffsetDateTime paidAt,
        String failureReason
) {
}
