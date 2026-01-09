package com.flexlease.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * OrderReturnCompleteRequest 请求 DTO。
 */
public record OrderReturnCompleteRequest(
        @NotNull UUID vendorId,
        @Size(max = 500) String remark,
        @DecimalMin(value = "0", inclusive = true, message = "退款金额不可为负") BigDecimal refundAmount
) {
}
