package com.flexlease.payment.dto;

import com.flexlease.payment.domain.PaymentChannel;
import com.flexlease.payment.domain.PaymentScene;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * 创建支付单请求。
 *
 * <p>支持可选分账明细（splits）：由订单服务在下单/试算阶段生成，用于结算展示。</p>
 */
public record PaymentInitRequest(
        @NotNull UUID userId,
        @NotNull UUID vendorId,
        @NotNull PaymentScene scene,
        @NotNull PaymentChannel channel,
        @NotNull @Positive BigDecimal amount,
        String description,
        @Valid @Size(max = 10) List<PaymentSplitRequest> splits
) {
}
